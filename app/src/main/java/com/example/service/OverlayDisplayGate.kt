package com.example.service

/**
 * Pure decision logic for whether an overlay page may be shown at a given
 * moment — items #17 and #18, with #57 (batch 5) splitting the block
 * screen off from the call rule.
 *
 * Two families of pages use this gate:
 *
 *  - The BLOCK SCREEN (LockdownActivity and, since #59 unified the site
 *    blocks onto the same page, BlockWarningActivity) is governed by
 *    [decideBlockScreen]: it must never fight the lock screen (#18 —
 *    while the keyguard is locked the launch is deferred until unlock),
 *    but it NO LONGER stands down for a phone call. #17 originally made
 *    the call UI win so the user could answer; in practice the two
 *    surfaces fought — the block screen finished itself for the call,
 *    the enforcement loop relaunched it, the call UI pushed back — a
 *    visible flicker war. Mehdi's #57 ruling: the flickering stops and
 *    the BLOCK SCREEN STAYS. #17 is superseded for the block screen.
 *
 *  - The auxiliary pages (AppLimitWarningActivity, DataCapWarningActivity,
 *    InstallGuardActivity) keep the original [decide] table: a call
 *    yields (YIELD_TO_CALL — those pages are transient prompts, not the
 *    block screen, and standing down for a call costs nothing), the
 *    keyguard defers. A call takes precedence over the keyguard there:
 *    a phone can be locked AND ringing, and in that state the call UI
 *    (which has its own show-when-locked presentation) is the one
 *    surface that must stay reachable.
 *
 * Everything here is plain Kotlin (no Android types) so the whole decision
 * table is unit-testable; the service feeds it live system state.
 */
object OverlayDisplayGate {

    enum class Decision {
        /** Show the block overlay now. */
        SHOW,

        /** Phone is locked: hold the launch until the user unlocks (#18). */
        DEFER_UNTIL_UNLOCK,

        /** A call is ringing / in progress: keep the overlay out of the way (#17). */
        YIELD_TO_CALL
    }

    fun decide(keyguardLocked: Boolean, callActive: Boolean): Decision {
        return when {
            callActive -> Decision.YIELD_TO_CALL
            keyguardLocked -> Decision.DEFER_UNTIL_UNLOCK
            else -> Decision.SHOW
        }
    }

    /**
     * #57 (batch 5): the BLOCK SCREEN's table. A call is deliberately
     * not an input: whether or not a call is ringing, the block screen
     * stays — it never finishes itself for the call UI and the service
     * never stands its launch down, so the two surfaces cannot enter
     * the finish/relaunch fight #17 produced. Only the keyguard still
     * defers the launch (#18 unchanged: never draw over the lock
     * screen; the parked launch fires on unlock while the lock runs).
     */
    fun decideBlockScreen(keyguardLocked: Boolean): Decision {
        return if (keyguardLocked) Decision.DEFER_UNTIL_UNLOCK else Decision.SHOW
    }

    // AudioManager mode values, mirrored as plain ints so this file stays
    // Android-free: MODE_NORMAL = 0, MODE_RINGTONE = 1, MODE_IN_CALL = 2,
    // MODE_IN_COMMUNICATION = 3. Reading AudioManager.getMode() needs no
    // permission (unlike TelephonyManager call-state callbacks, which need
    // READ_PHONE_STATE), which is why the service detects calls this way.
    fun isCallAudioMode(mode: Int): Boolean {
        return mode == 1 || mode == 2 || mode == 3
    }

    /**
     * True when [packageName] is a dialer / in-call UI package. The audio
     * mode is the primary call signal; this is the backstop for OEM call
     * screens whose audio mode lags the window change. SMS packages are
     * deliberately NOT call UI — a text conversation does not need the
     * overlay to stand down the way a ringing phone does.
     */
    fun isCallUiPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val pkg = packageName.lowercase()
        return pkg.contains("incallui") ||
            pkg.contains(".dialer") ||
            pkg == "com.android.phone" ||
            pkg == "com.android.server.telecom" ||
            pkg == "com.sec.phone"
    }
}
