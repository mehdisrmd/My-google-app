package com.example.service

/**
 * Pure state machine + screen classification for the Install Guard
 * (item #27).
 *
 * When the guard toggle is ON, an attempt to install a NEW app is
 * interrupted and the user gets [CONFIRM_WINDOW_MS] (10 s) to confirm
 * with the app PIN. A correct PIN inside the window grants a ONE-SHOT
 * allowance: the install the user then starts (within
 * [ALLOWANCE_WINDOW_MS]) proceeds; the allowance is consumed when that
 * install UI closes. No confirmation in time → no allowance → the
 * install stays blocked, and the next attempt prompts again.
 *
 * States (persisted via ShieldPreferences so the service, the ticker and
 * the PIN activity all see the same one):
 *  - idle:      promptDeadlineMs = 0, allowanceUntilMs = 0
 *  - prompted:  promptDeadlineMs = start + 10 s (PIN pad is showing)
 *  - allowance: allowanceUntilMs = confirm + 2 min; [State.allowanceEngaged]
 *               flips true once an install UI is actually seen using it,
 *               so "the install UI closed" can consume exactly that one
 *               install and not a later, unrelated one.
 *
 * Detection coverage — honest limits:
 *  - The classic package installer (com.android.packageinstaller /
 *    com.google.android.packageinstaller, incl. OEM skins containing
 *    "packageinstaller") waits on a confirmation dialog, so watching its
 *    window content is a complete gate: nothing installs before the user
 *    confirms, and the guard interrupts before that.
 *  - Google Play (com.android.vending) starts downloading the moment
 *    "Install" is tapped. The guard therefore watches for the tap itself
 *    (TYPE_VIEW_CLICKED on the Install button) and, as a backstop, for
 *    the download/install-in-progress page appearing. Interrupting sends
 *    the user HOME and shows the PIN pad — but a download Play already
 *    queued may finish in the background and Play may still install it;
 *    without Device Owner (#11) there is no API to cancel it. What the
 *    guard guarantees on Play is that the install cannot be *started
 *    deliberately and quietly*: every fresh attempt is interrupted and
 *    demands the PIN within 10 s.
 *  - The backstop cannot tell a fresh install's progress page from an
 *    UPDATE's progress page (identical UI), so an update started from
 *    Play may also be interrupted once; updates are never blocked for
 *    good — confirming the PIN (or turning the guard off) lets them run.
 *  - Updating X Blocker itself is never gated (its installer screens
 *    mention the app), and uninstall dialogs are the uninstall guard's
 *    job, not this one's.
 */
object InstallGuardLogic {

    /** Time the user has to type the PIN after an install attempt. */
    const val CONFIRM_WINDOW_MS: Long = 10_000L

    /** After a successful PIN confirm, one install may start within this window. */
    const val ALLOWANCE_WINDOW_MS: Long = 120_000L

    data class State(
        val promptDeadlineMs: Long = 0L,
        val allowanceUntilMs: Long = 0L,
        val allowanceEngaged: Boolean = false
    )

    enum class AttemptAction {
        /** An allowance is active: let this install proceed. */
        ALLOW,

        /** No prompt running: interrupt and show the PIN pad. */
        PROMPT,

        /** A prompt is already counting down: just keep interrupting. */
        ALREADY_PROMPTING
    }

    fun isPromptActive(state: State, nowMs: Long): Boolean =
        state.promptDeadlineMs > nowMs

    fun isAllowanceActive(state: State, nowMs: Long): Boolean =
        state.allowanceUntilMs > nowMs

    /** Milliseconds left to confirm; 0 when no prompt is running. */
    fun promptRemainingMs(state: State, nowMs: Long): Long =
        (state.promptDeadlineMs - nowMs).coerceAtLeast(0L)

    /**
     * An install UI was just detected. Decides whether it may proceed,
     * must be interrupted with a fresh prompt, or is already being
     * prompted about. An expired prompt is cleared by the transition.
     */
    fun onInstallAttempt(state: State, nowMs: Long): Pair<State, AttemptAction> {
        if (isAllowanceActive(state, nowMs)) {
            // Mark the allowance as engaged by THIS install so its UI
            // closing consumes it (see onInstallUiHidden).
            return state.copy(allowanceEngaged = true) to AttemptAction.ALLOW
        }
        if (isPromptActive(state, nowMs)) {
            return state to AttemptAction.ALREADY_PROMPTING
        }
        val fresh = State(promptDeadlineMs = nowMs + CONFIRM_WINDOW_MS)
        return fresh to AttemptAction.PROMPT
    }

