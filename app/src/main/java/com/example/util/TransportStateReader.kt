package com.example.util

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings

/**
 * #46 (batch 3): reads whether a transport is currently ON.
 *
 * Both readers are best-effort and return null when the state cannot be
 * determined (service unavailable, OEM quirk, exception): callers must
 * treat null as "unknown", never as OFF — the data-cap warning only
 * releases on an explicit OFF observation.
 */
object TransportStateReader {

    fun isOn(context: Context, transport: DataCapEnforcer.Transport): Boolean? =
        when (transport) {
            DataCapEnforcer.Transport.WIFI -> isWifiOn(context)
            DataCapEnforcer.Transport.MOBILE -> isMobileDataOn(context)
        }

    fun isWifiOn(context: Context): Boolean? = try {
        val wifiManager = context.applicationContext
            .getSystemService(WifiManager::class.java)
        wifiManager?.isWifiEnabled
    } catch (e: Exception) {
        null
    }

    /**
     * There is no public API for the mobile-data toggle state; the
     * global setting the Device Owner route writes is readable and is
     * what the system itself consults. -1 / any other value = unknown.
     */
    fun isMobileDataOn(context: Context): Boolean? = try {
        when (Settings.Global.getInt(context.contentResolver, "mobile_data", -1)) { // literal key: no public MOBILE_DATA constant in Settings.Global
            1 -> true
            0 -> false
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
