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
 * Three actions are recognised, all targeting THIS app:
 *  - [TamperAction.UNINSTALL] — the legacy guard: uninstall confirmation
 *    in Settings / the package installer, or the app-info "disable" dialog.
 *  - [TamperAction.CLEAR_DATA] — X Blocker's App-info storage page (the
 *    "Clear data" / "Clear storage" buttons live there) or its delete-data
 *    confirmation dialog. "Clear cache" alone is harmless and never fires.
 *  - [TamperAction.DISABLE_ACCESSIBILITY] — X Blocker's own accessibility
 *    service page (its detail page shows the service description, which
 *    the services LIST does not) or the "stop this service" dialog.
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
        DISABLE_ACCESSIBILITY
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
        if (!isInstaller && !isSettingsHost) return null

        val combinedLower = screenTexts.joinToString(" | ").lowercase()
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
    private fun hasAccessibilityOffSignal(combinedLower: String): Boolean {
        val stopDialog = combinedLower.contains("stop this service") ||
            combinedLower.contains("stop x blocker") ||
            combinedLower.contains("do you want to stop") ||
            combinedLower.contains("توقف")
        if (stopDialog) return true

        val serviceDetailPage = combinedLower.contains(SERVICE_DESCRIPTION_FRAGMENT) ||
            combinedLower.contains("use x blocker")
        return serviceDetailPage
    }
}
