package com.example.deviceowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log
import com.example.data.preferences.ShieldPreferences

/**
 * DevicePolicyManager wrapper for the Device Owner protection (item #11).
 *
 * All decisions come from the pure [DeviceOwnerPolicy]; this object only
 * reads the live system state and performs the DPM calls. Every call is
 * guarded twice — by the owner check (the DPM restriction APIs throw
 * SecurityException for anyone who is not the device/profile owner) and
 * by try/catch (a failed call must never crash the app or the
 * accessibility service; the next reconcile retries it).
 *
 * [reconcile] is the single entry point and is idempotent: it computes
 * the desired restriction set from (isDeviceOwner × stored toggle) and
 * drives DPM to exactly that state. It runs at app start, at
 * accessibility-service start, from the admin receiver callbacks, and
 * whenever the settings toggle flips — so the applied state survives
 * reboots and process death and self-heals if anything drifts.
 */
object DeviceOwnerManager {

    private const val TAG = "DeviceOwnerManager"

    /** The admin component every DPM call is attributed to. */
    fun adminComponent(context: Context): ComponentName =
        ComponentName(context.applicationContext, ShieldDeviceAdminReceiver::class.java)

    private fun dpm(context: Context): DevicePolicyManager? =
        context.getSystemService(DevicePolicyManager::class.java)

    fun isDeviceOwner(context: Context): Boolean = try {
        dpm(context)?.isDeviceOwnerApp(context.packageName) == true
    } catch (e: Exception) {
        false
    }

    fun isProfileOwner(context: Context): Boolean = try {
        dpm(context)?.isProfileOwnerApp(context.packageName) == true
    } catch (e: Exception) {
        false
    }

    fun isAdminActive(context: Context): Boolean = try {
        dpm(context)?.isAdminActive(adminComponent(context)) == true
    } catch (e: Exception) {
        false
    }

    /** The app's current standing, for the settings UI's honest status. */
    fun status(context: Context): DeviceOwnerPolicy.Status =
        DeviceOwnerPolicy.statusFor(
            isDeviceOwner = isDeviceOwner(context),
            isProfileOwner = isProfileOwner(context),
            isAdminActive = isAdminActive(context)
        )

    /** Reconcile using the toggle choices stored in preferences. */
    fun reconcile(context: Context) {
        val prefs = ShieldPreferences(context)
        reconcile(
            context,
            toggleEnabled = prefs.getIsDeviceOwnerProtectionEnabled(),
            vpnBlockEnabled = prefs.getIsVpnConfigBlockEnabled()
        )
    }

    /**
     * Drive DPM to the state [DeviceOwnerPolicy.desiredRestrictions]
     * prescribes. When the app is not the Device Owner there is nothing
     * to apply AND nothing of ours left to clear (Android drops an
     * ex-owner's restrictions itself), so the method returns quietly.
     */
    fun reconcile(context: Context, toggleEnabled: Boolean, vpnBlockEnabled: Boolean = false) {
        val manager = dpm(context) ?: return
        if (!isDeviceOwner(context)) return

        val admin = adminComponent(context)
        val desired = DeviceOwnerPolicy.desiredRestrictions(
            isDeviceOwner = true,
            toggleEnabled = toggleEnabled,
            vpnBlockEnabled = vpnBlockEnabled
        )

        // 1. Uninstall block for X Blocker's own package — covers both
        //    the Settings uninstall button and `adb uninstall`.
        try {
            manager.setUninstallBlocked(
                admin,
                context.packageName,
                DeviceOwnerPolicy.Restriction.BLOCK_OWN_UNINSTALL in desired
            )
        } catch (e: Exception) {
            Log.w(TAG, "setUninstallBlocked reconcile failed", e)
        }

        // 2. USB debugging / ADB off while the protection is on, so no
        //    computer channel remains to bypass the on-device guards.
        try {
            if (DeviceOwnerPolicy.Restriction.DISALLOW_DEBUGGING in desired) {
                manager.addUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)
            } else {
                manager.clearUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)
            }
        } catch (e: Exception) {
            Log.w(TAG, "debugging-features restriction reconcile failed", e)
        }

        // 3. VPN configuration off while its own toggle (#42) is on, so
        //    no VPN can be set up to bypass the blocker.
        try {
            if (DeviceOwnerPolicy.Restriction.DISALLOW_CONFIG_VPN in desired) {
                manager.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_VPN)
            } else {
                manager.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_VPN)
            }
        } catch (e: Exception) {
            Log.w(TAG, "config-vpn restriction reconcile failed", e)
        }
    }
}
