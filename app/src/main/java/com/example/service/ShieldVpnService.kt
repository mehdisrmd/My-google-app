package com.example.service

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.R

/**
 * usafe's own VPN service — two modes, one TUN at a time.
 *
 * KNOCKDOWN (#51, batch 4): started by ShieldAccessibilityService
 * when a foreign VPN is detected while the "Block VPN Setup" toggle
 * (#42) is on. Android allows only one active VPN, so [Builder]
 * .establish on our TUN disconnects the foreign VPN — and we close
 * the TUN and stop immediately. NO routes are added, so no app's
 * traffic is ever routed through us, not even for the split second
 * we exist.
 *
 * FIREWALL (#63, batch 5): while the Block VPN guard is on AND the
 * Per-App Internet policy wants apps blocked, the TUN stays UP with
 * a full route but ONLY the blocked apps are routed into it
 * (addAllowedApplication); every packet that arrives is read and
 * dropped, so those apps have no connectivity while every other app
 * is untouched. The desired set is computed by [PerAppFirewall];
 * this service just builds/tears the TUN to match.
 *
 * Interplay: a knockdown request that arrives while a firewall set
 * is desired becomes a firewall (re)establish instead — establishing
 * our TUN disconnects the foreign VPN just the same, and the
 * firewall is the mode that must remain afterwards. When a foreign
 * VPN displaces the firewall, onRevoke marks it down and the
 * accessibility service's #51 knockdown path brings the firewall
 * straight back (logging the knock in the Block Log as before).
 *
 * The service requires the one-time system VPN consent for usafe
 * itself (requested from the dashboard when the toggle is first
 * turned on); without it, establish() simply fails and everything
 * here is a silent no-op — a failure must never crash or wedge
 * anything.
 */
class ShieldVpnService : VpnService() {

    private var firewallDescriptor: ParcelFileDescriptor? = null
    private var firewallThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_FIREWALL) {
            val blocked = intent.getStringArrayExtra(EXTRA_BLOCKED_PACKAGES)
                ?.toSet() ?: emptySet()
            handleFirewallRequest(blocked)
            return START_NOT_STICKY
        }
        // A plain (knockdown) start: if the firewall is desired or
        // already running, (re)establishing IT is the correct knock —
        // it disconnects the foreign VPN and stays up.
        val desired = PerAppFirewall.desiredBlocked
        if (desired.isNotEmpty() || firewallDescriptor != null) {
            handleFirewallRequest(desired)
            return START_NOT_STICKY
        }
        performKnockdown()
        return START_NOT_STICKY
    }

    /** #51: establish-and-immediately-close; the establish itself is
     *  what disconnects the foreign VPN. */
    private fun performKnockdown() {
        try {
            val descriptor = Builder()
                .setSession(getString(R.string.app_name))
                // A local interface address is required to establish;
                // with no routes added it never sees a packet.
                .addAddress(VPN_ADDRESS, 32)
                .establish()
            // The establish above is what disconnected the foreign
            // VPN. Tear ours down in the same breath: we never stay
            // connected in knockdown mode.
            try {
                descriptor?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not close knockdown TUN", e)
            }
        } catch (e: Exception) {
            // No consent, OEM refusal, raced state — the knock simply
            // did not happen; the monitor will retry after its
            // cooldown.
            Log.w(TAG, "VPN knockdown establish failed", e)
        } finally {
            stopSelf()
        }
    }

    /** #63: build, rebuild or tear down the persistent firewall TUN
     *  so it matches [blocked]. */
    private fun handleFirewallRequest(blocked: Set<String>) {
        if (blocked.isEmpty()) {
            teardownFirewall()
            stopSelf()
            return
        }
        if (firewallDescriptor != null && blocked == PerAppFirewall.appliedBlocked) {
            // Already running with exactly this set.
            return
        }
        teardownFirewall()
        try {
            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .addAddress(VPN_ADDRESS, 32)
                // Full route, but addAllowedApplication below limits
                // the TUN to the blocked apps only — all other apps
                // keep their normal connectivity.
                .addRoute("0.0.0.0", 0)
            var allowedAny = false
            for (pkg in blocked) {
                try {
                    builder.addAllowedApplication(pkg)
                    allowedAny = true
                } catch (_: PackageManager.NameNotFoundException) {
                    // App uninstalled since the rule was made: skip.
                }
            }
            if (!allowedAny) {
                stopSelf()
                return
            }
            val descriptor = builder.establish()
            if (descriptor == null) {
                Log.w(TAG, "Firewall establish returned null")
                stopSelf()
                return
            }
            firewallDescriptor = descriptor
            PerAppFirewall.appliedBlocked = blocked
            PerAppFirewall.isActive = true
            startDropLoop(descriptor)
        } catch (e: Exception) {
            Log.w(TAG, "Firewall establish failed", e)
            PerAppFirewall.isActive = false
            PerAppFirewall.appliedBlocked = emptySet()
            stopSelf()
        }
    }

    /**
     * The drop loop: read every packet routed into the TUN and
     * discard it. Blocked apps' connections simply time out, which
     * is exactly "no internet" from their point of view.
     */
    private fun startDropLoop(descriptor: ParcelFileDescriptor) {
        val input = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
        val thread = Thread {
            val buffer = ByteArray(32767)
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val read = input.read(buffer)
                    if (read < 0) break
                }
            } catch (_: Exception) {
                // TUN closed under us (teardown/revoke): exit quietly.
            }
        }
        thread.isDaemon = true
        thread.name = "usafe-firewall-drop"
        thread.start()
        firewallThread = thread
    }

    private fun teardownFirewall() {
        try {
            firewallThread?.interrupt()
        } catch (_: Exception) {
        }
        firewallThread = null
        try {
            firewallDescriptor?.close()
        } catch (_: Exception) {
        }
        firewallDescriptor = null
        PerAppFirewall.isActive = false
        PerAppFirewall.appliedBlocked = emptySet()
    }

    override fun onRevoke() {
        // A foreign VPN (or the user, in Settings) displaced us. Mark
        // the firewall down; the accessibility service notices the
        // foreign VPN transport and its knockdown start brings the
        // firewall back via the desired set (see onStartCommand).
        teardownFirewall()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        teardownFirewall()
        super.onDestroy()
    }

    companion object {
        const val ACTION_FIREWALL = "com.example.service.action.PER_APP_FIREWALL"
        const val EXTRA_BLOCKED_PACKAGES = "com.example.service.extra.BLOCKED_PACKAGES"

        private const val TAG = "ShieldVpn"
        private const val VPN_ADDRESS = "10.253.253.1"
    }
}
