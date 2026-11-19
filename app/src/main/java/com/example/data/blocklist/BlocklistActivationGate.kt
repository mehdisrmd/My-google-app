package com.example.data.blocklist

/**
 * The activation gate for the blocklist (#50, batch 3 — replaces the
 * batch-2 #29 "activation-blocked screen").
 *
 * Entry into the app is NEVER gated on the list: if the first-launch
 * download fails or is skipped, the user walks in and can use every
 * screen. The one place the list is demanded is ACTIVATION — turning
 * protection ON from the master switch. While the on-disk list is still
 * only the built-in fallback seed, activation is refused with a message
 * telling the user to update or add a blocklist repository first.
 *
 * "Still only the seed" is read off the update timestamp convention the
 * updater and preferences already keep: the fallback seed records its
 * domain count but leaves the last-updated timestamp at 0 ("never
 * updated from the internet"); any successful download — first-launch,
 * manual, or the weekly worker — stamps it. So the gate condition is
 * exactly: no real update has ever landed.
 */
object BlocklistActivationGate {

    /**
     * True when turning protection ON must be refused because no real
     * blocklist update has ever completed ([lastUpdatedMs] is the
     * persisted adult-blocklist update timestamp, 0 = never).
     */
    fun isActivationBlocked(lastUpdatedMs: Long): Boolean = lastUpdatedMs <= 0L
}
