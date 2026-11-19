package com.example.service

import com.example.service.TamperAttemptClassifier.TamperAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Classification tests for [TamperAttemptClassifier] — item #6: the
 * uninstall guard extended to Clear Data and Accessibility-off.
 * Inputs are fake Settings window text lists, mirroring what
 * TextContentScanner.extractScreenText produces on real pages.
 */
class TamperAttemptClassifierTest {

    private val own = "com.aistudio.shieldblock.qxtvnz"
    private val settings = "com.android.settings"
    private val installer = "com.google.android.packageinstaller"

    // ---- UNINSTALL (legacy behavior, must not regress) ----

    @Test
    fun settingsUninstallConfirmation_isUninstall() {
        val texts = listOf(
            "X Blocker",
            "Do you want to uninstall this app?",
            "Cancel", "OK"
        )
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun installerUninstallConfirmation_isUninstall() {
        val texts = listOf("X Blocker", "Uninstall", "Cancel")
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(texts, installer, own)
        )
    }

    @Test
    fun installerInstallDialogForOwnApp_isNotGated() {
        // Sideloading a newer X Blocker APK must stay possible.
        val texts = listOf(
            "X Blocker",
            "Do you want to install this app?",
            "Install", "Cancel"
        )
        assertNull(TamperAttemptClassifier.classify(texts, installer, own))
    }

    @Test
    fun settingsAppDisableDialog_isUninstall() {
        val texts = listOf(
            "X Blocker",
            "Disable this app?",
            "If you disable this app, it will stop working.",
            "Cancel", "Disable app"
        )
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    // ---- CLEAR_DATA (#6a) ----

    @Test
    fun appInfoStoragePage_isClearData() {
        // The Storage page of X Blocker's App info: app name + the
        // Clear storage / Clear cache buttons.
        val texts = listOf(
            "X Blocker",
            "Storage",
            "App size", "User data", "Cache",
            "Clear storage", "Clear cache"
        )
        assertEquals(
            TamperAction.CLEAR_DATA,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun clearDataConfirmationDialog_isClearData() {
        val texts = listOf(
            "Delete app data?",
            "All this app's data will be deleted permanently.",
            "Cancel", "Delete"
        )
        // The dialog itself may not repeat the app name; pair it with the
        // page title the extractor also sees in the same window stack.
        assertEquals(
            TamperAction.CLEAR_DATA,
            TamperAttemptClassifier.classify(texts + "X Blocker", settings, own)
        )
    }

    @Test
    fun clearCacheOnly_isNotClearData() {
        val texts = listOf("X Blocker", "Storage", "Clear cache")
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    @Test
    fun otherAppsStoragePage_isIgnored() {
        val texts = listOf(
            "Some Other App",
            "Storage",
            "Clear storage", "Clear cache"
        )
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    @Test
    fun appInfoMainPage_isIgnored() {
        // The main App-info page (Open / Uninstall / Force stop buttons,
        // "Storage & cache" row) has no danger action on it yet.
        val texts = listOf(
            "X Blocker",
            "Open", "Uninstall", "Force stop",
            "Storage & cache", "Permissions", "Notifications"
        )
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    // ---- DISABLE_ACCESSIBILITY (#6b) ----

    @Test
    fun ownServiceDetailPage_isAccessibilityOff() {
        // The detail page shows the service description; the list doesn't.
        val texts = listOf(
            "X Blocker",
            "On",
            "X Blocker monitors on-screen text, website URLs, Instagram " +
                "Reels, and Telegram Search to protect against unwanted content.",
            "Accessibility shortcut"
        )
        assertEquals(
            TamperAction.DISABLE_ACCESSIBILITY,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun stopServiceDialog_isAccessibilityOff() {
        val texts = listOf(
            "Stop X Blocker?",
            "If you stop this service, it may stop working correctly.",
            "Cancel", "Stop"
        )
        assertEquals(
            TamperAction.DISABLE_ACCESSIBILITY,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun installedServicesList_isIgnored() {
        // Just browsing the list of installed accessibility services:
        // names and On/Off states, no detail-page signals.
        val texts = listOf(
            "Installed apps",
            "X Blocker", "On",
            "TalkBack", "Off",
            "Select to Speak", "Off"
        )
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    // ---- Gates & priority ----

    @Test
    fun browserWindow_isNeverClassified() {
        val texts = listOf("X Blocker", "Clear data", "Do you want to uninstall this app?")
        assertNull(
            TamperAttemptClassifier.classify(texts, "com.android.chrome", own)
        )
    }

    @Test
    fun uninstallWinsOverClearData() {
        val texts = listOf(
            "X Blocker",
            "Do you want to uninstall this app?",
            "Clear storage"
        )
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun persianUninstallConfirmation_isUninstall() {
        val texts = listOf(
            "ایکس بلاکر",
            "آیا می‌خواهید این برنامه را حذف کنید؟",
            "لغو", "حذف نصب"
        )
        // The FA page still carries the Latin service/package strings in
        // view ids, which is what mentionsOurApp keys on.
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(
                texts + "shieldaccessibilityservice",
                settings,
                own
            )
        )
    }
}
