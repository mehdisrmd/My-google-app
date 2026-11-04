package com.example

import android.content.Context
import android.view.WindowManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DefaultKeywords
import com.example.data.local.DefaultSectionRules
import com.example.data.local.ShieldDatabase
import com.example.data.model.AppUsageLimit
import com.example.data.model.FocusScheduleSlot
import com.example.data.preferences.ShieldPreferences
import com.example.data.repository.ShieldRepository
import com.example.service.TextContentScanner
import com.example.ui.focus.SmartFocusEngine
import com.example.widget.StreakWidgetProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ShieldBlock", appName)
    }

    @Test
    fun `test porn website detection`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val match = TextContentScanner.findFirstMatch(
            "Visiting https://pornhub.com/view_video.php on browser",
            keywords
        )
        assertNotNull(match)
        assertEquals("pornhub.com", match?.matchedKeyword)
    }

    @Test
    fun `test explicit keyword detection`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val match = TextContentScanner.findFirstMatch(
            "Searching for free xxx video streaming",
            keywords
        )
        assertNotNull(match)
        assertEquals("xxx", match?.matchedKeyword)
    }

    @Test
    fun `test Persian adult keyword detection`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val match = TextContentScanner.findFirstMatch(
            "جستجوی فیلم سوپر و دانلود پورن در وب",
            keywords
        )
        assertNotNull(match)
        assertTrue(match?.matchedKeyword == "فیلم سوپر" || match?.matchedKeyword == "پورن")
    }

    @Test
    fun `test Persian text normalization with diacritics`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val match = TextContentScanner.findFirstMatch(
            "متن تستی سِکْسِی آنلاین",
            keywords
        )
        assertNotNull(match)
        assertEquals("سکسی", match?.matchedKeyword)
    }

    @Test
    fun `test short Persian words do not false-positive inside innocent Persian text`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val safeSentence = "زندگی سالم و کسب و کار موفق در اکنون و گفتگو"
        val matchSafe = TextContentScanner.findFirstMatch(safeSentence, keywords)
        assertNull("Expected no false positive on innocent Persian words, got: ${matchSafe?.matchedKeyword}", matchSafe)

        val explicitShortPersian = "جستجوی گی در سایت"
        val matchExplicit = TextContentScanner.findFirstMatch(explicitShortPersian, keywords)
        assertNotNull(matchExplicit)
        assertEquals("گی", matchExplicit?.matchedKeyword)
    }

    @Test
    fun `test section rules detection for Instagram Reels and Telegram Search`() {
        val rules = DefaultSectionRules.getDefaultRules()

        val reelsMatch = TextContentScanner.findFirstSectionMatch(
            "Watching Reels tab",
            "com.instagram.android",
            rules
        )
        assertNotNull(reelsMatch)
        assertTrue(reelsMatch!!.matchedKeyword.contains("Instagram Reels"))

        val telegramPersianSearchMatch = TextContentScanner.findFirstSectionMatch(
            "جستجوی جهانی در تلگرام",
            "org.telegram.messenger",
            rules
        )
        assertNotNull(telegramPersianSearchMatch)
        assertTrue(telegramPersianSearchMatch!!.matchedKeyword.contains("Telegram Search"))
    }

    @Test
    fun `test leetspeak evasion detection`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val match = TextContentScanner.findFirstMatch(
            "trying to bypass with p0rn video",
            keywords
        )
        assertNotNull(match)
        assertEquals("porn", match?.matchedKeyword)
    }

    @Test
    fun `test innocent content is not blocked`() {
        val keywords = DefaultKeywords.getPreloadedKeywords()
        val match = TextContentScanner.findFirstMatch(
            "Reading tutorial about Kotlin programming and Jetpack Compose layouts",
            keywords
        )
        assertNull(match)
    }

    @Test
    fun `test widget milestone badge calculation`() {
        assertEquals("Novice", StreakWidgetProvider.getMilestoneBadge(1))
        assertEquals("Spark", StreakWidgetProvider.getMilestoneBadge(3))
        assertEquals("Bronze Shield", StreakWidgetProvider.getMilestoneBadge(7))
        assertEquals("Silver Guardian", StreakWidgetProvider.getMilestoneBadge(14))
        assertEquals("Gold Warrior", StreakWidgetProvider.getMilestoneBadge(30))
        assertEquals("Diamond Master", StreakWidgetProvider.getMilestoneBadge(90))
    }

    @Test
    fun `test preferences streak reset and second-level app usage tracking`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)
        prefs.setLockDurationMinutes(15)
        assertEquals(15, prefs.getLockDurationMinutes())
        assertTrue(prefs.getStrictModeEnabled())

        prefs.resetStreak("Test slip")
        assertEquals(1, prefs.getSlipsCount())
        assertEquals("Test slip", prefs.lastSlipReason.value)

        // Sub-minute usage tracking test
        val testPkg = "com.instagram.android"
        prefs.resetTodayAppUsage(testPkg)
        assertEquals(0, prefs.getTodayAppUsageSeconds(testPkg))
        prefs.addAppUsageSeconds(testPkg, 40)
        prefs.addAppUsageSeconds(testPkg, 35)
        assertEquals(75, prefs.getTodayAppUsageSeconds(testPkg))
        assertEquals(1, prefs.getTodayAppUsageMinutes(testPkg))
    }

    @Test
    fun `test panic mode urges defeated and personal pledge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)

        val initialUrges = prefs.getUrgesDefeatedCount()
        val afterFirst = prefs.incrementUrgesDefeated()
        assertEquals(initialUrges + 1, afterFirst)
        assertEquals(afterFirst, prefs.urgesDefeatedCount.value)

        prefs.setPersonalPledge("Stay focused on my goals every single day.")
        assertEquals("Stay focused on my goals every single day.", prefs.getPersonalPledge())
        assertEquals("Stay focused on my goals every single day.", prefs.personalPledge.value)

        val endTime = prefs.startLockdown(
            durationMinutes = 15,
            triggerWord = "Emergency Panic Shield (15m Voluntary Lock)",
            sourceApp = "ShieldBlock Enforcement"
        )
        assertTrue(endTime > System.currentTimeMillis())
        assertTrue(prefs.getActiveLockTrigger().contains("Panic"))
        prefs.clearLockdown()
    }

    @Test
    fun `test weekly activity status 7-day metrics calculation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)
        val pkg = "com.instagram.android"
        prefs.resetTodayAppUsage(pkg)
        prefs.addAppUsageMinutes(pkg, 45)
        prefs.incrementUrgesDefeated()

        val weeklyMetrics = com.example.ui.components.WeeklyActivityCalculator.buildWeeklyMetrics(
            incidents = emptyList(),
            appLimits = emptyList(),
            preferences = prefs
        )
        assertEquals(7, weeklyMetrics.size)
        val todayMetric = weeklyMetrics.last()
        assertTrue(todayMetric.isToday)
        assertEquals(45, todayMetric.screenTimeMinutes)
        assertTrue(todayMetric.urgesDefeated >= 1)

        val score = com.example.ui.components.WeeklyActivityCalculator.calculateDisciplineScore(weeklyMetrics)
        assertEquals(100, score)
        assertEquals("Today", todayMetric.dayShortLabel)
    }

    @Test
    fun `test always on panic notification posts persistent SOS actions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.notification.LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        val panicNotif = activeNotifications.find { it.id == 1002 }
        assertNotNull("Expected Always-On Panic notification (ID 1002) to be active", panicNotif)
        val actions = panicNotif?.notification?.actions
        assertNotNull(actions)
        assertTrue((actions?.size ?: 0) >= 2)
        assertTrue(actions!![0].title.toString().contains("PANIC SOS"))
    }

    @Test
    fun `test smart focus deep work time slot matching and auto trigger`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)
        prefs.clearLockdown()
        prefs.setSmartFocusEnabled(true)

        val slot = FocusScheduleSlot(
            id = 77L,
            title = "Morning Deep Work",
            startHour = 9,
            startMinute = 0,
            endHour = 11,
            endMinute = 0,
            daysOfWeekCsv = "1,2,3,4,5,6,7",
            isEnabled = true,
            autoTriggerLockdown = true
        )

        assertEquals(120, slot.getDurationMinutes())
        assertEquals("09:00 – 11:00", slot.formattedTimeRange())
        assertTrue(slot.isTimeWithinSlot(currentHour = 9, currentMinute = 30, isoDayOfWeek = 1))
        assertFalse(slot.isTimeWithinSlot(currentHour = 12, currentMinute = 0, isoDayOfWeek = 1))
        assertEquals(90, slot.getRemainingSlotMinutes(currentHour = 9, currentMinute = 30))

        val testCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
        }

        val triggered = SmartFocusEngine.checkAndFindActiveSlotToTrigger(
            slots = listOf(slot),
            preferences = prefs,
            calendar = testCalendar
        )
        assertNotNull(triggered)
        assertEquals(105, triggered?.second)

        // Once marked triggered today, should not re-trigger on the same day
        prefs.markFocusSlotTriggeredToday(slot.id, testCalendar.timeInMillis)
        val secondCheck = SmartFocusEngine.checkAndFindActiveSlotToTrigger(
            slots = listOf(slot),
            preferences = prefs,
            calendar = testCalendar
        )
        assertNull(secondCheck)
    }

    @Test
    fun `test custom app limit addition and strict 2 hour modification cooldown`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = ShieldPreferences(context)
        val repo = ShieldRepository(db.shieldDao(), prefs)

        val initialTime = 1_700_000_000_000L
        val customLimit = AppUsageLimit(
            packageName = "com.zhiliaoapp.musically",
            appName = "TikTok",
            dailyLimitMinutes = 45,
            isEnabled = true,
            lastModifiedTimestamp = initialTime
        )

        // 1. Add custom app limit (TikTok)
        val savedFirst = repo.saveAppUsageLimit(customLimit, nowMs = initialTime)
        assertTrue(savedFirst)

        // 2. Attempt to change limit 30 minutes later (within 2-hour cooldown) -> MUST BE BLOCKED
        val thirtyMinsLater = initialTime + (30L * 60L * 1000L)
        assertTrue(customLimit.isCooldownActive(thirtyMinsLater))
        assertTrue(customLimit.formatRemainingCooldown(thirtyMinsLater).contains("1h 30m"))

        val attemptChangeEarly = repo.saveAppUsageLimit(
            customLimit.copy(dailyLimitMinutes = 180, lastModifiedTimestamp = thirtyMinsLater),
            nowMs = thirtyMinsLater
        )
        assertFalse("Modifying app timer within 2 hours must be blocked", attemptChangeEarly)

        val attemptToggleOffEarly = repo.toggleAppUsageLimit(customLimit, nowMs = thirtyMinsLater)
        assertFalse("Disabling app limit within 2 hours must be blocked", attemptToggleOffEarly)

        val attemptDeleteEarly = repo.deleteAppUsageLimit("com.zhiliaoapp.musically", nowMs = thirtyMinsLater)
        assertFalse("Deleting app limit within 2 hours must be blocked", attemptDeleteEarly)

        // 3. Attempt to change limit 2 hours + 1 minute later -> MUST SUCCEED
        val afterTwoHours = initialTime + AppUsageLimit.COOLDOWN_DURATION_MS + 60_000L
        assertFalse(customLimit.isCooldownActive(afterTwoHours))

        val savedAfterCooldown = repo.saveAppUsageLimit(
            customLimit.copy(dailyLimitMinutes = 90, lastModifiedTimestamp = afterTwoHours),
            nowMs = afterTwoHours
        )
        assertTrue("Modifying app timer after 2 hours must succeed", savedAfterCooldown)

        val updatedInDb = db.shieldDao().getAppUsageLimitByPackage("com.zhiliaoapp.musically")
        assertEquals(90, updatedInDb?.dailyLimitMinutes)
        db.close()
    }

    @Test
    fun `test biometric lodge preferences and pin verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)

        prefs.setBiometricLockEnabled(true)
        assertTrue(prefs.getIsBiometricLockEnabled())

        prefs.setSecurityPinCode("2580")
        assertEquals("2580", prefs.getSecurityPinCode())
        assertTrue(prefs.verifySecurityPin("2580"))
        assertFalse(prefs.verifySecurityPin("0000"))

        prefs.setBiometricLockEnabled(false)
        assertFalse(prefs.getIsBiometricLockEnabled())
    }

    @Test
    fun `test anti-agent black screen FLAG_SECURE off by default and personal screenshot 60s window`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)
        prefs.clearLockdown()

        // Default: Blackout Privacy Shield is OFF so the user can view the app in the emulator
        assertFalse(prefs.getIsBlackoutPrivacyShieldEnabled())
        assertFalse(prefs.isFlagSecureCurrentlyActive())

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val initialFlags = activity.window.attributes.flags
        assertEquals(
            "Expected FLAG_SECURE to be OFF by default so emulator displays the screen",
            0,
            initialFlags and WindowManager.LayoutParams.FLAG_SECURE
        )

        // When user toggles Blackout Privacy Shield ON -> external agents see black screen (FLAG_SECURE)
        prefs.setBlackoutPrivacyShieldEnabled(true)
        prefs.clearTemporaryPersonalScreenshotWindow()
        assertTrue(prefs.isFlagSecureCurrentlyActive())
        activity.syncWindowSecureFlag()

        val flagsWhenSecure = activity.window.attributes.flags
        assertTrue(
            "Expected FLAG_SECURE to be set on MainActivity window when Blackout Shield is active",
            (flagsWhenSecure and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )

        // Open 60-second Personal Screenshot Window -> FLAG_SECURE temporarily cleared so user can screenshot
        prefs.enableTemporaryPersonalScreenshotWindow(60)
        assertFalse(prefs.isFlagSecureCurrentlyActive())
        activity.syncWindowSecureFlag()
        val flagsDuringScreenshotWindow = activity.window.attributes.flags
        assertEquals(
            "Expected FLAG_SECURE to be cleared during 60s personal screenshot window",
            0,
            flagsDuringScreenshotWindow and WindowManager.LayoutParams.FLAG_SECURE
        )

        // Clear screenshot window -> FLAG_SECURE immediately re-engaged
        prefs.clearTemporaryPersonalScreenshotWindow()
        activity.syncWindowSecureFlag()
        assertTrue(
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        )
        controller.pause().stop().destroy()
    }

    @Test
    fun `test AppPackageResolver multi-client matching and custom app package resolution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ShieldPreferences(context)

        val telegramLimit = AppUsageLimit(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            dailyLimitMinutes = 60,
            isEnabled = true
        )

        // Official Telegram, Telegram X, Plus Messenger, and Nekogram must all match the Telegram limit
        assertTrue(com.example.util.AppPackageResolver.matchesAppLimit(telegramLimit, "org.telegram.messenger", context))
        assertTrue(com.example.util.AppPackageResolver.matchesAppLimit(telegramLimit, "org.telegram.messenger.web", context))
        assertTrue(com.example.util.AppPackageResolver.matchesAppLimit(telegramLimit, "tw.nekomimi.nekogram", context))
        assertFalse(com.example.util.AppPackageResolver.matchesAppLimit(telegramLimit, "com.spotify.music", context))

        // Usage recorded on a client variant (e.g. Telegram X) must aggregate under the canonical Telegram limit
        prefs.resetTodayAppUsage("org.telegram.messenger", "Telegram")
        prefs.addAppUsageMinutes("org.telegram.messenger.web", 25)
        assertEquals(25, prefs.getTodayAppUsageMinutes("org.telegram.messenger", "Telegram"))

        // Transient system packages (SystemUI, keyboards) must never be treated as foreground user apps
        assertTrue(com.example.util.AppPackageResolver.isTransientSystemPackage("com.android.systemui", context.packageName))
        assertTrue(com.example.util.AppPackageResolver.isTransientSystemPackage("com.google.android.inputmethod.latin", context.packageName))
        assertFalse(com.example.util.AppPackageResolver.isTransientSystemPackage("com.android.chrome", context.packageName))

        // Custom app input resolution must resolve real packages instead of leaving stale defaults
        val resolvedDiscord = com.example.util.AppPackageResolver.resolvePackageForCustomInput(
            appNameInput = "Discord",
            packageInput = "",
            installedApps = emptyList()
        )
        assertEquals("com.discord", resolvedDiscord)
    }

    @Test
    fun `test resetEntireAppFromScratch clears usage timers cooldowns streaks and restores clean state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = ShieldPreferences(context)
        val repo = ShieldRepository(db.shieldDao(), prefs)

        // Simulate dirty state: active usage, 2h cooldown lock, slips, urges, custom incident
        val now = System.currentTimeMillis()
        prefs.addAppUsageMinutes("com.instagram.android", 95)
        prefs.incrementUrgesDefeated()
        prefs.resetStreak("Relapse test")
        repo.saveAppUsageLimit(
            AppUsageLimit(
                packageName = "com.discord",
                appName = "Discord",
                dailyLimitMinutes = 30,
                isEnabled = true,
                lastModifiedTimestamp = now
            ),
            nowMs = now
        )
        repo.recordIncident("Test Trigger", "com.discord", 15)

        assertTrue(prefs.getTodayAppUsageMinutes("com.instagram.android", "Instagram") >= 95)
        assertTrue(prefs.getSlipsCount() >= 1)

        // Execute full Reset From Scratch
        repo.resetEntireAppFromScratch()

        assertEquals(0, prefs.getTodayAppUsageMinutes("com.instagram.android", "Instagram"))
        assertEquals(0, prefs.getSlipsCount())
        assertEquals(0, prefs.getUrgesDefeatedCount())
        assertFalse(prefs.isCurrentlyLocked())
        assertFalse(prefs.getIsBlackoutPrivacyShieldEnabled())
        assertNull(db.shieldDao().getAppUsageLimitByPackage("com.discord"))

        val defaultInstagram = db.shieldDao().getAppUsageLimitByPackage("com.instagram.android")
        assertNotNull(defaultInstagram)
        assertFalse("Freshly reset default limits must not be locked in a 2-hour cooldown", defaultInstagram!!.isCooldownActive(now))
        db.close()
    }
}
