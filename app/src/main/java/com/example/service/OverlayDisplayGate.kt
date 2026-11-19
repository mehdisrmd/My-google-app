package com.example.service

/**
 * Pure decision logic for whether the block (lockdown) overlay may be shown
 * at a given moment — items #17 and #18.
 *
 * The block screen must never fight two system surfaces:
 *  - #17: an incoming / ongoing phone call. The call UI always wins, so the
 *    user can actually answer. The overlay stands down while a call is
 *    ringing or in progress and comes back afterwards (the enforcement loop
 *    re-launches it) if the lock is still running.
 *  - #18: the lock screen. While the keyguard is locked the block screen
 *    must NOT be drawn over it; the launch is deferred until the user has
 *    unlocked the phone, and only then shown if the lock still applies.
 *
 * A call takes precedence over the keyguard: a phone can be locked AND
 * ringing, and in that state the call UI (which has its own
 * show-when-locked presentation) is the one surface that must stay
 * reachable.
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
