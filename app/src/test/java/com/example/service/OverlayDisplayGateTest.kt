package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision-table tests for [OverlayDisplayGate] — items #17 (incoming call
 * yields the auxiliary pages), #18 (no block screen while the phone is
 * locked) and #57 (batch 5: the block screen itself no longer yields to
 * a call — it stays).
 */
class OverlayDisplayGateTest {

    // ---- decide(): the full truth table ----

    @Test
    fun unlockedAndNoCall_showsTheOverlay() {
        assertEquals(
            OverlayDisplayGate.Decision.SHOW,
            OverlayDisplayGate.decide(keyguardLocked = false, callActive = false)
        )
    }

    @Test
    fun lockedPhone_defersUntilUnlock() {
        assertEquals(
            OverlayDisplayGate.Decision.DEFER_UNTIL_UNLOCK,
            OverlayDisplayGate.decide(keyguardLocked = true, callActive = false)
        )
    }

    @Test
    fun activeCall_yieldsSoTheUserCanAnswer() {
        assertEquals(
            OverlayDisplayGate.Decision.YIELD_TO_CALL,
            OverlayDisplayGate.decide(keyguardLocked = false, callActive = true)
        )
    }

    @Test
    fun lockedAndRinging_callWinsOverTheKeyguard() {
        // A locked phone that rings must still present its call UI; the
        // block overlay stands down for the call, not just for the unlock.
        assertEquals(
            OverlayDisplayGate.Decision.YIELD_TO_CALL,
            OverlayDisplayGate.decide(keyguardLocked = true, callActive = true)
        )
    }

    // ---- decideBlockScreen(): #57 (batch 5) — the block screen stays ----

    @Test
    fun blockScreen_unlocked_shows() {
        assertEquals(
            OverlayDisplayGate.Decision.SHOW,
            OverlayDisplayGate.decideBlockScreen(keyguardLocked = false)
        )
    }

    @Test
    fun blockScreen_lockedPhone_stillDefersUntilUnlock() {
        // #18 is unchanged for the block screen: never draw over the
        // lock screen.
        assertEquals(
            OverlayDisplayGate.Decision.DEFER_UNTIL_UNLOCK,
            OverlayDisplayGate.decideBlockScreen(keyguardLocked = true)
        )
    }

    @Test
    fun blockScreen_neverYieldsToCall_unlikeTheAuxiliaryPages() {
        // The whole point of #57: for the same inputs where decide()
        // yields to a call, the block-screen table has no call input
        // at all — its only non-SHOW outcome is the keyguard defer.
        assertEquals(
            OverlayDisplayGate.Decision.YIELD_TO_CALL,
            OverlayDisplayGate.decide(keyguardLocked = false, callActive = true)
        )
        assertEquals(
            OverlayDisplayGate.Decision.SHOW,
            OverlayDisplayGate.decideBlockScreen(keyguardLocked = false)
        )
    }

    // ---- isCallAudioMode(): AudioManager mode values ----

    @Test
    fun audioModes_ringtoneInCallAndCommunicationAreCalls() {
        // MODE_NORMAL = 0 is not a call; MODE_RINGTONE = 1,
        // MODE_IN_CALL = 2 and MODE_IN_COMMUNICATION = 3 are.
        assertFalse(OverlayDisplayGate.isCallAudioMode(0))
        assertTrue(OverlayDisplayGate.isCallAudioMode(1))
        assertTrue(OverlayDisplayGate.isCallAudioMode(2))
        assertTrue(OverlayDisplayGate.isCallAudioMode(3))
        assertFalse(OverlayDisplayGate.isCallAudioMode(4))
        assertFalse(OverlayDisplayGate.isCallAudioMode(-1))
    }

    // ---- isCallUiPackage(): dialer / in-call packages ----

    @Test
    fun callUiPackages_areRecognized() {
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.android.incallui"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.samsung.android.incallui"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.android.dialer"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.google.android.dialer"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.samsung.android.dialer"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.android.phone"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.android.server.telecom"))
        assertTrue(OverlayDisplayGate.isCallUiPackage("com.sec.phone"))
    }

    @Test
    fun nonCallPackages_areNotCallUi() {
        assertFalse(OverlayDisplayGate.isCallUiPackage("com.android.chrome"))
        assertFalse(OverlayDisplayGate.isCallUiPackage("com.android.systemui"))
        assertFalse(OverlayDisplayGate.isCallUiPackage("com.aistudio.shieldblock.qxtvnz"))
        // Messaging is not call UI: a text conversation must not stand
        // the block screen down the way a ringing phone does.
        assertFalse(OverlayDisplayGate.isCallUiPackage("com.google.android.apps.messaging"))
        assertFalse(OverlayDisplayGate.isCallUiPackage("com.android.mms"))
        assertFalse(OverlayDisplayGate.isCallUiPackage(null))
        assertFalse(OverlayDisplayGate.isCallUiPackage(""))
        assertFalse(OverlayDisplayGate.isCallUiPackage("   "))
    }
}
