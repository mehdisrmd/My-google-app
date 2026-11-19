package com.example.service

import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.example.R

/**
 * #51 (batch 4): the knockdown half of the foreign-VPN enforcement.
 *
 * Started by ShieldAccessibilityService when a foreign VPN is detected
 * while the "Block VPN Setup" toggle (#42) is on. Android allows only
 * one active VPN, so [Builder.establish] on our TUN disconnects the
 * foreign VPN — and we close the TUN and stop immediately:
 *
 *  - NO routes are added (no addRoute call), so no app's traffic is
 *    ever routed through us, not even for the split second we exist.
 *  - The interface is closed in the same call stack that opened it.
 *
 * The service requires the one-time system VPN consent for X Blocker
 * itself (requested from the dashboard when the toggle is first turned
 * on); without it, establish() simply fails and everything here is a
 * silent no-op — a failure must never crash or wedge anything.
 */
class ShieldVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val descriptor = Builder()
                .setSession(getString(R.string.app_name))
                // A local interface address is required to establish;
                // with no routes added it never sees a packet.
                .addAddress(VPN_ADDRESS, 32)
                .establish()
            // The establish above is what disconnected the foreign VPN.
            // Tear ours down in the same breath: we never stay connected.
            try {
                descriptor?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not close knockdown TUN", e)
            }
        } catch (e: Exception) {
            // No consent, OEM refusal, raced state — the knock simply
            // did not happen; the monitor will retry after its cooldown.
            Log.w(TAG, "VPN knockdown establish failed", e)
        } finally {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        // The system (or a newer VPN) revoked us: nothing to clean up —
        // our TUN never outlives onStartCommand anyway.
        stopSelf()
        super.onRevoke()
    }

    private companion object {
        const val TAG = "ShieldVpn"
        const val VPN_ADDRESS = "10.253.253.1"
    }
}
