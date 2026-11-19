package com.example.service

import com.example.service.InstallGuardLogic.AttemptAction
import com.example.service.InstallGuardLogic.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * State-machine + detection tests for [InstallGuardLogic] — item #27:
 * install attempts need a PIN confirm within 10 seconds, else blocked.
 */
class InstallGuardLogicTest {

    private val t0 = 1_000_000L

    // ---- Attempt → prompt ----

    @Test
    fun idleAttempt_startsPromptWithTenSecondWindow() {
        val (state, action) = InstallGuardLogic.onInstallAttempt(State(), t0)
        assertEquals(AttemptAction.PROMPT, action)
        assertEquals(t0 + InstallGuardLogic.CONFIRM_WINDOW_MS, state.promptDeadlineMs)
        assertEquals(10_000L, InstallGuardLogic.CONFIRM_WINDOW_MS)
        assertTrue(InstallGuardLogic.isPromptActive(state, t0 + 9_999L))
        assertFalse(InstallGuardLogic.isPromptActive(state, t0 + 10_000L))
    }

    @Test
    fun attemptDuringPrompt_staysPromptingWithoutExtending() {
        val (prompted, _) = InstallGuardLogic.onInstallAttempt(State(), t0)
        val (state, action) =
            InstallGuardLogic.onInstallAttempt(prompted, t0 + 5_000L)
        assertEquals(AttemptAction.ALREADY_PROMPTING, action)
        // The deadline is NOT extended by repeated attempts.
        assertEquals(prompted.promptDeadlineMs, state.promptDeadlineMs)
    }

    @Test
    fun attemptAfterPromptExpired_promptsAgain() {
        val (prompted, _) = InstallGuardLogic.onInstallAttempt(State(), t0)
        val (state, action) =
            InstallGuardLogic.onInstallAttempt(prompted, t0 + 10_001L)
        assertEquals(AttemptAction.PROMPT, action)
        assertEquals(t0 + 10_001L + InstallGuardLogic.CONFIRM_WINDOW_MS, state.promptDeadlineMs)
    }

    // ---- Confirm ----

    @Test
    fun confirmInsideWindow_grantsOneShotAllowance() {
        val (prompted, _) = InstallGuardLogic.onInstallAttempt(State(), t0)
        val (state, granted) = InstallGuardLogic.onPinConfirmed(prompted, t0 + 9_999L)
        assertTrue(granted)
        assertEquals(0L, state.promptDeadlineMs)
        assertEquals(
            t0 + 9_999L + InstallGuardLogic.ALLOWANCE_WINDOW_MS,
            state.allowanceUntilMs
        )
        assertFalse(state.allowanceEngaged)
        assertTrue(InstallGuardLogic.isAllowanceActive(state, t0 + 10_000L))
    }

    @Test
    fun confirmAfterWindow_grantsNothing() {
        val (prompted, _) = InstallGuardLogic.onInstallAttempt(State(), t0)
        val (state, granted) = InstallGuardLogic.onPinConfirmed(prompted, t0 + 10_000L)
        assertFalse(granted)
        assertEquals(0L, state.allowanceUntilMs)
        assertEquals(0L, state.promptDeadlineMs)
    }

    @Test
    fun confirmWithoutPrompt_grantsNothing() {
        val (state, granted) = InstallGuardLogic.onPinConfirmed(State(), t0)
        assertFalse(granted)
        assertEquals(State(), state)
    }

    // ---- Prompt timeout ----

    @Test
    fun promptClosedAfterExpiry_clearsDeadline() {
        val (prompted, _) = InstallGuardLogic.onInstallAttempt(State(), t0)
        val closed = InstallGuardLogic.onPromptClosed(prompted, t0 + 10_000L)
        assertEquals(0L, closed.promptDeadlineMs)
    }

    @Test
    fun promptClosedWhileActive_keepsDeadline() {
        val (prompted, _) = InstallGuardLogic.onInstallAttempt(State(), t0)
        val closed = InstallGuardLogic.onPromptClosed(prompted, t0 + 5_000L)
        assertEquals(prompted.promptDeadlineMs, closed.promptDeadlineMs)
    }

    // ---- Allowance lifecycle ----

    @Test
    fun attemptWithAllowance_isAllowedAndEngagesIt() {
        val allowance = State(allowanceUntilMs = t0 + 60_000L)
        val (state, action) = InstallGuardLogic.onInstallAttempt(allowance, t0)
        assertEquals(AttemptAction.ALLOW, action)
        assertTrue(state.allowanceEngaged)
    }

    @Test
    fun installUiHidden_consumesEngagedAllowance() {
        val engaged = State(allowanceUntilMs = t0 + 60_000L, allowanceEngaged = true)
        assertEquals(State(), InstallGuardLogic.onInstallUiHidden(engaged))
    }

