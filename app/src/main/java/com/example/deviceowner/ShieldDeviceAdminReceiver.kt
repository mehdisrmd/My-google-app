package com.example.deviceowner

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device-admin receiver behind the Device Owner protection (item #11).
 *
 * This class is the component that one-time provisioning names:
 * `adb shell dpm set-device-owner <applicationId>/com.example.deviceowner.ShieldDeviceAdminReceiver`.
 * The app can never grant itself this role — Android only accepts the
 * command from a computer (on a phone with no accounts signed in) or via
 * QR provisioning during initial device setup.
 *
 * The callbacks simply re-run the restriction reconcile: when admin/owner
 * rights land, the stored toggle choice takes effect immediately; when
 * they are removed, [DeviceOwnerManager.reconcile] is owner-gated and
 * no-ops (the system itself drops an ex-owner's restrictions).
 */
class ShieldDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        DeviceOwnerManager.reconcile(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        DeviceOwnerManager.reconcile(context)
    }
}
