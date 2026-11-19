package com.example.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #39 (batch 2): with Strict Lock Mode OFF a block must close only the
 * offending app — the whole-phone pinning is a strict-mode behavior.
 */
class LockdownPinningPolicyTest {

    @Test
    fun strictMode_pinsWholePhone() {
        assertTrue(LockdownPinningPolicy.shouldPinWholePhone(strictModeEnabled = true))
    }

    @Test
    fun nonStrictMode_neverPinsWholePhone() {
        assertFalse(LockdownPinningPolicy.shouldPinWholePhone(strictModeEnabled = false))
    }

    @Test
    fun nonStrictMode_reblocksOnlyTheOffendingApp() {
        assertTrue(
            LockdownPinningPolicy.shouldReblockForegroundApp(
                strictModeEnabled = false,
                activeLockPackage = "com.android.chrome",
                foregroundPackage = "com.android.chrome"
            )
        )
        assertFalse(
            LockdownPinningPolicy.shouldReblockForegroundApp(
                strictModeEnabled = false,
                activeLockPackage = "com.android.chrome",
                foregroundPackage = "org.telegram.messenger"
            )
        )
    }

    @Test
    fun strictMode_reblockCheckStaysOff_becausePinningCoversIt() {
        assertFalse(
            LockdownPinningPolicy.shouldReblockForegroundApp(
                strictModeEnabled = true,
                activeLockPackage = "com.android.chrome",
                foregroundPackage = "com.android.chrome"
            )
        )
    }

    @Test
    fun locksWithoutAPackage_neverReblockAnApp() {
        // Scheduled focus / voluntary panic locks record no package.
        assertFalse(
            LockdownPinningPolicy.shouldReblockForegroundApp(
                strictModeEnabled = false,
                activeLockPackage = null,
                foregroundPackage = "com.android.chrome"
            )
        )
        assertFalse(
            LockdownPinningPolicy.shouldReblockForegroundApp(
                strictModeEnabled = false,
                activeLockPackage = "",
                foregroundPackage = "com.android.chrome"
            )
        )
        assertFalse(
            LockdownPinningPolicy.shouldReblockForegroundApp(
                strictModeEnabled = false,
                activeLockPackage = "com.android.chrome",
                foregroundPackage = null
            )
        )
    }
}
