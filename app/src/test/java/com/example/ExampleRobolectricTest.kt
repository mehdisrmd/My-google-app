package com.example

import com.example.data.local.DefaultKeywords
import com.example.data.local.DefaultSectionRules
import com.example.data.model.AppUsageLimit
import com.example.data.model.FocusScheduleSlot
import com.example.service.TextContentScanner
import com.example.ui.components.WeeklyActivityCalculator
import com.example.ui.i18n.AppStrings
import com.example.ui.info.GithubUpdateChecker
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
    fun `test section rules detection for Instagram Reels and Telegram Search Box`() {
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
    fun `test weekly activity status 7-day metrics calculation without predefined examples`() {
        AppThemeState.isPersianLanguage = false
        val emptyLimitsMetrics = WeeklyActivityCalculator.buildWeeklyMetrics(
            incidents = emptyList(),
            appLimits = emptyList(),
            preferences = null
        )
        assertEquals(7, emptyLimitsMetrics.size)
        assertEquals(0, emptyLimitsMetrics.last().trackedAppMinutes)
        assertEquals("Today", emptyLimitsMetrics.last().dayShortLabel)
    }

    @Test
    fun `test Persian language RTL and Dark White theme switching and GithubUpdateChecker`() {
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

        // GitHub Update Checker & @mehdisrmd Telegram contact verification
        assertEquals("@mehdisrmd", GithubUpdateChecker.TELEGRAM_HANDLE)
        assertEquals("https://t.me/mehdisrmd", GithubUpdateChecker.TELEGRAM_URL)
        assertEquals(
            "mehdisrmd/x-blocker",
            GithubUpdateChecker.extractSlugFromUrlOrInput("https://github.com/mehdisrmd/x-blocker/releases")
        )
        assertTrue(GithubUpdateChecker.isRemoteVersionNewer("v2.3.0", "2.2.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("v2.2.0", "2.2.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("v1.9.0", "2.2.0"))

        // Motivational Quotes & Don't Break the Chain verification
        val quotes = com.example.ui.components.MotivationalQuotesRepository.allQuotes
        assertTrue(quotes.size >= 10)
        assertTrue(quotes.any { it.isChainMotto && it.quoteEn.contains("Don't break the chain", ignoreCase = true) })
        assertTrue(quotes.any { it.authorEn.contains("Marcus Aurelius") })

        // Breathing Studio Modes verification
        val breathingModes = com.example.ui.components.BreathingPatternMode.entries
        assertEquals(3, breathingModes.size)
        assertEquals(3, com.example.ui.components.BreathingPatternMode.RELAX_478.phases.size)
        assertEquals(4, com.example.ui.components.BreathingPatternMode.BOX_4444.phases.size)

        // Predefined Streak Share Milestones verification
        assertEquals(7, com.example.ui.streak.STREAK_MILESTONES.size)
        assertTrue(com.example.ui.streak.STREAK_MILESTONES.first().shareMottoEn.contains("chain", ignoreCase = true))
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
}
