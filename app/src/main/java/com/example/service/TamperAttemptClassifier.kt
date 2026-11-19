package com.example.service

/**
 * Pure classification of "the user is trying to defeat X Blocker from
 * system Settings" — item #6 (extends the original uninstall guard).
 *
 * The classifier works on the flat list of on-screen texts the
 * accessibility service already extracts ([TextContentScanner.extractScreenText]),
 * so the whole decision table is unit-testable without Android types.
 * [TextContentScanner.findTamperAttempt] is the node-tree entry point.
 *
 * Four actions are recognised, all targeting THIS app:
 *  - [TamperAction.UNINSTALL] — the legacy guard: uninstall confirmation
 *    in Settings / the package installer, or the app-info "disable" dialog.
 *  - [TamperAction.CLEAR_DATA] — X Blocker's App-info storage page (the
 *    "Clear data" / "Clear storage" buttons live there) or its delete-data
 *    confirmation dialog. "Clear cache" alone is harmless and never fires.
 *  - [TamperAction.DISABLE_ACCESSIBILITY] — X Blocker's own accessibility
 *    service page (its detail page shows the service description, which
 *    the services LIST does not) or the "stop this service" dialog.
 *  - [TamperAction.FORCE_STOP] — X Blocker's own App-info page (#45,
 *    batch 2): the "Force stop" button lives there, and force-stopping
 *    the app kills every guard at once, so the page itself is the
 *    guarded surface — the same reasoning as CLEAR_DATA's storage page.
 *    Best-effort layer; under Device Owner the DISALLOW_APPS_CONTROL
 *    restriction is the hard block.
 *
 * Plus one that does NOT target this app (#42, batch 2):
 *  - [TamperAction.VPN_CONFIG] — a VPN setup surface (the Settings VPN
 *    profile editor, or the system's VPN consent dialog): a VPN would
 *    bypass the blocker, so configuring one is tamper-class. This is
 *    the best-effort layer; under Device Owner the DISALLOW_CONFIG_VPN
 *    restriction is the hard block.
 *
 * Honest limits (same as the plan): this is a deterrent layer. Page
 * layouts and wording differ across Android versions and OEMs (AOSP vs
 * Samsung One UI vs MIUI), so the phrase sets are deliberately redundant
 * (English + Persian) and the service ALSO re-checks on a 1-second ticker,
 * not only on accessibility events. If the user ever does get data
 * cleared, the protection state is gone with it — only Device Owner
 * (#11) is a hard control.
 */
object TamperAttemptClassifier {

    enum class TamperAction {
        UNINSTALL,
        CLEAR_DATA,
        DISABLE_ACCESSIBILITY,
        VPN_CONFIG,
        FORCE_STOP
    }

    /**
     * Distinctive fragment of this app's accessibility-service description
     * (res/values/strings.xml). The service DETAIL page in Settings shows
     * it; the installed-services list does not — which is what separates
     * "user opened our service's page" (one tap from the off switch) from
     * "user is browsing the list".
     */
    private const val SERVICE_DESCRIPTION_FRAGMENT = "monitors on-screen text"

