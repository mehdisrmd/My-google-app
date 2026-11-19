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

    // ---- #54: after the rename, Settings shows the NEW label
    // ("usafe"); the guards must recognize our pages by it too. ----

    @Test
    fun settingsUninstallConfirmationWithNewName_isUninstall() {
        val texts = listOf(
            "usafe",
            "Do you want to uninstall this app?",
            "Cancel", "OK"
        )
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun stopServiceDialogWithNewName_isAccessibilityOff() {
        val texts = listOf(
            "Stop usafe?",
            "If you stop this service, it may stop working correctly.",
            "Cancel", "Stop"
        )
        assertEquals(
            TamperAction.DISABLE_ACCESSIBILITY,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun ownServiceDetailPageWithNewName_isAccessibilityOff() {
        val texts = listOf(
            "usafe",
            "On",
            "Use usafe",
            "Accessibility shortcut"
        )
        assertEquals(
            TamperAction.DISABLE_ACCESSIBILITY,
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
    fun appInfoMainPage_isForceStop() {
        // The main App-info page (Open / Uninstall / Force stop buttons,
        // "Storage & cache" row): no confirmation dialog is up yet, so
        // it is not UNINSTALL / CLEAR_DATA — but since #45 the Force
        // stop button makes this page itself the guarded surface
        // (the service only acts when the guard's own toggle is ON).
        val texts = listOf(
            "X Blocker",
            "Open", "Uninstall", "Force stop",
            "Storage & cache", "Permissions", "Notifications"
        )
        assertEquals(
            TamperAction.FORCE_STOP,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
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

    // ---- VPN_CONFIG (#42, batch 2) ----

    @Test
    fun settingsVpnProfileEditor_isVpnConfig() {
        // The VPN editor never mentions X Blocker — it must still classify.
        val texts = listOf("Edit VPN profile", "Name", "Type", "Server address", "Save")
        assertEquals(
            TamperAction.VPN_CONFIG,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun settingsAddVpn_isVpnConfig() {
        val texts = listOf("Add VPN", "Always-on VPN", "Block connections without VPN")
        assertEquals(
            TamperAction.VPN_CONFIG,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }

    @Test
    fun vpnConsentDialog_isVpnConfig() {
        val texts = listOf(
            "Connection request",
            "Some App wants to set up a VPN connection so it can monitor network traffic."
        )
        assertEquals(
            TamperAction.VPN_CONFIG,
            TamperAttemptClassifier.classify(texts, "com.android.vpndialogs", own)
        )
    }

    @Test
    fun networkSettingsPageWithVpnRow_isNotClassified() {
        // The Network & internet page merely LISTS a VPN row among the
        // other radios — no configuration surface is open yet.
        val texts = listOf(
            "Network & internet", "Wi-Fi", "Mobile", "Airplane mode", "VPN", "Private DNS"
        )
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    @Test
    fun vpnEditorInBrowserPackage_isNotClassified() {
        val texts = listOf("Edit VPN profile", "Name", "Save")
        assertNull(
            TamperAttemptClassifier.classify(texts, "com.android.chrome", own)
        )
    }

    // ---- VPN SECTION PAGE (#51, batch 4): instant ejection ----

    @Test
    fun vpnListPage_isSectionPage() {
        val texts = listOf("VPN", "None", "Add VPN profile")
        assertEquals(
            true,
            TamperAttemptClassifier.isVpnSettingsSectionPage(texts, settings)
        )
    }

    @Test
    fun vpnProfilePageWithAlwaysOn_isSectionPage() {
        // The per-profile page is where the Always-on switch lives —
        // exactly what the ejection exists to keep unreachable.
        val texts = listOf(
            "HomeVPN", "Always-on VPN", "Block connections without VPN", "Forget VPN"
        )
        assertEquals(
            true,
            TamperAttemptClassifier.isVpnSettingsSectionPage(texts, settings)
        )
    }

    @Test
    fun vpnEditorPage_isNotSectionPage() {
        // The editor keeps the #42 lockdown intervention instead.
        val texts = listOf("Edit VPN profile", "Name", "Type", "Server address", "Save")
        assertEquals(
            false,
            TamperAttemptClassifier.isVpnSettingsSectionPage(texts, settings)
        )
    }

    @Test
    fun networkHubPage_isNotSectionPage() {
        // The hub page has a bare "VPN" row too — its Wi-Fi / airplane
        // companions veto the match so the hub stays usable.
        val texts = listOf(
            "Network & internet", "Wi-Fi", "Mobile", "Airplane mode", "VPN", "Private DNS"
        )
        assertEquals(
            false,
            TamperAttemptClassifier.isVpnSettingsSectionPage(texts, settings)
        )
    }

    @Test
    fun vpnTitleOutsideSettings_isNotSectionPage() {
        val texts = listOf("VPN")
        assertEquals(
            false,
            TamperAttemptClassifier.isVpnSettingsSectionPage(texts, "com.android.chrome")
        )
    }

    @Test
    fun emptyTexts_isNotSectionPage() {
        assertEquals(
            false,
            TamperAttemptClassifier.isVpnSettingsSectionPage(emptyList(), settings)
        )
    }

    // ---- FORCE_STOP (#45, batch 2) ----

    @Test
    fun persianAppInfoPage_isForceStop() {
        val texts = listOf(
            "ایکس بلاکر",
            "باز کردن", "حذف نصب", "توقف اجباری",
            "حافظه و فضای ذخیره‌سازی", "مجوزها"
        )
        // Like the FA uninstall test: the Latin package string is what
        // mentionsOurApp keys on.
        assertEquals(
            TamperAction.FORCE_STOP,
            TamperAttemptClassifier.classify(texts + own, settings, own)
        )
    }

    @Test
    fun otherAppsAppInfoPage_isIgnored() {
        // Another app's App-info page has the same Force stop button —
        // the mentionsOurApp gate keeps it fully usable.
        val texts = listOf(
            "Some Other App",
            "Open", "Uninstall", "Force stop",
            "Storage & cache", "Permissions"
        )
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    @Test
    fun settingsPageWithoutOurApp_isNotForceStop() {
        // The phrase alone is not enough: without our app on the page
        // there is nothing to protect.
        val texts = listOf("Apps", "See all apps", "Force stop")
        assertNull(TamperAttemptClassifier.classify(texts, settings, own))
    }

    @Test
    fun forceStopTextOutsideSettings_isIgnored() {
        // Host gate: the same words in a browser window never classify.
        val texts = listOf("X Blocker", "Force stop")
        assertNull(
            TamperAttemptClassifier.classify(texts, "com.android.chrome", own)
        )
    }

    @Test
    fun uninstallDialogWinsOverForceStop() {
        // The uninstall confirmation is the more advanced stage and
        // keeps its own classification (and its own guard).
        val texts = listOf(
            "X Blocker",
            "Do you want to uninstall this app?",
            "Force stop",
            "Cancel", "OK"
        )
        assertEquals(
            TamperAction.UNINSTALL,
            TamperAttemptClassifier.classify(texts, settings, own)
        )
    }
}
