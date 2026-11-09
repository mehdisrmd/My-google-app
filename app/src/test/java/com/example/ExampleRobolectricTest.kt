package com.example

import com.example.data.local.DefaultKeywords
import com.example.data.local.DefaultSectionRules
import com.example.data.model.AppUsageLimit
import com.example.data.model.FocusScheduleSlot
import com.example.data.preferences.UninstallGuard
import com.example.notification.LockdownNotificationManager
import com.example.service.KeywordGraceEpisode
import com.example.service.KeywordGraceLogic
import com.example.service.TextContentScanner
import com.example.ui.components.CalendarDayStatus
import com.example.ui.components.StreakDayClassifier
import com.example.ui.components.MotivationalQuotesRepository
import com.example.ui.components.WeeklyActivityCalculator
import com.example.ui.i18n.AppStrings
import com.example.ui.info.AppInfo
import com.example.ui.quiz.ScientificAddictionQuizRepository
import com.example.ui.streak.STREAK_MILESTONES
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianTextPrimary
import com.example.util.AppPackageResolver
import com.example.widget.StreakWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

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
    fun `test section rules default to off on install and detect Instagram Reels and exclude Telegram Archive when enabled`() {
        val defaultRules = DefaultSectionRules.getDefaultRules()
        // All section blockers must be OFF by default when the app is first installed
        assertTrue(defaultRules.all { !it.isEnabled })

        val rules = defaultRules.map { it.copy(isEnabled = true) }

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

        // Item 3: Opening Telegram Archived Chats must NEVER trigger the Telegram Search Blocker!
        val telegramArchiveMatchEn = TextContentScanner.findFirstSectionMatch(
            "Archived Chats | Search",
            "org.telegram.messenger",
            rules
        )
        assertNull("Archived Chats in Telegram must not trigger search blocker", telegramArchiveMatchEn)

        val telegramArchiveMatchFa = TextContentScanner.findFirstSectionMatch(
            "چت‌های آرشیو | جستجو",
            "org.telegram.messenger",
            rules
        )
        assertNull("Persian Archive in Telegram must not trigger search blocker", telegramArchiveMatchFa)
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
    fun `test Duolingo style weekly streak calendar pre-install gray and clean day status`() {
        AppThemeState.isPersianLanguage = false
        val metrics = WeeklyActivityCalculator.buildWeeklyMetrics(
            incidents = emptyList(),
            appLimits = emptyList(),
            preferences = null
        )
        assertEquals(7, metrics.size)
        assertEquals(0, metrics.last().trackedAppMinutes)
        assertEquals("Today", metrics.last().dayShortLabel)
        // Days before install are gray, and Today starts from Day 0 (TODAY_IN_PROGRESS, 0 clean days completed)
        assertEquals(CalendarDayStatus.PRE_INSTALL_GRAY, metrics.first().status)
        assertEquals(CalendarDayStatus.TODAY_IN_PROGRESS, metrics.last().status)
        assertEquals(0, metrics.count { it.status == CalendarDayStatus.CLEAN_STREAK_DAY })
    }

    @Test
    fun `test v0_4 version, My-google-app GitHub repo, 32 daily quotes, 8 milestone badges, and NoBeep YouTube reset lessons`() {
        // Theme toggle (Dark vs White)
        AppThemeState.isDarkTheme = true
        val darkBg = ObsidianBg
        val darkText = ObsidianTextPrimary

        AppThemeState.isDarkTheme = false
        val whiteBg = ObsidianBg
        val whiteText = ObsidianTextPrimary

        assertNotEquals(darkBg, whiteBg)
        assertNotEquals(darkText, whiteText)

        // Persian language toggle
        AppThemeState.isPersianLanguage = true
        assertEquals("ایکس بلاکر", AppStrings.tr("X Blocker", "ایکس بلاکر"))
        AppThemeState.isPersianLanguage = false
        assertEquals("X Blocker", AppStrings.tr("X Blocker", "ایکس بلاکر"))

        // Item 9 & App Info: static identity — app version 0.4, developer Telegram,
        // GitHub repo as a plain link (the in-app update checker was removed).
        assertEquals("0.4", AppInfo.CURRENT_APP_VERSION)
        assertEquals("@mehdisrmd", AppInfo.TELEGRAM_HANDLE)
        assertEquals("https://t.me/mehdisrmd", AppInfo.TELEGRAM_URL)
        assertEquals("https://github.com/mehdisrmd/My-google-app", AppInfo.GITHUB_REPO_URL)

        // Daily 9:00 AM streak alarm: before 9 AM it fires today at 9:00,
        // at/after 9 AM it rolls to tomorrow at 9:00.
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.NOVEMBER, 9, 8, 30, 15)
        cal.set(java.util.Calendar.MILLISECOND, 250)
        val beforeNine = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        assertEquals(
            cal.timeInMillis,
            LockdownNotificationManager.nextDailyStreakTriggerMillis(beforeNine)
        )
        cal.set(java.util.Calendar.MILLISECOND, 1)
        val justAfterNine = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        assertEquals(
            cal.timeInMillis,
            LockdownNotificationManager.nextDailyStreakTriggerMillis(justAfterNine)
        )

        // Item 10: Expanded quotes (32+) & deterministic Daily Quote
        val quotes = MotivationalQuotesRepository.allQuotes
        assertTrue(quotes.size >= 30)
        assertTrue(quotes.any { it.isChainMotto && it.quoteEn.contains("Don't break the chain", ignoreCase = true) })
        val todayQuote = MotivationalQuotesRepository.getDailyQuoteForToday(1_700_000_000_000L)
        val nextDayQuote = MotivationalQuotesRepository.getDailyQuoteForToday(1_700_000_000_000L + 86_400_000L)
        assertNotEquals(todayQuote.id, nextDayQuote.id)

        // Item 15: 8 Milestone Badges starting from Day 0 Starter Badge
        assertEquals(8, STREAK_MILESTONES.size)
        assertEquals(0, STREAK_MILESTONES.first().days)
        assertEquals(90, STREAK_MILESTONES.last().days)

        // Item 16: NoBeep-Style Scientific Addiction Assessment Quiz
        assertEquals(8, ScientificAddictionQuizRepository.questions.size)
        val lowEval = ScientificAddictionQuizRepository.evaluateScore(2)
        assertTrue(lowEval.scorePercent < 20)
        val highEval = ScientificAddictionQuizRepository.evaluateScore(22)
        assertTrue(highEval.scorePercent >= 80)

    }

    @Test
    fun `test smart focus deep work time slot matching`() {
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
    }

    @Test
    fun `test custom app limit strict 2 hour modification cooldown`() {
        val initialTime = 1_700_000_000_000L
        val customLimit = AppUsageLimit(
            packageName = "com.zhiliaoapp.musically",
            appName = "TikTok",
            dailyLimitMinutes = 45,
            isEnabled = true,
            lastModifiedTimestamp = initialTime
        )

        val thirtyMinsLater = initialTime + (30L * 60L * 1000L)
        assertTrue(customLimit.isCooldownActive(thirtyMinsLater))
        assertTrue(customLimit.formatRemainingCooldown(thirtyMinsLater).contains("1h 30m"))

        val afterTwoHours = initialTime + AppUsageLimit.COOLDOWN_DURATION_MS + 60_000L
        assertFalse(customLimit.isCooldownActive(afterTwoHours))
    }

    @Test
    fun `test AppPackageResolver multi-client matching and custom app package resolution`() {
        val equivalents = AppPackageResolver.getAllEquivalentPackages("org.telegram.messenger", "Telegram")
        assertTrue(equivalents.contains("org.telegram.messenger"))
        assertTrue(equivalents.contains("org.telegram.messenger.web"))
        assertTrue(equivalents.contains("tw.nekomimi.nekogram"))

        assertTrue(AppPackageResolver.isTransientSystemPackage("com.android.systemui", "com.example"))
        assertTrue(AppPackageResolver.isTransientSystemPackage("com.google.android.inputmethod.latin", "com.example"))
        assertFalse(AppPackageResolver.isTransientSystemPackage("com.android.chrome", "com.example"))

        val resolvedDiscord = AppPackageResolver.resolvePackageForCustomInput(
            appNameInput = "Discord",
            packageInput = "",
            installedApps = emptyList()
        )
        assertEquals("com.discord", resolvedDiscord)
    }

    @Test
    fun `test uninstall guard 5 hour deadline math`() {
        val t0 = 1_700_000_000_000L
        val fiveHoursMs = 5L * 60L * 60L * 1000L
        val deadline = UninstallGuard.deadlineForRequest(t0)
        assertEquals(t0 + fiveHoursMs, deadline)

        // No request yet: not requested, never unlocked, protection stays locked on
        assertFalse(UninstallGuard.isRequested(0L))
        assertTrue(UninstallGuard.isRequested(deadline))
        assertFalse(UninstallGuard.isUnlocked(0L, t0 + fiveHoursMs + 1L))
        assertFalse(UninstallGuard.canDisableProtection(0L, t0 + fiveHoursMs + 1L))
        assertEquals(0L, UninstallGuard.remainingMillis(0L, t0))

        // During the wait: full remaining at start, still locked 1ms before the end
        assertEquals(fiveHoursMs, UninstallGuard.remainingMillis(deadline, t0))
        assertFalse(UninstallGuard.isUnlocked(deadline, deadline - 1L))
        assertFalse(UninstallGuard.canDisableProtection(deadline, deadline - 1L))
        assertEquals(1L, UninstallGuard.remainingMillis(deadline, deadline - 1L))

        // At and after the deadline: unlocked, remaining clamps to 0
        assertTrue(UninstallGuard.isUnlocked(deadline, deadline))
        assertTrue(UninstallGuard.canDisableProtection(deadline, deadline + 60_000L))
        assertEquals(0L, UninstallGuard.remainingMillis(deadline, deadline + 60_000L))

        // Countdown formatting H:MM:SS
        assertEquals("5:00:00", UninstallGuard.formatRemainingHms(fiveHoursMs))
        assertEquals("4:59:32", UninstallGuard.formatRemainingHms(17_972_000L))
        assertEquals("0:00:00", UninstallGuard.formatRemainingHms(0L))
        assertEquals("0:00:00", UninstallGuard.formatRemainingHms(-5L))
    }

    @Test
    fun `test keyword grace episode - button once, suppress during grace, reblock without button`() {
        val t0 = 1_700_000_000_000L
        var episode = KeywordGraceEpisode()

        // First match of a new episode: block WITH the 10-second button
        val first = KeywordGraceLogic.onKeywordMatch(episode, "xxx", t0)
        assertTrue(first is KeywordGraceLogic.MatchDecision.Block)
        val firstBlock = first as KeywordGraceLogic.MatchDecision.Block
        assertTrue(firstBlock.graceAvailable)
        episode = firstBlock.episode
        assertEquals("xxx", episode.word)
        assertTrue(episode.graceOffered)
        assertFalse(episode.graceUsed)

        // The button is offered once per episode: if that first block is
        // simply waited out (never tapped) and the word is met again, the
        // block already comes WITHOUT the button.
        val waitedOut = KeywordGraceLogic.onKeywordMatch(episode, "xxx", t0 + 600_000L)
        assertTrue(waitedOut is KeywordGraceLogic.MatchDecision.Block)
        assertFalse((waitedOut as KeywordGraceLogic.MatchDecision.Block).graceAvailable)

        // Tapping the button opens exactly a 10-second free window, once
        episode = KeywordGraceLogic.onGraceButtonTapped(episode, t0 + 1_000L)
        assertTrue(episode.graceUsed)
        assertEquals(t0 + 1_000L + KeywordGraceLogic.GRACE_MILLIS, episode.graceUntilMs)
        val tappedAgain = KeywordGraceLogic.onGraceButtonTapped(episode, t0 + 2_000L)
        assertEquals(episode, tappedAgain)

        // During the window matches are suppressed (user is unblocked)
        val during = KeywordGraceLogic.onKeywordMatch(episode, "xxx", t0 + 5_000L)
        assertTrue(during is KeywordGraceLogic.MatchDecision.Suppress)

        // Word "disappearing" mid-window does NOT end the episode yet
        assertEquals(episode, KeywordGraceLogic.onKeywordAbsent(episode, t0 + 5_000L))

        // After the window, word still on screen: block again WITHOUT button
        val reblock = KeywordGraceLogic.onKeywordMatch(episode, "xxx", t0 + 11_001L)
        assertTrue(reblock is KeywordGraceLogic.MatchDecision.Block)
        assertFalse((reblock as KeywordGraceLogic.MatchDecision.Block).graceAvailable)

        // Word gone after the window: episode closes...
        val cleared = KeywordGraceLogic.onKeywordAbsent(episode, t0 + 11_001L)
        assertEquals("", cleared.word)
        assertFalse(cleared.graceOffered)
        assertFalse(cleared.graceUsed)
        assertEquals(0L, cleared.graceUntilMs)

        // ...so a genuinely new appearance blocks WITH the button again
        val fresh = KeywordGraceLogic.onKeywordMatch(cleared, "xxx", t0 + 12_000L)
        assertTrue(fresh is KeywordGraceLogic.MatchDecision.Block)
        assertTrue((fresh as KeywordGraceLogic.MatchDecision.Block).graceAvailable)

        // A different word is always a brand-new episode with the button
        val other = KeywordGraceLogic.onKeywordMatch(episode, "porn", t0 + 100L)
        assertTrue(other is KeywordGraceLogic.MatchDecision.Block)
        assertTrue((other as KeywordGraceLogic.MatchDecision.Block).graceAvailable)
        assertEquals("porn", other.episode.word)
    }

    @Test
    fun `test streak day classifier - today gray, passed green, relapse red`() {
        val today = 20_000L
        val install = 19_990L
        val relapses = setOf(19_995L, today)

        // Today is gray/in-progress regardless — even with a relapse logged today
        assertEquals(
            CalendarDayStatus.TODAY_IN_PROGRESS,
            StreakDayClassifier.classify(today, today, install, relapses)
        )
        // Yesterday, inside the running streak, no relapse: passed clean (green)
        assertEquals(
            CalendarDayStatus.CLEAN_STREAK_DAY,
            StreakDayClassifier.classify(today - 1, today, install, relapses)
        )
        // A recorded relapse day in the past: failed (red)
        assertEquals(
            CalendarDayStatus.RELAPSE_DAY,
            StreakDayClassifier.classify(19_995L, today, install, relapses)
        )
        // A passed day from an earlier, already-broken streak still counts green
        assertEquals(
            CalendarDayStatus.CLEAN_STREAK_DAY,
            StreakDayClassifier.classify(19_992L, today, install, relapses)
        )
        // Install day itself, ended without relapse: green
        assertEquals(
            CalendarDayStatus.CLEAN_STREAK_DAY,
            StreakDayClassifier.classify(install, today, install, relapses)
        )
        // Before install: muted pre-install gray
        assertEquals(
            CalendarDayStatus.PRE_INSTALL_GRAY,
            StreakDayClassifier.classify(install - 1, today, install, relapses)
        )
        // Future days are never classified as clean
        assertEquals(
            CalendarDayStatus.PRE_INSTALL_GRAY,
            StreakDayClassifier.classify(today + 1, today, install, relapses)
        )
    }
}