    fun classify(
        screenTexts: List<String>,
        currentPackage: String,
        ownPackage: String = ""
    ): TamperAction? {
        if (screenTexts.isEmpty()) return null
        val pkgLower = currentPackage.lowercase()

        val isInstaller = pkgLower.contains("packageinstaller")
        val isSettingsHost = pkgLower == "com.android.settings" ||
            pkgLower.contains("miui.securitycenter") ||
            pkgLower.contains("permissioncontroller")
        // #42: the system's VPN consent dialogs live in their own package.
        val isVpnDialogHost = pkgLower == "com.android.vpndialogs"
        if (!isInstaller && !isSettingsHost && !isVpnDialogHost) return null

        val combinedLower = screenTexts.joinToString(" | ").lowercase()

        // #42 (batch 2): VPN setup surfaces are checked BEFORE the
        // our-app gate below — a VPN settings page never mentions
        // X Blocker, but configuring a VPN would bypass it entirely.
        if ((isSettingsHost || isVpnDialogHost) && hasVpnConfigSignal(combinedLower)) {
            return TamperAction.VPN_CONFIG
        }

        val ownLower = ownPackage.trim().lowercase()
        val mentionsOurApp = combinedLower.contains("x blocker") ||
            combinedLower.contains("com.aistudio.shieldblock") ||
            combinedLower.contains("shieldaccessibilityservice") ||
            (ownLower.isNotEmpty() && combinedLower.contains(ownLower))
        if (!mentionsOurApp) return null

        if (isInstaller) {
            // The package installer also hosts INSTALL / UPDATE dialogs
            // for this app (e.g. sideloading a newer X Blocker APK).
            // Those must stay usable — only the UNINSTALL confirmation
            // counts. (Legacy wording: any "uninstall" text in the
            // installer means its uninstall confirmation.)
            return if (isUninstallConfirmation(combinedLower, inInstaller = true)) {
                TamperAction.UNINSTALL
            } else {
                null
            }
        }

        // Settings hosts only below.
        if (isUninstallConfirmation(combinedLower, inInstaller = false) ||
            hasAppDisableDialog(combinedLower)
        ) {
            return TamperAction.UNINSTALL
        }
        if (hasClearDataSignal(combinedLower)) {
            return TamperAction.CLEAR_DATA
        }
        // #45 (batch 2): checked BEFORE the accessibility branch — the
        // Persian force-stop label «توقف اجباری» contains «توقف», which
        // the accessibility branch's stop-dialog check would otherwise
        // claim for itself on this very page.
        if (hasForceStopSignal(combinedLower)) {
            return TamperAction.FORCE_STOP
        }
        if (hasAccessibilityOffSignal(combinedLower)) {
            return TamperAction.DISABLE_ACCESSIBILITY
        }
        return null
    }

    /** Uninstall-confirmation wording (AOSP + Samsung + Persian locales). */
    private fun isUninstallConfirmation(combinedLower: String, inInstaller: Boolean): Boolean {
        if (combinedLower.contains("do you want to uninstall") ||
            combinedLower.contains("uninstall this app") ||
            combinedLower.contains("آیا می‌خواهید این برنامه را حذف کنید") ||
            combinedLower.contains("حذف نصب شود") ||
            combinedLower.contains("حذف شود")
        ) {
            return true
        }
        // Inside the package installer the bare word is enough: its only
        // uninstall surface is the confirmation dialog.
        return inInstaller && (
            combinedLower.contains("uninstall") ||
                combinedLower.contains("حذف نصب")
            )
    }

    /** App-info "Disable this app?" confirmation wording. */
    private fun hasAppDisableDialog(combinedLower: String): Boolean {
        return combinedLower.contains("disable this app") ||
            combinedLower.contains("if you disable this app") ||
            combinedLower.contains("غیرفعال کردن این برنامه")
    }

    /**
     * Clear-data signals: the delete-data confirmation dialog, or the
     * App-info STORAGE page itself — that page is where the "Clear data"
     * / "Clear storage" buttons live, and it only ever shows one app's
     * name (already gated on [mentionsOurApp] above), so the button label
     * alone is a reliable "user is staring at the Clear Data button"
     * signal. "Clear cache" is deliberately NOT a signal.
     */
    private fun hasClearDataSignal(combinedLower: String): Boolean {
        return combinedLower.contains("delete app data") ||
            combinedLower.contains("data will be deleted") ||
            combinedLower.contains("clear storage") ||
            combinedLower.contains("clear data") ||
            combinedLower.contains("پاک کردن داده") ||
            combinedLower.contains("حذف داده")
    }

    /**
     * Accessibility-off signals: either the "stop this service"
     * confirmation dialog, or our service's own detail page (recognised
     * by its description text / the "Use X Blocker" switch label).
     */
    private fun hasAccessibilityOffSignal(combinedLower: String): Boolean {        val stopDialog = combinedLower.contains("stop this service") ||
            combinedLower.contains("stop x blocker") ||
            combinedLower.contains("do you want to stop") ||
            combinedLower.contains("توقف")
        if (stopDialog) return true

        val serviceDetailPage = combinedLower.contains(SERVICE_DESCRIPTION_FRAGMENT) ||
            combinedLower.contains("use x blocker")
        return serviceDetailPage
    }

    /**
     * Force-stop signals (#45): the "Force stop" button label on this
     * app's own App-info page. This branch is only reached after the
     * [mentionsOurApp] gate above, so the label is a reliable "user is
     * staring at OUR Force stop button" signal — the same reasoning as
     * [hasClearDataSignal]'s storage page. Another app's App-info page
     * never passes that gate, so its Force stop button stays usable.
     * The Persian label is matched in full («توقف اجباری») so unrelated
     * «توقف» wording elsewhere is not claimed by this branch.
     */
    private fun hasForceStopSignal(combinedLower: String): Boolean {
        return combinedLower.contains("force stop") ||
            combinedLower.contains("توقف اجباری")
    }

