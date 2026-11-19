package com.example.deviceowner

/**
 * Pure policy logic for the Device Owner protection (item #11).
 *
 * The owner chose the DEFINITIVE route against ADB/computer uninstalls:
 * Device Owner mode. Once X Blocker has been made the phone's Device
 * Owner by one-time provisioning OUTSIDE the app (see [provisioningCommand]),
 * Android itself enforces the blocks — no accessibility watching, no race.
 *
 * This object holds only the decisions; every Android call lives in
 * [DeviceOwnerManager]. The managed restriction set started as exactly
 * the two from the batch plan — nothing more is ever applied, and
 * turning the toggles OFF (or losing owner status) clears exactly these:
 *
 *  - [Restriction.BLOCK_OWN_UNINSTALL] — `DevicePolicyManager.setUninstallBlocked`
 *    for X Blocker's own package: the app cannot be uninstalled at all,
 *    from Settings or over ADB.
 *  - [Restriction.DISALLOW_DEBUGGING] — the `UserManager.DISALLOW_DEBUGGING_FEATURES`
 *    user restriction: USB debugging / ADB is switched off system-wide,
 *    so there is no computer channel left to abuse.
 *
 * Batch 2 added two more, each behind its OWN toggle:
 *  - [Restriction.DISALLOW_CONFIG_VPN] (#42) — the
 *    `UserManager.DISALLOW_CONFIG_VPN` restriction: no VPN can be set up.
 *  - [Restriction.DISALLOW_APPS_CONTROL] (#45) — the
 *    `UserManager.DISALLOW_APPS_CONTROL` restriction: the user cannot
 *    modify apps in Settings at all. NOTE: this is the ONLY Device Owner
 *    lever that covers Force Stop — Android has no per-app force-stop
 *    restriction — and it is deliberately broad: while it is on, Force
 *    Stop / Clear Data / Uninstall in Settings are blocked for ALL apps,
 *    not just X Blocker. That breadth is why #45 is opt-in (default OFF).
 *
 * (A general `DISALLOW_UNINSTALL_APPS` restriction was considered and is
 * intentionally NOT applied: the plan scopes this feature to protecting
 * X Blocker itself, and the Install Guard (#27) already gates installing
 * or removing other apps behind the PIN.)
 */
object DeviceOwnerPolicy {

    /** The DPM restrictions this feature manages — the full list. */
    enum class Restriction {
        BLOCK_OWN_UNINSTALL,
        DISALLOW_DEBUGGING,
        DISALLOW_CONFIG_VPN,
        DISALLOW_APPS_CONTROL
    }

    /** What the system currently says about this app's admin standing. */
    enum class Status {
        /** isDeviceOwnerApp: the hard protections can be applied. */
        DEVICE_OWNER,

        /** Owns only a work profile — not enough for the device-wide blocks. */
        PROFILE_OWNER_ONLY,

        /** Plain device admin is active, but the app owns nothing. */
        ADMIN_ONLY,

        /** Neither owner nor admin: provisioning has not happened. */
        NOT_PROVISIONED
    }

    /**
     * Device Owner outranks profile owner, which outranks a plain active
     * admin — the UI reports the strongest standing the app actually has.
     */
    fun statusFor(
        isDeviceOwner: Boolean,
        isProfileOwner: Boolean,
        isAdminActive: Boolean
    ): Status = when {
        isDeviceOwner -> Status.DEVICE_OWNER
        isProfileOwner -> Status.PROFILE_OWNER_ONLY
        isAdminActive -> Status.ADMIN_ONLY
        else -> Status.NOT_PROVISIONED
    }

    /**
     * The restrictions that should be in force right now. The uninstall /
     * debugging pair requires BOTH conditions: without Device Owner the
     * DPM calls would throw, and with the toggle OFF the user has
     * explicitly stood the protection down — so every other combination
     * yields the empty set, which the manager treats as "clear exactly
     * the managed restrictions".
     *
     * The VPN restriction (#42, batch 2) follows its OWN toggle,
     * independent of the Device Owner protection toggle: it applies
     * whenever the app is Device Owner and the VPN block is ON.
     *
     * The apps-control restriction (#45, batch 2) likewise follows its
     * OWN toggle (the Force Stop guard): it applies whenever the app is
     * Device Owner and that guard is ON.
     */
    fun desiredRestrictions(
        isDeviceOwner: Boolean,
        toggleEnabled: Boolean,
        vpnBlockEnabled: Boolean = false,
        forceStopGuardEnabled: Boolean = false
    ): Set<Restriction> {
        if (!isDeviceOwner) return emptySet()
        val desired = mutableSetOf<Restriction>()
        if (toggleEnabled) {
            desired.add(Restriction.BLOCK_OWN_UNINSTALL)
            desired.add(Restriction.DISALLOW_DEBUGGING)
        }
        if (vpnBlockEnabled) {
            desired.add(Restriction.DISALLOW_CONFIG_VPN)
        }
        if (forceStopGuardEnabled) {
            desired.add(Restriction.DISALLOW_APPS_CONTROL)
        }
        return desired
    }

    /** The flattened component name `dpm set-device-owner` expects. */
    fun provisioningComponent(applicationId: String, receiverClassName: String): String =
        "$applicationId/$receiverClassName"

    /** The exact one-time provisioning command shown in the settings UI. */
    fun provisioningCommand(applicationId: String, receiverClassName: String): String =
        "adb shell dpm set-device-owner ${provisioningComponent(applicationId, receiverClassName)}"
}