    @Test
    fun installUiHidden_keepsUnengagedAllowance() {
        // The user confirmed but hasn't started the install yet: closing
        // some other UI must not eat the allowance.
        val allowance = State(allowanceUntilMs = t0 + 60_000L)
        assertEquals(allowance, InstallGuardLogic.onInstallUiHidden(allowance))
    }

    @Test
    fun expiredAllowance_promptsAgain() {
        val allowance = State(allowanceUntilMs = t0 + 60_000L)
        val (_, action) = InstallGuardLogic.onInstallAttempt(allowance, t0 + 60_001L)
        assertEquals(AttemptAction.PROMPT, action)
    }

    @Test
    fun fullFlow_attemptTimeoutAttemptConfirmInstall() {
        // 1. Attempt → prompt. 2. No confirm → window lapses, prompt
        // cleared. 3. New attempt → prompt again. 4. Confirm in time →
        // allowance. 5. Install UI appears → allowed + engaged.
        // 6. Install UI closes → allowance consumed. 7. Next attempt
        // prompts again (one-shot really means one).
        var state = State()
        val a1 = InstallGuardLogic.onInstallAttempt(state, t0)
        state = a1.first
        assertEquals(AttemptAction.PROMPT, a1.second)

        state = InstallGuardLogic.onPromptClosed(state, t0 + 10_000L)
        assertEquals(State(), state)

        val a2 = InstallGuardLogic.onInstallAttempt(state, t0 + 20_000L)
        state = a2.first
        assertEquals(AttemptAction.PROMPT, a2.second)

        val confirmed = InstallGuardLogic.onPinConfirmed(state, t0 + 25_000L)
        state = confirmed.first
        assertTrue(confirmed.second)

        val a3 = InstallGuardLogic.onInstallAttempt(state, t0 + 30_000L)
        state = a3.first
        assertEquals(AttemptAction.ALLOW, a3.second)

        state = InstallGuardLogic.onInstallUiHidden(state)
        assertEquals(State(), state)

        val a4 = InstallGuardLogic.onInstallAttempt(state, t0 + 40_000L)
        assertEquals(AttemptAction.PROMPT, a4.second)
    }

    // ---- Detection ----

    @Test
    fun installerFreshInstallDialog_detected() {
        val texts = listOf(
            "Cool New App",
            "Do you want to install this app?",
            "Cancel", "Install"
        )
        assertTrue(
            InstallGuardLogic.looksLikeInstallConfirmation(texts, ownPackage = "com.example")
        )
    }

    @Test
    fun installerUninstallDialog_notAnInstallAttempt() {
        val texts = listOf("Cool New App", "Do you want to uninstall this app?", "Cancel", "OK")
        assertFalse(
            InstallGuardLogic.looksLikeInstallConfirmation(texts, ownPackage = "com.example")
        )
    }

    @Test
    fun installerUpdateDialog_notAnInstallAttempt() {
        val texts = listOf(
            "Cool New App",
            "Do you want to install this update?",
            "Cancel", "Install"
        )
        assertFalse(
            InstallGuardLogic.looksLikeInstallConfirmation(texts, ownPackage = "com.example")
        )
    }

    @Test
    fun ownAppInstall_neverGated() {
        val texts = listOf(
            "X Blocker",
            "Do you want to install this app?",
            "Cancel", "Install"
        )
        assertFalse(
            InstallGuardLogic.looksLikeInstallConfirmation(
                texts,
                ownPackage = "com.aistudio.shieldblock.qxtvnz"
            )
        )
    }

    @Test
    fun playProgressPage_detected() {
        val texts = listOf("Cool New App", "Downloading…", "Cancel")
        assertTrue(InstallGuardLogic.looksLikePlayInstallInProgress(texts))
    }

    @Test
    fun playProgressWithoutCancel_notDetected() {
        val texts = listOf("Cool New App", "Downloading…")
        assertFalse(InstallGuardLogic.looksLikePlayInstallInProgress(texts))
    }

    @Test
    fun installButtonLabels() {
        assertTrue(InstallGuardLogic.isInstallButtonLabel("Install"))
        assertTrue(InstallGuardLogic.isInstallButtonLabel(" install "))
        assertTrue(InstallGuardLogic.isInstallButtonLabel("نصب"))
        assertFalse(InstallGuardLogic.isInstallButtonLabel("Update"))
        assertFalse(InstallGuardLogic.isInstallButtonLabel("Install the app to continue reading"))
        assertFalse(InstallGuardLogic.isInstallButtonLabel(null))
    }

    @Test
    fun watchedPackages() {
        assertTrue(InstallGuardLogic.isInstallerPackage("com.android.packageinstaller"))
        assertTrue(InstallGuardLogic.isInstallerPackage("com.google.android.packageinstaller"))
        assertTrue(InstallGuardLogic.isPlayStorePackage("com.android.vending"))
        assertFalse(InstallGuardLogic.isWatchedInstallPackage("com.android.settings"))
        assertFalse(InstallGuardLogic.isWatchedInstallPackage("org.mozilla.firefox"))
    }
}