    /**
     * The user entered the correct PIN. Grants the one-shot allowance
     * only while a prompt is still inside its 10-second window; a late
     * confirm (prompt already expired) grants nothing.
     */
    fun onPinConfirmed(state: State, nowMs: Long): Pair<State, Boolean> {
        if (!isPromptActive(state, nowMs)) {
            return state.copy(promptDeadlineMs = 0L) to false
        }
        val granted = State(
            promptDeadlineMs = 0L,
            allowanceUntilMs = nowMs + ALLOWANCE_WINDOW_MS,
            allowanceEngaged = false
        )
        return granted to true
    }

    /** The prompt window lapsed (or was dismissed) with no confirmation. */
    fun onPromptClosed(state: State, nowMs: Long): State {
        return if (state.promptDeadlineMs in 1..nowMs) {
            state.copy(promptDeadlineMs = 0L)
        } else {
            state
        }
    }

    /**
     * The install UI the guard was watching has gone away. If an
     * allowance had been engaged by it, that one install is done (or
     * abandoned) and the allowance is consumed; an unengaged allowance
     * survives until its window ends so the user can still start the
     * install they confirmed for.
     */
    fun onInstallUiHidden(state: State): State {
        return if (state.allowanceEngaged) {
            State()
        } else {
            state
        }
    }

    // ---- Screen / event classification (pure, text-based) ----

    fun isInstallerPackage(packageName: String): Boolean =
        packageName.lowercase().contains("packageinstaller")

    fun isPlayStorePackage(packageName: String): Boolean =
        packageName.lowercase() == "com.android.vending"

    /** True for packages whose windows the install guard watches at all. */
    fun isWatchedInstallPackage(packageName: String): Boolean =
        isInstallerPackage(packageName) || isPlayStorePackage(packageName)

    /**
     * True when a label is exactly a Play Store / installer "Install"
     * action (English or Persian), as carried by a click event's source
     * node. Deliberately exact: "Install" appears in body text all over
     * Play pages, but only the button's own label is this short string.
     */
    fun isInstallButtonLabel(label: String?): Boolean {
        val clean = label?.trim()?.lowercase() ?: return false
        return clean == "install" || clean == "نصب" || clean == "install app"
    }

    /**
     * True when the classic installer's window shows a fresh-install
     * confirmation/progress UI for some OTHER app. Exclusions: uninstall
     * dialogs (the uninstall guard owns those), updates (the dialog says
     * "update"), and installs/updates of X Blocker itself (its screens
     * mention the app) — updating the blocker must stay possible.
     */
    fun looksLikeInstallConfirmation(screenTexts: List<String>, ownPackage: String = ""): Boolean {
        if (screenTexts.isEmpty()) return false
        val combinedLower = screenTexts.joinToString(" | ").lowercase()
        if (combinedLower.contains("uninstall")) return false
        if (combinedLower.contains("update")) return false

        val ownLower = ownPackage.trim().lowercase()
        val mentionsOwnApp = combinedLower.contains("x blocker") ||
            combinedLower.contains("com.aistudio.shieldblock") ||
            (ownLower.isNotEmpty() && combinedLower.contains(ownLower))
        if (mentionsOwnApp) return false

        val hasInstallButton = screenTexts.any { text ->
            val clean = text.trim().lowercase()
            clean == "install" || clean == "نصب"
        }
        return combinedLower.contains("do you want to install") ||
            combinedLower.contains("installing") ||
            hasInstallButton
    }

    /**
     * Backstop for Play: its in-progress page ("Downloading…" /
     * "Pending…" / "Installing…" with a Cancel action) appeared. Cannot
     * distinguish a fresh install from an update — see the class docs.
     */
    fun looksLikePlayInstallInProgress(screenTexts: List<String>): Boolean {
        if (screenTexts.isEmpty()) return false
        val combinedLower = screenTexts.joinToString(" | ").lowercase()
        if (combinedLower.contains("uninstall")) return false
        val hasProgress = combinedLower.contains("downloading") ||
            combinedLower.contains("pending") ||
            combinedLower.contains("installing") ||
            combinedLower.contains("در حال دانلود") ||
            combinedLower.contains("در حال نصب")
        val hasCancel = screenTexts.any { text ->
            val clean = text.trim().lowercase()
            clean == "cancel" || clean == "لغو"
        }
        return hasProgress && hasCancel
    }
}
