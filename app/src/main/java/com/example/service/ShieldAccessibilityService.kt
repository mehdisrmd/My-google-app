package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.ShieldApplication
import com.example.data.blocklist.AdultBlocklistStore
import com.example.data.blocklist.AdultDomainMatcher
import com.example.data.blocklog.BlockLogPolicy
import com.example.data.blocklog.ScreenshotStore
import com.example.data.model.AppLimitBudget
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockedKeyword
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import com.example.data.preferences.ShieldPreferences
import com.example.data.repository.ShieldRepository
import com.example.ui.lockdown.LockdownActivity
import com.example.ui.security.InstallGuardActivity
import com.example.util.AppPackageResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class ShieldAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var repository: ShieldRepository
    private lateinit var preferences: ShieldPreferences
    private lateinit var adultBlocklistStore: AdultBlocklistStore
    private lateinit var screenshotStore: ScreenshotStore

    /**
     * #14: single background thread that runs takeScreenshot callbacks and
     * the bitmap copy/compress work, so capture never touches the main
     * thread or the service's IO scope ordering.
     */
    private val screenshotExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** #14: wall-clock time of the last capture ATTEMPT (see BlockLogPolicy). */
    @Volatile
    private var lastScreenshotAttemptMs = 0L

    /** #14: true while a takeScreenshot call is awaiting its callback. */
    @Volatile
    private var screenshotInFlight = false

    /** #14: per-process sequence making screenshot file names unique. */
    private val screenshotSequence = AtomicLong(0L)

    private val powerManager: PowerManager? by lazy {
        getSystemService(PowerManager::class.java)
    }

    private val keyguardManager: KeyguardManager? by lazy {
        getSystemService(KeyguardManager::class.java)
    }

    private val audioManager: AudioManager? by lazy {
        getSystemService(AudioManager::class.java)
    }

    /**
     * #18: an overlay launch that arrived while the phone was locked is
     * parked here (trigger word, source app, lock end time) and launched
     * by [flushDeferredOverlay] the moment the user unlocks — via the
     * ACTION_USER_PRESENT receiver, with the 1-second ticker as backstop.
     */
    @Volatile
    private var deferredOverlay: Triple<String, String, Long>? = null

    /**
     * #1: consecutive ticker re-check scans that did NOT find the episode's
     * word after its grace window ended. The re-check is only consumed
     * after [KeywordGraceLogic.MAX_RECHECK_ABSENT_SCANS] of these (or at
     * once when the word IS found and the re-block fires).
     */
    @Volatile
    private var graceRecheckAbsentScans = 0

    /**
     * #27 ticker bookkeeping: whether a watched install UI (installer
     * confirmation / Play progress page) was visible on the previous
     * tick. Used to edge-trigger the Play backstop and to consume an
     * engaged one-shot allowance when the install UI closes.
     */
    @Volatile
    private var installUiWasVisible = false

    @Volatile
    private var playInstallUiWasVisible = false

    /** #18: fires the parked overlay launch as soon as the user unlocks. */
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                flushDeferredOverlay()
            }
        }
    }

    @Volatile
    private var cachedKeywords: List<BlockedKeyword> = emptyList()

    @Volatile
    private var cachedSectionRules: List<SectionBlockRule> = emptyList()

    @Volatile
    private var cachedAppLimits: List<AppUsageLimit> = emptyList()

    @Volatile
    private var cachedFocusSlots: List<FocusScheduleSlot> = emptyList()

    // These three are written from both the main thread (accessibility
    // events) and the IO ticker coroutine, so they must be volatile.
    @Volatile
    private var lastScanTimestamp = 0L

    @Volatile
    private var lastForegroundPackage: String = ""

    @Volatile
    private var lastUsageTickTimestamp = 0L


    // Guards "is a lock running? -> start one" so the ticker thread and the
    // event thread can never both start a lock (which used to restart the
    // timer and record the incident twice).
    private val lockdownGate = Any()

    @Volatile
    private var lastOverlayLaunchMs = 0L

    /**
     * Runs [block] in the service scope and logs any failure instead of letting
     * it escape. An uncaught exception inside a plain `launch` goes to the
     * default handler and can crash the process or silently end the enforcement
     * loop; both are unacceptable for a blocker.
     */
    private fun launchSafely(name: String, block: suspend () -> Unit) {
        serviceScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "$name failed", e)
            }
        }
    }

    private fun isScreenInteractive(): Boolean = powerManager?.isInteractive != false

    /** #18: true while the keyguard is locked (the user has not unlocked). */
    private fun isKeyguardLockedNow(): Boolean {
        return try {
            keyguardManager?.isKeyguardLocked == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * #17: true while a call is ringing or in progress. AudioManager's
     * mode needs no permission (TelephonyManager call-state callbacks
     * would need READ_PHONE_STATE); the foreground-package check is the
     * backstop for OEM call screens whose audio mode lags the window.
     */
    private fun isCallActiveNow(): Boolean {
        val mode = try {
            audioManager?.mode ?: AudioManager.MODE_NORMAL
        } catch (_: Exception) {
            AudioManager.MODE_NORMAL
        }
        return OverlayDisplayGate.isCallAudioMode(mode) ||
            OverlayDisplayGate.isCallUiPackage(lastForegroundPackage)
    }

    /** #17/#18: may the block overlay be shown right now? */
    private fun overlayGateDecision(): OverlayDisplayGate.Decision {
        return OverlayDisplayGate.decide(
            keyguardLocked = isKeyguardLockedNow(),
            callActive = isCallActiveNow()
        )
    }

    /**
     * #18: show a previously deferred overlay launch once the phone is
     * unlocked — but only if its lock is still running ("still applies").
     * A lock that expired while the phone sat locked is dropped, never
     * resurrected.
     */
    private fun flushDeferredOverlay() {
        val parked = deferredOverlay ?: return
        if (isKeyguardLockedNow()) return
        deferredOverlay = null
        if (!preferences.isCurrentlyLocked()) return
        launchLockdownOverlay(
            triggerWord = parked.first,
            sourceApp = parked.second,
            endTimeEpoch = parked.third,
            force = true
        )
    }

    /**
     * #26(a) warning surface: a long Toast over the app the user is currently
     * in. A Toast (not a notification) keeps the owner's "notifications stay
     * minimal" rule intact, and it appears in the monitored app's context
     * exactly one minute before the block lands.
     */
    private fun showDailyLimitOneMinuteWarning(appName: String) {
        val message = if (preferences.getIsPersianLanguage()) {
            "⚠ فقط ۱ دقیقه از زمان امروز $appName باقی مانده"
        } else {
            "⚠ Only 1 minute left for $appName today"
        }
        mainHandler.post {
            try {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w(TAG, "Could not show daily-limit warning toast", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as ShieldApplication
        repository = app.repository

        preferences = app.preferences
        adultBlocklistStore = app.adultBlocklistStore
        screenshotStore = app.screenshotStore

        // #18: wake the deferred-overlay flush the moment the user unlocks.
        // ACTION_USER_PRESENT is a protected system broadcast; NOT_EXPORTED
        // is the correct flag for it on API 33+.
        try {
            ContextCompat.registerReceiver(
                this,
                userPresentReceiver,
                IntentFilter(Intent.ACTION_USER_PRESENT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            // The ticker backstop in flushDeferredOverlay's callers still
            // covers the unlock case if registration is unavailable.
            Log.w(TAG, "Could not register USER_PRESENT receiver", e)
        }

        // Keep active rules cached in memory for zero-latency scanning
        launchSafely("keywords") {
            repository.allKeywords.collect { list ->
                cachedKeywords = list.filter { it.isEnabled }
            }
        }
        launchSafely("sectionRules") {
            repository.allSectionRules.collect { list ->
                cachedSectionRules = list.filter { it.isEnabled }
            }
        }
        launchSafely("appLimits") {
            repository.allAppUsageLimits.collect { list ->
                cachedAppLimits = list.filter { it.isEnabled }
            }
        }
        launchSafely("focusSlots") {
            repository.allFocusSlots.collect { list ->
                cachedFocusSlots = list.filter { it.isEnabled }
            }
        }

        // Background ticker every 1 second to enforce:
        // 1. Active Lockdown Strict Screen Pinning (allows only X Blocker + temporary Call/SMS window)
        // 2. Real-time Per-App Daily Usage Limits
        // 3. Scheduled Smart Focus Auto-Activation
        launchSafely("ticker") {
            while (true) {
                delay(1000L)
                // One bad tick (a usage-stats query, a preference read, ...)

                // must never end enforcement for the rest of the session.
                try {
                    val now = System.currentTimeMillis()

                    // 1. Check Smart Focus Schedules
                    if (preferences.getIsSmartFocusEnabled() && !preferences.isCurrentlyLocked()) {
                        val activeMatch = com.example.ui.focus.SmartFocusEngine.checkAndFindActiveSlotToTrigger(
                            slots = cachedFocusSlots,
                            preferences = preferences
                        )
                        if (activeMatch != null) {
                            val (slot, durationMins) = activeMatch
                            preferences.markFocusSlotTriggeredToday(slot.id, now)
                            triggerLockdown(
                                triggerWord = "Smart Focus: ${slot.title}",
                                sourceApp = "Scheduled Focus",
                                customDurationMinutes = durationMins
                            )
                        }
                    }

                    val rootNode = try {
                        rootInActiveWindow
                    } catch (_: Exception) {
                        null
                    }
                    val rootPkg = rootNode?.packageName?.toString().orEmpty()
                    val sysPkg = preferences.queryRecentForegroundPackageFromSystem().orEmpty()
                    val activePkg = when {
                        rootPkg.isNotBlank() && !isIgnoredSystemUiPackage(rootPkg) -> rootPkg
                        sysPkg.isNotBlank() && !isIgnoredSystemUiPackage(sysPkg) -> sysPkg
                        else -> lastForegroundPackage

                    }

                    if (activePkg.isNotBlank()) {
                        lastForegroundPackage = activePkg
                    }

                    // 2a. #18 backstop: if an overlay launch was deferred
                    //     because the phone was locked, show it now that the
                    //     user has unlocked (the USER_PRESENT receiver is the
                    //     fast path; this covers a missed broadcast).
                    flushDeferredOverlay()

                    // 2. Strict Lockdown Enforcement: If timer is counting down, ONLY allow X Blocker
                    //    or whitelisted Phone Call / SMS apps launched from the Block Screen!
                    if (preferences.isCurrentlyLocked()) {
                        if (activePkg.isNotBlank() &&
                            !preferences.isPackageAllowedDuringLockdown(activePkg)
                        ) {
                            launchLockdownOverlay(
                                triggerWord = preferences.getActiveLockTrigger(),
                                sourceApp = preferences.getActiveLockApp(),
                                endTimeEpoch = preferences.getActiveLockEndTime()
                            )
                        }
                        continue
                    }

                    // 2b. Tamper Guard (ticker path): even between accessibility
                    //     events, cover Settings app-manager / package-uninstaller
                    //     screens that target X Blocker while the guard is armed.
                    //     #6: the classifier also covers Clear Data on our
                    //     App-info page and switching our accessibility
                    //     service off — same intervention for all three.
                    val tamperAction = if (rootNode != null && activePkg.isNotBlank() &&
                        isUninstallGuardArmed()
                    ) {
                        TextContentScanner.findTamperAttempt(
                            rootNode,
                            activePkg,
                            packageName
                        )
                    } else {
                        null
                    }
                    if (tamperAction != null) {
                        triggerLockdown(

                            triggerWord = tamperTriggerWord(tamperAction),
                            sourceApp = "System Settings"
                        )
                        continue
                    }

                    // 2d. Install Guard (#27, ticker path): watch the
                    //     classic installer's confirmation UI and, as a
                    //     backstop, Play's install-in-progress page. The
                    //     guard has its own toggle and does NOT depend on
                    //     master protection being on.
                    if (preferences.getIsInstallGuardEnabled() && rootNode != null &&
                        activePkg.isNotBlank() && activePkg != packageName
                    ) {
                        val installTexts = TextContentScanner.extractScreenText(rootNode)
                        val isPlay = InstallGuardLogic.isPlayStorePackage(activePkg)
                        val installUiVisible = when {
                            InstallGuardLogic.isInstallerPackage(activePkg) ->
                                InstallGuardLogic.looksLikeInstallConfirmation(
                                    installTexts,
                                    packageName
                                )
                            isPlay ->
                                InstallGuardLogic.looksLikePlayInstallInProgress(installTexts)
                            else -> false
                        }
                        if (installUiVisible) {
                            // The Play backstop is edge-triggered: its
                            // progress page can sit on screen for minutes,
                            // only its APPEARANCE is a new attempt. The
                            // installer dialog, once dismissed by our
                            // BACK, is gone — no edge needed there.
                            if (!isPlay || !playInstallUiWasVisible) {
                                handleInstallAttempt()
                            }
                            installUiWasVisible = true
                            if (isPlay) playInstallUiWasVisible = true
                        } else {
                            if (installUiWasVisible) {
                                // The watched install UI closed: consume
                                // a one-shot allowance it had engaged.
                                preferences.saveInstallGuardState(
                                    InstallGuardLogic.onInstallUiHidden(
                                        preferences.getInstallGuardState()
                                    )
                                )
                            }
                            installUiWasVisible = false
                            playInstallUiWasVisible = false
                        }
                    }

                    if (!preferences.getIsProtectionEnabled()) continue

                    // 2c. Keyword Grace re-check: a 10-second free window just ended.
                    //     If the trigger word is still on screen, re-block WITHOUT
                    //     the grace button.
                    //     Loop guards (v0.4.9 regression — the endless 5:00
                    //     countdown): (1) never scan our OWN window — the block
                    //     screen itself prints the trigger word as the block
                    //     reason, so scanning it "finds" the word forever and
                    //     re-blocks after every expiry; (2) the re-check only
                    //     runs against a real app window — if the notification
                    //     shade or keyboard happens to be the active root on the
                    //     tick the window ends, the scan is skipped entirely
                    //     (the re-check stays pending, see below).
                    //     #1 FIX (this batch): the re-check is consumed ONLY
                    //     when its scan FINDS the word (and the re-block
                    //     fires). The old code consumed the re-check BEFORE
                    //     scanning and, on a "not found" verdict, also
                    //     cleared the episode — but this scan reads only the
                    //     extracted window tree (a capped traversal), while
                    //     the match that opened the episode may have come
                    //     from an accessibility event's own text payload, so
                    //     a single scan could call a still-present word
                    //     "gone". The one-shot re-check was spent, the
                    //     episode was closed, and on a static page (no new
                    //     events) the block screen never came back. Now an
                    //     absent verdict leaves the episode untouched and
                    //     the re-check retries on the next ticks (bounded by
                    //     MAX_RECHECK_ABSENT_SCANS, after which the window
                    //     is consumed but the episode is STILL not cleared —
                    //     closing an episode is the event path's job, via
                    //     onKeywordAbsent on real content scans).
                    val graceEpisode = preferences.getKeywordGraceEpisode()
                    if (!KeywordGraceLogic.isGraceRecheckPending(graceEpisode, now) &&
                        graceEpisode.graceUntilMs > now
                    ) {
                        // A fresh window is running (or none): forget any
                        // absent-scan count an older window accumulated.
                        graceRecheckAbsentScans = 0
                    }
                    if (KeywordGraceLogic.isGraceRecheckPending(graceEpisode, now) &&
                        rootNode != null && rootPkg.isNotBlank() && rootPkg != packageName &&
                        !isIgnoredSystemUiPackage(rootPkg)
                    ) {
                        val episodeKeyword = cachedKeywords.firstOrNull { it.text == graceEpisode.word }
                        val stillPresent = episodeKeyword != null &&
                            TextContentScanner.findMatchingKeyword(
                                TextContentScanner.extractScreenText(rootNode),
                                listOf(episodeKeyword)
                            ) != null
                        if (stillPresent) {
                            graceRecheckAbsentScans = 0
                            preferences.saveKeywordGraceEpisode(
                                KeywordGraceLogic.onGraceRecheckConsumed(graceEpisode)
                            )
                            triggerLockdown(
                                triggerWord = graceEpisode.word,
                                sourceApp = resolveReadableAppName(
                                    activePkg.ifBlank { lastForegroundPackage }
                                ),
                                graceAvailable = false
                            )
                        } else {
                            graceRecheckAbsentScans += 1
                            if (KeywordGraceLogic.shouldConsumeGraceRecheck(graceRecheckAbsentScans)) {
                                graceRecheckAbsentScans = 0
                                preferences.saveKeywordGraceEpisode(
                                    KeywordGraceLogic.onGraceRecheckConsumed(graceEpisode)
                                )
                            }
                        }
                    }

                    // 3. Track Per-App Usage & Enforce Daily Limit
                    //    Usage only accrues while the screen is on: with the
                    //    screen off or the phone locked there is no foreground
                    //    app in use, and counting the last one could trigger a
                    //    daily-limit block the user never earned.
                    if (activePkg.isNotBlank() &&
                        activePkg != packageName &&
                        !isSystemLauncherOrKeyboard(activePkg) &&
                        isScreenInteractive()
                    ) {
                        val elapsedSec = if (lastUsageTickTimestamp > 0L) {

                            ((now - lastUsageTickTimestamp) / 1000L).toInt().coerceIn(1, 5)
                        } else {
                            1
                        }
                        lastUsageTickTimestamp = now
                        preferences.addAppUsageSeconds(activePkg, elapsedSec)

                        val matchedLimit = cachedAppLimits.firstOrNull { limit ->
                            limit.isEnabled && AppPackageResolver.matchesAppLimit(
                                limit = limit,
                                candidatePackage = activePkg,
                                context = this@ShieldAccessibilityService
                            )
                        }
                        if (matchedLimit != null) {
                            val usedMins = preferences.getTodayAppUsageMinutes(
                                packageName = matchedLimit.packageName,
                                appName = matchedLimit.appName
                            )
                            // #26(a): one minute before the budget runs out,
                            // warn the user once per day while they are still
                            // inside the app. The decision math is pure (see
                            // AppLimitBudget); the "already warned" flag is
                            // day-keyed in preferences next to the counters.
                            val usedSecs = preferences.getTodayAppUsageSeconds(
                                packageName = matchedLimit.packageName,
                                appName = matchedLimit.appName
                            )
                            if (AppLimitBudget.shouldShowOneMinuteWarning(
                                    usedSecondsToday = usedSecs,
                                    dailyLimitMinutes = matchedLimit.dailyLimitMinutes,
                                    isEnabled = matchedLimit.isEnabled,
                                    alreadyWarnedToday = preferences.wasDailyLimitWarningShownToday(
                                        matchedLimit.packageName,
                                        matchedLimit.appName
                                    )
                                )
                            ) {
                                preferences.markDailyLimitWarningShownToday(
                                    matchedLimit.packageName,
                                    matchedLimit.appName
                                )
                                showDailyLimitOneMinuteWarning(matchedLimit.appName)
                            }
                            if (AppLimitBudget.isExceeded(usedMins, matchedLimit.dailyLimitMinutes, matchedLimit.isEnabled)) {
                                // Per-app budget spent: the app stays blocked
                                // until the daily window resets at midnight —
                                // not for one short lock duration.
                                triggerLockdown(
                                    triggerWord = "Daily Limit (${matchedLimit.dailyLimitMinutes}m): ${matchedLimit.appName}",
                                    sourceApp = matchedLimit.appName,
                                    customDurationMinutes = AppLimitBudget.minutesUntilNextMidnight(
                                        nowMs = now,
                                        zoneOffsetMs = TimeZone.getDefault().getOffset(now).toLong()
                                    )
                                )
                            }

                        }
                    } else {
                        lastUsageTickTimestamp = now
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Enforcement tick failed", e)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (!isIgnoredSystemUiPackage(pkgName)) {
            lastForegroundPackage = pkgName
        }

        // 1. If device is currently in active lockdown, block leaving X Blocker (except whitelisted Call/SMS)
        //    Enforcement fires only on REAL app presence: a notification
        //    posted by some app (TYPE_NOTIFICATION_STATE_CHANGED carries the
        //    NOTIFYING app's package, not the foreground app) or a SystemUI /
        //    keyboard event must never slam the block screen back over the
        //    dialer / SMS app the user deliberately opened from the block
        //    screen (v0.4.9 bug: Call / Messages buttons "did nothing"
        //    because the overlay instantly returned on unrelated events).
        if (preferences.isCurrentlyLocked()) {
            if (!isIgnoredSystemUiPackage(pkgName) &&
                event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&

                !preferences.isPackageAllowedDuringLockdown(pkgName)
            ) {
                launchLockdownOverlay(
                    triggerWord = preferences.getActiveLockTrigger(),
                    sourceApp = preferences.getActiveLockApp(),
                    endTimeEpoch = preferences.getActiveLockEndTime()
                )
            }
            return
        }

        if (pkgName == packageName) return

        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: event.source

        // OWN-WINDOW EXCLUSION (v0.4.9 endless-loop root cause): if the
        // active window belongs to X Blocker itself (runtime package =
        // applicationId), never scan its content or make any block decision
        // from it. The block screen PRINTS the trigger word as the block
        // reason, and our own screens (history, dashboards) list blocked
        // words too — scanning them re-triggers blocks from our own UI,
        // which is exactly how an expired timer resurrected itself.
        val activeWindowPkg = rootNode?.packageName?.toString().orEmpty()
        if (activeWindowPkg == packageName) return

        // 2. Tamper Guard (Item 7 + Uninstall Delay feature, extended by
        //    #6): armed while the standalone toggle is ON or master
        //    protection is ON — but never after the 5-hour uninstall wait
        //    has elapsed, which is the owner's deliberate escape hatch.
        //    Covers uninstall, Clear Data on our App-info page, and
        //    switching our accessibility service off.
        if (isUninstallGuardArmed()) {
            val tamperAction = TextContentScanner.findTamperAttempt(rootNode, pkgName, packageName)
            if (tamperAction != null) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                triggerLockdown(
                    triggerWord = tamperTriggerWord(tamperAction),
                    sourceApp = "System Settings"
                )
                return
            }
        }

        // 2e. Install Guard (#27, event path): gated by its own toggle,
        //     independent of master protection.
        if (preferences.getIsInstallGuardEnabled()) {
            // Google Play starts the download the moment "Install" is
            // tapped, so the tap itself is the attempt to intercept.
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED &&
                InstallGuardLogic.isPlayStorePackage(pkgName)
            ) {
                val clickedLabel = event.source?.text?.toString()
                    ?: event.source?.contentDescription?.toString()
                    ?: event.text.firstOrNull()?.toString()
                if (InstallGuardLogic.isInstallButtonLabel(clickedLabel)) {
                    handleInstallAttempt()
                    return
                }
            }
            // The classic installer waits on its confirmation dialog, so
            // watching its window content is a complete gate.
            if (rootNode != null && InstallGuardLogic.isInstallerPackage(pkgName) &&
                InstallGuardLogic.looksLikeInstallConfirmation(
                    TextContentScanner.extractScreenText(rootNode),
                    packageName
                )
            ) {
                handleInstallAttempt()
                return
            }
        }

        if (!preferences.getIsProtectionEnabled()) return
        if (isSystemLauncherOrKeyboard(pkgName)) return

        val now = System.currentTimeMillis()
        if (now - lastScanTimestamp < 220L) return
        lastScanTimestamp = now

        val activeRootPkg = rootNode?.packageName?.toString()?.takeIf { it.isNotBlank() } ?: pkgName

        // 3. Check Per-App Daily Time Limit
        val matchedLimit = cachedAppLimits.firstOrNull { limit ->
            limit.isEnabled && AppPackageResolver.matchesAppLimit(
                limit = limit,
                candidatePackage = activeRootPkg,
                context = this@ShieldAccessibilityService
            )
        }
        if (matchedLimit != null) {
            val usedMins = preferences.getTodayAppUsageMinutes(

                packageName = matchedLimit.packageName,
                appName = matchedLimit.appName
            )
            if (AppLimitBudget.isExceeded(usedMins, matchedLimit.dailyLimitMinutes, matchedLimit.isEnabled)) {
                // Per-app budget spent: blocked until the daily window
                // resets at midnight (see AppLimitBudget).
                triggerLockdown(
                    triggerWord = "Daily Limit (${matchedLimit.dailyLimitMinutes}m): ${matchedLimit.appName}",
                    sourceApp = matchedLimit.appName,
                    customDurationMinutes = AppLimitBudget.minutesUntilNextMidnight(
                        nowMs = now,
                        zoneOffsetMs = TimeZone.getDefault().getOffset(now).toLong()
                    )
                )
                return
            }
        }

        // 4. Check Specific App Section Blocker (Instagram Reels & Telegram Search Box)
        // Confirmed by User (Item 4): Show Full Block Page for both Telegram Search and Instagram Reels!
        val matchedSectionRule = TextContentScanner.findMatchingSectionRule(
            root = rootNode,
            currentPackage = activeRootPkg,
            rules = cachedSectionRules
        )
        if (matchedSectionRule != null) {
            if (preferences.getIsSectionExitOnlyMode()) {
                val appLabel = resolveReadableAppName(activeRootPkg)
                // #14: exit-only blocks get a block-log record (with a
                // screenshot attempt) just like full lockdowns. The
                // capture is initiated BEFORE the BACK below so the shot
                // still shows the blocked section, not the screen behind it.
                recordIncidentWithScreenshot(
                    triggerWord = "Section Blocked: ${matchedSectionRule.sectionName}",
                    sourceApp = appLabel,
                    durationMinutes = 0,
                    blockReason = "Blocked app section"
                )
                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerLockdown(
                    triggerWord = "${matchedSectionRule.sectionName} (${resolveReadableAppName(activeRootPkg)})",
                    sourceApp = resolveReadableAppName(activeRootPkg),
                    blockReason = "Blocked app section"
                )
            }
            return
        }

        // 5. Check Explicit Keywords & Custom Domains
        val screenTexts = mutableListOf<String>()
        event.text.forEach { charSeq ->
            charSeq?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { screenTexts.add(it) }
        }
        event.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
            screenTexts.add(it)
        }
        screenTexts.addAll(TextContentScanner.extractScreenText(rootNode))

        // 5a. Adult Site Blocking (#12): the single toggle that replaced
        //     the Adult Preset. When the foreground window shows a URL /
        //     host covered by the downloaded domain list (#13), the
        //     browser is sent to the Google homepage and the standard
        //     lockdown flow runs (which also records the block incident).
        //     This runs before keyword matching so a listed domain always
        //     wins over a coincidental keyword hit on the same page.
        if (preferences.getIsAdultBlockEnabled()) {
            val blocklistIndex = adultBlocklistStore.currentIndex()
            val blockedHost = blocklistIndex?.let {
                AdultDomainMatcher.findBlockedHost(screenTexts, it)
            }
            if (blockedHost != null) {
                redirectBrowserToGoogleHome(activeRootPkg)
                triggerLockdown(
                    triggerWord = "Adult Site Blocked: $blockedHost",
                    sourceApp = resolveReadableAppName(activeRootPkg),
                    blockReason = "Adult site",
                    blockedUrl = blockedHost
                )
                return
            }
        }

        val matchedWord = TextContentScanner.findMatchingKeyword(screenTexts, cachedKeywords)
        if (matchedWord != null) {
            // Keyword Grace: first match of an episode blocks WITH the
            // one-time 10-second free button; a match inside the free window
            // is suppressed; a match after the window (word still on screen)
            // blocks again WITHOUT the button.
            val episode = preferences.getKeywordGraceEpisode()
            when (val decision = KeywordGraceLogic.onKeywordMatch(episode, matchedWord, now)) {
                is KeywordGraceLogic.MatchDecision.Suppress -> {

                    preferences.saveKeywordGraceEpisode(decision.episode)
                }
                is KeywordGraceLogic.MatchDecision.Block -> {
                    preferences.saveKeywordGraceEpisode(decision.episode)
                    triggerLockdown(
                        triggerWord = matchedWord,
                        sourceApp = resolveReadableAppName(activeRootPkg),
                        graceAvailable = decision.graceAvailable
                    )
                }
            }
        } else {
            // No trigger word on screen: close any finished grace episode so
            // a genuinely new appearance starts fresh (button available).
            val episode = preferences.getKeywordGraceEpisode()
            if (episode.word.isNotBlank()) {
                val updated = KeywordGraceLogic.onKeywordAbsent(episode, now)
                if (updated != episode) {
                    preferences.saveKeywordGraceEpisode(updated)
                }
            }
        }
    }

    private fun triggerLockdown(
        triggerWord: String,
        sourceApp: String,
        customDurationMinutes: Int? = null,
        graceAvailable: Boolean = false,
        blockReason: String? = null,
        blockedUrl: String? = null
    ) {
        val durationMinutes = customDurationMinutes ?: preferences.getLockDurationMinutes()
        // Check-then-start must be one atomic step: the 1-second ticker (IO

        // thread) and accessibility events (main thread) can both decide to
        // block at the same moment. A running lock is never restarted, and its
        // incident is not recorded a second time.
        val endTime = synchronized(lockdownGate) {
            if (preferences.isCurrentlyLocked()) return
            preferences.startLockdown(
                durationMinutes = durationMinutes,
                triggerWord = triggerWord,
                sourceApp = sourceApp,
                graceAvailable = graceAvailable
            )
        }

        // #14: capture the blocked page for the block log and record the
        // incident. The capture is INITIATED here — before the overlay
        // launch below — so the display still shows the offending page;
        // the incident row itself is written when the capture resolves.
        recordIncidentWithScreenshot(
            triggerWord = triggerWord,
            sourceApp = sourceApp,
            durationMinutes = durationMinutes,
            blockReason = blockReason,
            blockedUrl = blockedUrl
        )

        // #17: while a call is ringing / in progress, do not yank the
        // user around with BACK/HOME — on some OEMs that buries the call
        // screen. The lock above is already recorded; the overlay gate
        // stands the block screen down until the call ends, and the
        // enforcement loop then surfaces it if the lock is still running.
        if (!isCallActiveNow()) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        launchLockdownOverlay(
            triggerWord = triggerWord,

            sourceApp = sourceApp,
            endTimeEpoch = endTime,
            force = true
        )
    }

    /**
     * #14: records one block incident, attaching a screenshot of the
     * blocked page when one can be captured.
     *
     * Capture contract:
     * - Uses AccessibilityService.takeScreenshot, which exists only on
     *   API 30+; on older devices the incident is recorded without an
     *   image, exactly as before this feature.
     * - The capture is fire-and-forget: this function returns immediately
     *   and NEVER delays the lockdown that called it. The incident row is
     *   inserted from the capture callback (or right away when no capture
     *   is attempted), so a slow or failed capture costs nothing but the
     *   image.
     * - Failure modes all degrade to "incident without screenshot": the
     *   system's own rate limit (ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT),
     *   FLAG_SECURE windows (ERROR_TAKE_SCREENSHOT_SECURE_WINDOW), and any
     *   exception. [BlockLogPolicy] additionally throttles attempts so
     *   rapid repeat episodes of one burst don't burn the system budget.
     * - Screenshots land in app-private storage via [ScreenshotStore];
     *   they are never written to MediaStore, so they can never appear
     *   in the phone gallery.
     */
    private fun recordIncidentWithScreenshot(
        triggerWord: String,
        sourceApp: String,
        durationMinutes: Int,
        blockReason: String? = null,
        blockedUrl: String? = null
    ) {
        val effectiveReason = blockReason ?: BlockLogPolicy.reasonFor(triggerWord)
        val now = System.currentTimeMillis()
        val mayCapture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            BlockLogPolicy.shouldAttemptCapture(
                nowMs = now,
                lastCaptureAttemptMs = lastScreenshotAttemptMs,
                captureInFlight = screenshotInFlight
            )
        if (!mayCapture) {
            recordIncidentNow(triggerWord, sourceApp, durationMinutes, null, effectiveReason, blockedUrl)
            return
        }

        lastScreenshotAttemptMs = now
        screenshotInFlight = true
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        // Runs on screenshotExecutor: buffer wrapping,
                        // scaling and JPEG compression stay off the main
                        // thread and off the service's event path.
                        var savedName: String? = null
                        try {
                            val buffer = result.hardwareBuffer
                            try {
                                val bitmap = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                                if (bitmap != null) {
                                    try {
                                        val fileName = BlockLogPolicy.screenshotFileName(
                                            timestampMs = now,
                                            sequence = screenshotSequence.incrementAndGet()
                                        )
                                        if (screenshotStore.saveJpeg(bitmap, fileName)) {
                                            savedName = fileName
                                        }
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }
                            } finally {
                                buffer.close()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Block screenshot processing failed", e)
                        }
                        screenshotInFlight = false
                        recordIncidentNow(
                            triggerWord, sourceApp, durationMinutes,
                            savedName, effectiveReason, blockedUrl
                        )
                    }

                    override fun onFailure(errorCode: Int) {
                        // Interval limit, secure window, or a system
                        // refusal: the block still gets its log record,
                        // just without an image.
                        screenshotInFlight = false
                        Log.w(TAG, "Block screenshot unavailable (error $errorCode)")
                        recordIncidentNow(
                            triggerWord, sourceApp, durationMinutes,
                            null, effectiveReason, blockedUrl
                        )
                    }
                }
            )
        } catch (e: Exception) {
            screenshotInFlight = false
            Log.w(TAG, "takeScreenshot call failed", e)
            recordIncidentNow(triggerWord, sourceApp, durationMinutes, null, effectiveReason, blockedUrl)
        }
    }

    /** #14: the actual incident insert, always on the service IO scope. */
    private fun recordIncidentNow(
        triggerWord: String,
        sourceApp: String,
        durationMinutes: Int,
        screenshotPath: String?,
        blockReason: String?,
        blockedUrl: String?
    ) {
        serviceScope.launch {
            try {
                repository.recordIncident(
                    matchedWord = triggerWord,
                    sourceApp = sourceApp,
                    durationMinutes = durationMinutes,
                    screenshotPath = screenshotPath,
                    blockReason = blockReason,
                    blockedUrl = blockedUrl
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "recordIncident failed", e)
            }
        }
    }

    private fun launchLockdownOverlay(
        triggerWord: String,
        sourceApp: String,
        endTimeEpoch: Long,
        force: Boolean = false
    ) {
        // #17/#18 gate (see OverlayDisplayGate): never launch the block
        // screen over an incoming/ongoing call, and never over the lock
        // screen. A call needs no parking — the enforcement loop retries
        // on its own once the call ends. A locked phone parks the launch
        // in deferredOverlay; flushDeferredOverlay() fires it on unlock.
        when (overlayGateDecision()) {
            OverlayDisplayGate.Decision.YIELD_TO_CALL -> return
            OverlayDisplayGate.Decision.DEFER_UNTIL_UNLOCK -> {
                deferredOverlay = Triple(triggerWord, sourceApp, endTimeEpoch)
                return
            }
            OverlayDisplayGate.Decision.SHOW -> Unit
        }

        // Enforcement paths call this on every accessibility event and every
        // tick while the user sits in a non-allowed app. Without a limit that
        // floods the main thread with startActivity calls. A fresh block
        // (force = true) is never throttled.
        val nowElapsed = SystemClock.elapsedRealtime()
        if (!force && nowElapsed - lastOverlayLaunchMs < OVERLAY_RELAUNCH_MIN_INTERVAL_MS) return
        lastOverlayLaunchMs = nowElapsed

        mainHandler.post {
            // This runnable was queued some time ago. The lock may have ended
            // since then: the timer reached zero, or the user tapped the
            // one-time "10 seconds free" button, which clears the lock. Ask
            // the stored lock state now, not the end time captured when the
            // runnable was queued, so an expired block can never reappear.
            if (!preferences.isCurrentlyLocked()) return@post

            // The gate may also have flipped while this runnable sat in the
            // queue (a call started ringing, the phone got locked): stand
            // down the same way as a fresh launch would.
            when (overlayGateDecision()) {
                OverlayDisplayGate.Decision.YIELD_TO_CALL -> return@post
                OverlayDisplayGate.Decision.DEFER_UNTIL_UNLOCK -> {
                    deferredOverlay = Triple(triggerWord, sourceApp, endTimeEpoch)
                    return@post
                }
                OverlayDisplayGate.Decision.SHOW -> Unit
            }

            val lockIntent = Intent(this, LockdownActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or

                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(LockdownActivity.EXTRA_TRIGGER_WORD, triggerWord)
                putExtra(LockdownActivity.EXTRA_SOURCE_APP, sourceApp)
                putExtra(LockdownActivity.EXTRA_END_TIME_EPOCH, endTimeEpoch)
            }
            try {
                startActivity(lockIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start LockdownActivity", e)
            }
        }
    }

    /**
     * #12: sends the foreground browser to the Google homepage after an
     * adult domain was blocked.
     *
     * NOTE on fidelity: replacing the current tab's URL in place is
     * impossible through accessibility APIs — there is no way to rewrite
     * another app's address bar. The ACTION_VIEW intent opens Google in
     * the browser (targeted at the foreground package when possible),
     * while the lockdown launched alongside covers the adult page; when
     * the lock ends, the user lands on Google instead of the blocked
     * page. Browsers whose URL is not accessibility-visible (Custom Tabs,
     * in-app WebViews) cannot be domain-checked at all — the same
     * coverage limit the keyword scanner has always had.
     */
    private fun redirectBrowserToGoogleHome(foregroundPackage: String) {
        val googleHome = Uri.parse("https://www.google.com")
        try {
            val targeted = Intent(Intent.ACTION_VIEW, googleHome).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (foregroundPackage.isNotBlank()) {
                    setPackage(foregroundPackage)
                }
            }
            startActivity(targeted)
        } catch (e: Exception) {
            // The foreground app cannot handle VIEW intents (e.g. a chat
            // app showing a link): fall back to the default browser.
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, googleHome)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                Log.w(TAG, "Could not redirect browser to Google homepage", e2)
            }
        }
    }

    /** Incident-log wording per tamper action (#6). */
    private fun tamperTriggerWord(action: TamperAttemptClassifier.TamperAction): String {
        return when (action) {
            TamperAttemptClassifier.TamperAction.UNINSTALL -> "Prevent Uninstall Guard"
            TamperAttemptClassifier.TamperAction.CLEAR_DATA -> "Prevent Clear Data Guard"
            TamperAttemptClassifier.TamperAction.DISABLE_ACCESSIBILITY ->
                "Prevent Accessibility Disable Guard"
        }
    }

    /**
     * #27: an install attempt was detected (installer dialog, Play
     * Install tap, or Play progress page). Runs the pure state machine
     * against the persisted guard state: an active one-shot allowance
     * lets the install proceed; an already-running prompt just keeps the
     * user out of the installer; otherwise the attempt is interrupted
     * (BACK + HOME) and the 10-second PIN prompt is launched.
     */
    private fun handleInstallAttempt() {
        val now = System.currentTimeMillis()
        val state = preferences.getInstallGuardState()
        val (newState, action) = InstallGuardLogic.onInstallAttempt(state, now)
        if (newState != state) {
            preferences.saveInstallGuardState(newState)
        }
        when (action) {
            InstallGuardLogic.AttemptAction.ALLOW -> Unit
            InstallGuardLogic.AttemptAction.ALREADY_PROMPTING -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            InstallGuardLogic.AttemptAction.PROMPT -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchInstallGuardPrompt()
            }
        }
    }

    /**
     * Launches the install-guard PIN pad — unless the G6 display gate
     * says a call or the lock screen owns the display right now. In that
     * case the prompt is simply not shown: the install was already
     * interrupted and, with no way to confirm, stays blocked (the prompt
     * state expires on its own).
     */
    private fun launchInstallGuardPrompt() {
        if (overlayGateDecision() != OverlayDisplayGate.Decision.SHOW) return
        mainHandler.post {
            if (overlayGateDecision() != OverlayDisplayGate.Decision.SHOW) return@post
            val promptIntent = Intent(this, InstallGuardActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            try {
                startActivity(promptIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start InstallGuardActivity", e)
            }
        }
    }

    /**
     * The Settings / package-uninstaller block is armed while the standalone
     * Prevent Uninstall toggle is ON or master protection is ON — and disarms
     * for everyone once the owner has completed the 5-hour uninstall wait.
     */
    private fun isUninstallGuardArmed(): Boolean {
        if (preferences.isUninstallUnlocked()) return false
        return preferences.getIsPreventUninstallEnabled() || preferences.getIsProtectionEnabled()
    }

    private fun isIgnoredSystemUiPackage(pkgName: String): Boolean {
        val lower = pkgName.lowercase()
        return lower == "com.android.systemui" ||
            lower.contains("inputmethod") ||
            lower.contains("keyboard") ||
            lower == "android"
    }


    private fun isSystemLauncherOrKeyboard(pkgName: String): Boolean {
        val lower = pkgName.lowercase()
        return lower.contains("launcher") ||
            lower.contains("systemui") ||
            lower.contains("inputmethod") ||
            lower.contains("keyboard") ||
            lower.contains("nexuslauncher") ||
            lower.contains("pixel.launcher") ||
            lower.contains("trebuchet")
    }

    private fun resolveReadableAppName(pkgName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(pkgName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkgName.substringAfterLast('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    override fun onInterrupt() {
        // Required override for AccessibilityService
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(userPresentReceiver)
        } catch (_: Exception) {
            // Never registered (registration failed in onCreate) — fine.
        }
        // Drop any queued block-screen relaunches along with the coroutines.
        mainHandler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        screenshotExecutor.shutdown()
    }


    private companion object {
        const val TAG = "ShieldA11y"
        const val OVERLAY_RELAUNCH_MIN_INTERVAL_MS = 400L
    }
}