    /**
     * #51 (batch 4): is this Settings window the VPN SECTION itself —
     * the profile list page, or a per-profile page carrying the
     * "Always-on VPN" control? Batch 2 deliberately did NOT treat the
     * bare list page as a VPN_CONFIG signal (see [hasVpnConfigSignal]);
     * Mehdi superseded that exception: while the Block VPN toggle is
     * on, entering the section at all ejects the user instantly
     * (BACK out — no lockdown), so the Always-on controls can never be
     * reached. The profile EDITOR keeps the #42 lockdown intervention:
     * pages matching the editor signals are NOT section pages here.
     *
     * Page identity is best-effort (OEM wording varies): the section
     * list page carries a bare "VPN" title element, which the parent
     * Network hub page ALSO has as a row — so hub-page companions
     * (Wi-Fi / airplane mode / hotspot rows) veto the match.
     */
    fun isVpnSettingsSectionPage(
        screenTexts: List<String>,
        currentPackage: String
    ): Boolean {
        if (screenTexts.isEmpty()) return false
        val pkgLower = currentPackage.lowercase()
        val isSettingsHost = pkgLower == "com.android.settings" ||
            pkgLower.contains("miui.securitycenter") ||
            pkgLower.contains("permissioncontroller")
        if (!isSettingsHost) return false

        val combinedLower = screenTexts.joinToString(" | ").lowercase()
        // The editor keeps its #42 lockdown — never reclassify it as a
        // mere section page.
        if (hasVpnEditorSignal(combinedLower)) return false

        val elements = screenTexts.map { it.trim().lowercase() }
        val hasVpnTitle = elements.any { it == "vpn" || it == "وی‌پی‌ان" }
        if (hasVpnTitle && !hasNetworkHubSignal(combinedLower)) return true

        // Per-profile page: the Always-on switch lives here (with the
        // "block connections without VPN" / "Forget VPN" companions).
        return hasAlwaysOnVpnSignal(combinedLower) &&
            (combinedLower.contains("forget vpn") ||
                combinedLower.contains("block connections without vpn") ||
                combinedLower.contains("حذف vpn"))
    }

    /**
     * The VPN profile editor's own wording — #42's lockdown surface.
     * Deliberately NOT the bare phrase "vpn profile": the list page's
     * own "Add VPN profile" button contains it, and the list page must
     * stay a section page (ejection), not become an editor page.
     */
    private fun hasVpnEditorSignal(combinedLower: String): Boolean {
        return combinedLower.contains("edit vpn") ||
            combinedLower.contains("server address") ||
            combinedLower.contains("آدرس سرور")
    }

    /** Rows that only appear together on the parent Network hub page. */
    private fun hasNetworkHubSignal(combinedLower: String): Boolean {
        return combinedLower.contains("airplane mode") ||
            combinedLower.contains("wi-fi") ||
            combinedLower.contains("hotspot") ||
            combinedLower.contains("حالت هواپیما") ||
            combinedLower.contains("هات‌اسپات")
    }

    private fun hasAlwaysOnVpnSignal(combinedLower: String): Boolean {
        return combinedLower.contains("always-on vpn") ||
            combinedLower.contains("always on vpn")
    }

    /**
     * VPN-setup signals (#42): the profile editor / add flow in Settings
     * ("Edit VPN profile", "Add VPN", "Always-on VPN"…) and the system
     * consent dialog an app raises to request a VPN connection ("…wants
     * to set up a VPN connection…"). The bare VPN list page is
     * deliberately NOT a signal here — #51 (batch 4) covers the section
     * pages separately with instant ejection ([isVpnSettingsSectionPage]).
     */
    private fun hasVpnConfigSignal(combinedLower: String): Boolean {
        return combinedLower.contains("add vpn") ||
            combinedLower.contains("edit vpn") ||
            combinedLower.contains("vpn profile") ||
            combinedLower.contains("vpn configuration") ||
            combinedLower.contains("configure vpn") ||
            combinedLower.contains("always-on vpn") ||
            combinedLower.contains("always on vpn") ||
            combinedLower.contains("set up a vpn") ||
            combinedLower.contains("vpn connection") ||
            combinedLower.contains("تنظیمات vpn") ||
            combinedLower.contains("افزودن vpn") ||
            combinedLower.contains("پیکربندی vpn")
    }
}
