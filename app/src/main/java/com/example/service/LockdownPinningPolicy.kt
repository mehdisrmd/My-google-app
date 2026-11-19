package com.example.service

/**
 * #39 (batch 2): what "enforcing a running lock" means, split by the
 * Strict Lock Mode toggle.
 *
 * The bug: the accessibility service used to pin the WHOLE phone to the
 * block screen during any lockdown, whether or not Strict Lock Mode was
 * on — the strict toggle was never consulted on the enforcement path.
 *
 * Now:
 *  - Strict ON: the whole phone stays pinned — only X Blocker and the
 *    temporary Call/SMS allowance may come to the front.
 *  - Strict OFF: a block closes only the offending app (the trigger
 *    paths already BACK/HOME out of it when the block starts). During
 *    the lock, only that same app — recorded as the lock's package — is
 *    met by the block page again when it returns to the foreground.
 *    Every other app stays fully usable.
 *
 * Pure decisions so the semantics are unit-testable; the service feeds
 * them the live values.
 */
object LockdownPinningPolicy {

    /** Strict mode pins the whole phone for the lock's whole duration. */
    fun shouldPinWholePhone(strictModeEnabled: Boolean): Boolean = strictModeEnabled

    /**
     * Non-strict mode: the block page is re-shown only when the lock's
     * own offending app is the foreground app. Locks that recorded no
     * package (scheduled focus, voluntary panic) never re-block an app.
     */
    fun shouldReblockForegroundApp(
        strictModeEnabled: Boolean,
        activeLockPackage: String?,
        foregroundPackage: String?
    ): Boolean {
        if (strictModeEnabled) return false
        if (activeLockPackage.isNullOrBlank() || foregroundPackage.isNullOrBlank()) return false
        return activeLockPackage == foregroundPackage
    }
}
