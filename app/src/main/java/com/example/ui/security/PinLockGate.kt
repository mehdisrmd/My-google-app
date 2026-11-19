package com.example.ui.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ShieldPreferences
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import com.example.util.PinSecurity
import kotlinx.coroutines.delay

/**
 * Full-screen PIN Security Gate (#7) that blocks access to X Blocker until
 * the user proves their PIN. There is NO biometric / device-credential path
 * anymore: the gate is PIN-only, the PIN is mandatory (first entry forces
 * the setup flow: PIN + security question), and a forgotten PIN is recovered
 * by answering the security question. Only salted hashes are ever stored
 * (see ShieldPreferences / PinSecurity).
 */
private enum class PinGateStep {
    ENTER_PIN,
    SETUP_NEW_PIN,
    SETUP_CONFIRM_PIN,
    SETUP_QUESTION,
    SETUP_UNINSTALL_WAIT,
    CATCHUP_QUESTION,
    FORGOT_QUESTION,
    FORGOT_NEW_PIN,
    FORGOT_CONFIRM_PIN
}

@Composable
fun PinLockOverlay(
    preferences: ShieldPreferences,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember {
        mutableStateOf(
            if (preferences.isPinSet()) PinGateStep.ENTER_PIN else PinGateStep.SETUP_NEW_PIN
        )
    }
    var pinInput by remember { mutableStateOf("") }
    var pendingNewPin by remember { mutableStateOf("") }
    var verifiedAnswer by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    // #41 (batch 2): the uninstall-wait length chosen during first
    // setup; 1 hour is the default (see #40).
    var uninstallWaitMinutes by remember { mutableIntStateOf(60) }

    // Brute-force lockout countdown (deadline persisted in preferences).
    var lockoutUntil by remember { mutableStateOf(preferences.getPinLockoutUntilMs()) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockoutUntil) {
        while (System.currentTimeMillis() < lockoutUntil) {
            nowMs = System.currentTimeMillis()
            delay(500L)
        }
        nowMs = System.currentTimeMillis()
    }
    val lockoutRemainingSec = ((lockoutUntil - nowMs).coerceAtLeast(0L) + 999L) / 1000L
    val isLockedOut = step == PinGateStep.ENTER_PIN && lockoutRemainingSec > 0L

    fun goTo(next: PinGateStep) {
        step = next
        pinInput = ""
        errorText = null
    }

    fun confirmPinPad() {
        when (step) {
            PinGateStep.ENTER_PIN -> {
                if (isLockedOut) return
                if (preferences.verifySecurityPin(pinInput)) {
                    preferences.clearPinFailures()
                    lockoutUntil = 0L
                    if (preferences.hasSecurityQuestion()) {
                        onUnlocked()
                    } else {
                        // Migrated install: PIN exists but no recovery
                        // question yet — set one before entering.
                        goTo(PinGateStep.CATCHUP_QUESTION)
                    }
                } else {
                    lockoutUntil = preferences.recordFailedPinAttempt()
                    nowMs = System.currentTimeMillis()
                    pinInput = ""
                    errorText = if (lockoutUntil > nowMs) {
                        tr(
                            "Too many wrong attempts. PIN pad locked for a while.",
                            "تلاش‌های اشتباه زیاد شد. صفحه پین مدتی قفل شد."
                        )
                    } else {
                        tr("Incorrect PIN. Try again.", "پین اشتباه است. دوباره تلاش کنید.")
                    }
                }
            }
            PinGateStep.SETUP_NEW_PIN, PinGateStep.FORGOT_NEW_PIN -> {
                if (pinInput.length >= PinSecurity.MIN_PIN_LENGTH) {
                    pendingNewPin = pinInput
                    goTo(
                        if (step == PinGateStep.SETUP_NEW_PIN) PinGateStep.SETUP_CONFIRM_PIN
                        else PinGateStep.FORGOT_CONFIRM_PIN
                    )
                }
            }
            PinGateStep.SETUP_CONFIRM_PIN -> {
                if (pinInput == pendingNewPin) {
                    questionInput = ""
                    answerInput = ""
                    goTo(PinGateStep.SETUP_QUESTION)
                } else {
                    // goTo() clears errorText, so set the message after it.
                    goTo(PinGateStep.SETUP_NEW_PIN)
                    errorText = tr(
                        "PINs do not match. Start over.",
                        "پین‌ها یکی نیستند. از اول شروع کنید."
                    )
                }
            }
            PinGateStep.FORGOT_CONFIRM_PIN -> {
                if (pinInput == pendingNewPin) {
                    if (preferences.resetPinWithSecurityAnswer(verifiedAnswer, pendingNewPin)) {
                        onUnlocked()
                    } else {
                        goTo(PinGateStep.ENTER_PIN)
                        errorText = tr(
                            "Could not reset the PIN. Try the recovery again.",
                            "بازنشانی پین انجام نشد. بازیابی را دوباره امتحان کنید."
                        )
                    }
                } else {
                    goTo(PinGateStep.FORGOT_NEW_PIN)
                    errorText = tr(
                        "PINs do not match. Start over.",
                        "پین‌ها یکی نیستند. از اول شروع کنید."
                    )
                }
            }
            else -> Unit
        }
    }

    val title = when (step) {
        PinGateStep.ENTER_PIN -> tr("Enter your PIN", "پین خود را وارد کنید")
        PinGateStep.SETUP_NEW_PIN -> tr("Create your PIN", "پین خود را بسازید")
        PinGateStep.SETUP_CONFIRM_PIN, PinGateStep.FORGOT_CONFIRM_PIN ->
            tr("Repeat the PIN", "پین را دوباره وارد کنید")
        PinGateStep.SETUP_QUESTION -> tr("Security Question", "سؤال امنیتی")
        PinGateStep.SETUP_UNINSTALL_WAIT -> tr("Uninstall Wait", "زمان انتظار حذف")
        PinGateStep.CATCHUP_QUESTION -> tr("Set a Security Question", "یک سؤال امنیتی تعیین کنید")
        PinGateStep.FORGOT_QUESTION -> tr("Forgot PIN", "فراموشی پین")
        PinGateStep.FORGOT_NEW_PIN -> tr("Choose a New PIN", "پین جدید انتخاب کنید")
    }
    val subtitle = when (step) {
        PinGateStep.ENTER_PIN -> tr(
            "usafe asks for your PIN every time it opens.",
            "usafe هر بار موقع باز شدن پین شما را می‌خواهد."
        )
        PinGateStep.SETUP_NEW_PIN -> tr(
            "First, set a 4 to 6 digit PIN. You will need it every time you open usafe.",
            "اول یک پین ۴ تا ۶ رقمی تعیین کنید. هر بار موقع باز کردن usafe به آن نیاز دارید."
        )
        PinGateStep.SETUP_CONFIRM_PIN, PinGateStep.FORGOT_CONFIRM_PIN -> tr(
            "Type the same PIN once more to confirm it.",
            "همان پین را یک بار دیگر برای تأیید وارد کنید."
        )
        PinGateStep.SETUP_QUESTION -> tr(
            "If you ever forget your PIN, answering this question lets you set a new one.",
            "اگر پین را فراموش کردید، با جواب این سؤال می‌توانید پین جدید بگذارید."
        )
        PinGateStep.SETUP_UNINSTALL_WAIT -> tr(
            "If you ever request an uninstall, protection can only be turned off after this wait. Choose how long it should be.",
            "اگر روزی درخواست حذف بدهید، محافظ فقط بعد از این مدت انتظار خاموش می‌شود. انتخاب کنید چقدر باشد."
        )
        PinGateStep.CATCHUP_QUESTION -> tr(
            "Your PIN works, but you have no recovery question yet. Set one now so a forgotten PIN can be reset.",
            "پین شما کار می‌کند ولی هنوز سؤال بازیابی ندارید. همین حالا یکی تعیین کنید تا پین فراموش‌شده قابل بازنشانی باشد."
        )
        PinGateStep.FORGOT_QUESTION -> tr(
            "Answer your security question to set a new PIN.",
            "برای گذاشتن پین جدید، به سؤال امنیتی جواب بدهید."
        )
        PinGateStep.FORGOT_NEW_PIN -> tr(
            "Answer accepted. Now choose a new 4 to 6 digit PIN.",
            "جواب قبول شد. حالا یک پین جدید ۴ تا ۶ رقمی انتخاب کنید."
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(20.dp)
            .testTag("pin_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, CyanTelemetry.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CyanTelemetry.copy(alpha = 0.15f))
                        .border(2.dp, CyanTelemetry, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN Lock",
                        tint = CyanTelemetry,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = ObsidianTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary,
                    textAlign = TextAlign.Center
                )

                when (step) {
                    PinGateStep.SETUP_QUESTION, PinGateStep.CATCHUP_QUESTION -> {
                        SecurityQuestionFields(
                            question = questionInput,
                            onQuestionChange = { questionInput = it },
                            answer = answerInput,
                            onAnswerChange = { answerInput = it },
                            showPresets = true
                        )

                        errorText?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val ok = if (step == PinGateStep.SETUP_QUESTION) {
                                    preferences.completePinSetup(pendingNewPin, questionInput, answerInput)
                                } else {
                                    preferences.setSecurityQuestionAndAnswer(questionInput, answerInput)
                                }
                                if (ok) {
                                    if (step == PinGateStep.SETUP_QUESTION) {
                                        // #41 (batch 2): first setup also
                                        // asks the uninstall-wait length
                                        // before entering the app.
                                        goTo(PinGateStep.SETUP_UNINSTALL_WAIT)
                                    } else {
                                        onUnlocked()
                                    }
                                } else {
                                    errorText = tr(
                                        "Write a question and an answer first.",
                                        "اول یک سؤال و یک جواب بنویسید."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldShield,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_question_save_button")
                        ) {
                            Text(
                                tr("Save & Continue", "ذخیره و ادامه"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    PinGateStep.FORGOT_QUESTION -> {
                        Text(
                            text = preferences.getSecurityQuestion(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanTelemetry,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("pin_forgot_question_text")
                        )

                        SecureAnswerField(
                            answer = answerInput,
                            onAnswerChange = { answerInput = it }
                        )

                        errorText?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (preferences.verifySecurityAnswer(answerInput)) {
                                    verifiedAnswer = answerInput
                                    answerInput = ""
                                    goTo(PinGateStep.FORGOT_NEW_PIN)
                                } else {
                                    errorText = tr(
                                        "That answer does not match. Try again.",
                                        "این جواب درست نیست. دوباره تلاش کنید."
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldShield,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_forgot_verify_button")
                        ) {
                            Text(
                                tr("Verify Answer", "بررسی جواب"),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = { goTo(PinGateStep.ENTER_PIN) }) {
                            Text(
                                tr("Back to PIN", "برگشت به پین"),
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    PinGateStep.SETUP_UNINSTALL_WAIT -> {
                        // #41 (batch 2): preset choices for the uninstall
                        // wait; 1 hour is preselected (the #40 default).
                        val waitPresets = listOf(15, 30, 60, 120, 300)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            waitPresets.forEach { presetMinutes ->
                                val isSelected = uninstallWaitMinutes == presetMinutes
                                OutlinedButton(
                                    onClick = { uninstallWaitMinutes = presetMinutes },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) EmeraldShield else ObsidianBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pin_setup_wait_chip_$presetMinutes")
                                ) {
                                    Text(
                                        text = uninstallWaitLabel(presetMinutes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) EmeraldShield else ObsidianTextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                preferences.setUninstallWaitMinutes(uninstallWaitMinutes)
                                onUnlocked()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldShield,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_setup_wait_continue_button")
                        ) {
                            Text(
                                tr("Save & Continue", "ذخیره و ادامه"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        // PIN-pad steps: ENTER / SETUP / FORGOT PIN entry.
                        MaskedPinDisplay(
                            pin = pinInput,
                            hasError = errorText != null,
                            modifier = Modifier.testTag("pin_input_display")
                        )

                        CleanNumericKeypad(
                            onDigitClick = { digit ->
                                if (!isLockedOut && pinInput.length < PinSecurity.MAX_PIN_LENGTH) {
                                    pinInput += digit
                                    errorText = null
                                }
                            },
                            onClearAll = {
                                if (!isLockedOut) {
                                    pinInput = ""
                                    errorText = null
                                }
                            },
                            onConfirm = { confirmPinPad() }
                        )

                        if (isLockedOut) {
                            Text(
                                text = tr(
                                    "Locked — try again in ${lockoutRemainingSec}s",
                                    "قفل شد — ${lockoutRemainingSec} ثانیه دیگر تلاش کنید"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("pin_lockout_text")
                            )
                        }

                        errorText?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { confirmPinPad() },
                            enabled = !isLockedOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldShield,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_unlock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (step) {
                                    PinGateStep.ENTER_PIN -> tr("Unlock with PIN", "بازگشایی با پین")
                                    else -> tr("Continue", "ادامه")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (step == PinGateStep.ENTER_PIN && preferences.hasSecurityQuestion()) {
                            TextButton(
                                onClick = {
                                    answerInput = ""
                                    goTo(PinGateStep.FORGOT_QUESTION)
                                },
                                modifier = Modifier.testTag("pin_forgot_button")
                            ) {
                                Text(
                                    tr("Forgot PIN?", "پین را فراموش کردید؟"),
                                    color = CyanTelemetry
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Bilingual label for one uninstall-wait preset (#41). */
private fun uninstallWaitLabel(minutes: Int): String = when (minutes) {
    15 -> tr("15 minutes", "۱۵ دقیقه")
    30 -> tr("30 minutes", "۳۰ دقیقه")
    60 -> tr("1 hour (default)", "۱ ساعت (پیش‌فرض)")
    120 -> tr("2 hours", "۲ ساعت")
    300 -> tr("5 hours", "۵ ساعت")
    else -> tr("$minutes minutes", "$minutes دقیقه")
}

/** Masked "● ● ●" readout for the PIN pad (never shows the digits). */
@Composable
private fun MaskedPinDisplay(
    pin: String,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = BorderStroke(
            1.dp,
            if (hasError) CrimsonPrimary else CyanTelemetry.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val masked = if (pin.isEmpty()) {
                tr("Tap numbers below (PIN)", "اعداد پین را لمس کنید")
            } else {
                "● ".repeat(pin.length).trim()
            }
            Text(
                text = masked,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (pin.isEmpty()) ObsidianTextSecondary else CyanTelemetry
            )
        }
    }
}

/** Question + answer fields used by setup / catch-up / settings flows. */
@Composable
private fun SecurityQuestionFields(
    question: String,
    onQuestionChange: (String) -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    showPresets: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showPresets) {
            val presets = listOf(
                tr("What was the name of your first pet?", "اسم اولین حیوان خانگی‌ات چه بود؟"),
                tr("In what city were you born?", "در چه شهری به دنیا آمدی؟"),
                tr("What is your favorite movie?", "فیلم مورد علاقه‌ات چیست؟")
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.forEach { preset ->
                    OutlinedButton(
                        onClick = { onQuestionChange(preset) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianTextSecondary
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            label = {
                Text(tr("Security question", "سؤال امنیتی"), color = ObsidianTextSecondary)
            },
            singleLine = true,
            colors = pinTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_question_field")
        )

        SecureAnswerField(answer = answer, onAnswerChange = onAnswerChange)
    }
}

@Composable
private fun SecureAnswerField(
    answer: String,
    onAnswerChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        label = {
            Text(tr("Your answer", "جواب شما"), color = ObsidianTextSecondary)
        },
        singleLine = true,
        colors = pinTextFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pin_answer_field")
    )
}

@Composable
private fun pinTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanTelemetry,
    unfocusedBorderColor = ObsidianBorder,
    focusedTextColor = ObsidianTextPrimary,
    unfocusedTextColor = ObsidianTextPrimary,
    cursorColor = CyanTelemetry,
    focusedContainerColor = ObsidianCard,
    unfocusedContainerColor = ObsidianCard,
    focusedLabelColor = CyanTelemetry,
    unfocusedLabelColor = ObsidianTextSecondary
)

/**
 * Custom In-App Keyboard / Keypad without any DEL or CTRL buttons.
 */
@Composable
fun CleanNumericKeypad(
    onDigitClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLR", "0", "OK")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clean_numeric_keypad"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    val isAction = key == "CLR" || key == "OK"
                    Surface(
                        onClick = {
                            when (key) {
                                "CLR" -> onClearAll()
                                "OK" -> onConfirm()
                                else -> onDigitClick(key)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = when (key) {
                            "OK" -> EmeraldShield.copy(alpha = 0.22f)
                            "CLR" -> IndigoAccent.copy(alpha = 0.18f)
                            else -> ObsidianCard
                        },
                        border = BorderStroke(
                            1.dp,
                            when (key) {
                                "OK" -> EmeraldShield
                                "CLR" -> IndigoAccent
                                else -> ObsidianBorder
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("keypad_btn_${key.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (key) {
                                    "CLR" -> tr("CLEAR", "پاک")
                                    "OK" -> tr("OK", "تایید")
                                    else -> key
                                },
                                style = if (isAction) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = when (key) {
                                    "OK" -> EmeraldShield
                                    "CLR" -> IndigoAccent
                                    else -> ObsidianTextPrimary
                                },
                                fontSize = if (isAction) 11.sp else 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen solid black curtain rendered when the app enters background / Recents
 * so no external screen-reader or AI agent can inspect the preview frame.
 */
@Composable
fun BlackoutPrivacyCurtain() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("anti_agent_blackout_curtain"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Black Screen Privacy Active",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "X BLOCKER • BLACKOUT PRIVACY SHIELD",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color(0xFF1E293B)
            )
        }
    }
}

/**
 * Dashboard section: Anti-Agent blackout shield + the PIN gate card (#7).
 * The PIN gate is MANDATORY — there is no on/off switch and no fingerprint
 * option anymore; this card only offers Change PIN, Security Question and
 * Lock App Now.
 */
@Composable
fun PinAndPrivacyDashboardSection(
    preferences: ShieldPreferences,
    isBlackoutPrivacyShieldEnabled: Boolean,
    onLockAppNow: () -> Unit,
    onToggleBlackoutShield: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Simplified Anti-Agent Screenshot Block (Single ON/OFF Button + Clear Explanation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("blackout_privacy_shield_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                if (isBlackoutPrivacyShieldEnabled) EmeraldShield.copy(alpha = 0.5f) else ObsidianBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isBlackoutPrivacyShieldEnabled) EmeraldShield.copy(alpha = 0.18f)
                                else ObsidianCard
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isBlackoutPrivacyShieldEnabled) EmeraldShield else ObsidianTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Anti-Agent & Screenshot Blocker", "ضد اسکرین‌شات و مخفی‌ساز صفحه"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tr(
                                "Turn ON so AI agents, screen recorders, and screenshots only see a black screen (FLAG_SECURE). Turn OFF to allow normal screen viewing and screenshots.",
                                "با روشن کردن این گزینه، هوش مصنوعی، ضبط‌کننده‌های صفحه و اسکرین‌شات فقط یک صفحه سیاه می‌بینند. برای مشاهده عادی و گرفتن اسکرین‌شات آن را خاموش بگذارید."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Switch(
                    checked = isBlackoutPrivacyShieldEnabled,
                    onCheckedChange = onToggleBlackoutShield,
                    modifier = Modifier.testTag("blackout_privacy_shield_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = EmeraldShield
                    )
                )
            }
        }

        // 2. PIN Lock Card (always on — no toggle, no fingerprint)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_lock_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanTelemetry.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("PIN Lock", "قفل پین"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "A PIN is required every time usafe opens. Fingerprint unlock is not available.",
                                "هر بار برای ورود به usafe پین لازم است. بازگشایی با اثر انگشت وجود ندارد."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_change_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Change PIN", "تغییر پین"),
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianTextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = { showSecurityQuestionDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_security_question_button")
                    ) {
                        Text(
                            text = tr("Security Question", "سؤال امنیتی"),
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianTextPrimary
                        )
                    }
                }

                Button(
                    onClick = onLockAppNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_lock_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Lock App Now", "قفل فوری برنامه"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            preferences = preferences,
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showSecurityQuestionDialog) {
        ChangeSecurityQuestionDialog(
            preferences = preferences,
            onDismiss = { showSecurityQuestionDialog = false }
        )
    }
}

private enum class PinChangeStep { CURRENT, NEW, CONFIRM }

/**
 * Change-PIN dialog: prove the current PIN on the keypad, then enter and
 * repeat the new one. The PIN is only ever shown masked.
 */
@Composable
private fun ChangePinDialog(
    preferences: ShieldPreferences,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(PinChangeStep.CURRENT) }
    var pinInput by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun confirm() {
        when (step) {
            PinChangeStep.CURRENT -> {
                if (preferences.verifySecurityPin(pinInput)) {
                    currentPin = pinInput
                    pinInput = ""
                    errorText = null
                    step = PinChangeStep.NEW
                } else {
                    pinInput = ""
                    errorText = tr("Incorrect PIN. Try again.", "پین اشتباه است. دوباره تلاش کنید.")
                }
            }
            PinChangeStep.NEW -> {
                if (pinInput.length >= PinSecurity.MIN_PIN_LENGTH) {
                    newPin = pinInput
                    pinInput = ""
                    errorText = null
                    step = PinChangeStep.CONFIRM
                }
            }
            PinChangeStep.CONFIRM -> {
                if (pinInput == newPin) {
                    if (preferences.changeSecurityPin(currentPin, newPin)) {
                        onDismiss()
                    } else {
                        errorText = tr("Could not change the PIN.", "تغییر پین انجام نشد.")
                    }
                } else {
                    pinInput = ""
                    newPin = ""
                    errorText = tr("PINs do not match. Start over.", "پین‌ها یکی نیستند. از اول شروع کنید.")
                    step = PinChangeStep.NEW
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = tr("Change PIN", "تغییر پین"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = when (step) {
                        PinChangeStep.CURRENT -> tr(
                            "Enter your current PIN:",
                            "پین فعلی را وارد کنید:"
                        )
                        PinChangeStep.NEW -> tr(
                            "Enter a new 4 to 6 digit PIN:",
                            "یک پین جدید ۴ تا ۶ رقمی وارد کنید:"
                        )
                        PinChangeStep.CONFIRM -> tr(
                            "Repeat the new PIN:",
                            "پین جدید را دوباره وارد کنید:"
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
                MaskedPinDisplay(pin = pinInput, hasError = errorText != null)
                CleanNumericKeypad(
                    onDigitClick = { d ->
                        if (pinInput.length < PinSecurity.MAX_PIN_LENGTH) {
                            pinInput += d
                            errorText = null
                        }
                    },
                    onClearAll = {
                        pinInput = ""
                        errorText = null
                    },
                    onConfirm = { confirm() }
                )
                errorText?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { confirm() },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldShield)
            ) {
                Text(
                    tr("Continue", "ادامه"),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
            }
        }
    )
}

/**
 * Change security question dialog: the current PIN must be proven first,
 * then a new question + answer can be saved (the stored answer is hashed,
 * so it is never shown — a fresh answer is always typed).
 */
@Composable
private fun ChangeSecurityQuestionDialog(
    preferences: ShieldPreferences,
    onDismiss: () -> Unit
) {
    var pinVerified by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf(preferences.getSecurityQuestion()) }
    var answerInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = tr("Security Question", "سؤال امنیتی"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!pinVerified) {
                    Text(
                        text = tr(
                            "Enter your current PIN to change the security question:",
                            "برای تغییر سؤال امنیتی، پین فعلی را وارد کنید:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                    MaskedPinDisplay(pin = pinInput, hasError = errorText != null)
                    CleanNumericKeypad(
                        onDigitClick = { d ->
                            if (pinInput.length < PinSecurity.MAX_PIN_LENGTH) {
                                pinInput += d
                                errorText = null
                            }
                        },
                        onClearAll = {
                            pinInput = ""
                            errorText = null
                        },
                        onConfirm = {
                            if (preferences.verifySecurityPin(pinInput)) {
                                pinVerified = true
                                pinInput = ""
                                errorText = null
                            } else {
                                pinInput = ""
                                errorText = tr(
                                    "Incorrect PIN. Try again.",
                                    "پین اشتباه است. دوباره تلاش کنید."
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = tr(
                            "Pick a question and type its answer. The answer is stored protected and never shown.",
                            "یک سؤال انتخاب کنید و جوابش را بنویسید. جواب به‌صورت محافظت‌شده ذخیره می‌شود و هرگز نمایش داده نمی‌شود."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                    SecurityQuestionFields(
                        question = questionInput,
                        onQuestionChange = { questionInput = it },
                        answer = answerInput,
                        onAnswerChange = { answerInput = it },
                        showPresets = true
                    )
                }
                errorText?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            if (pinVerified) {
                Button(
                    onClick = {
                        if (preferences.setSecurityQuestionAndAnswer(questionInput, answerInput)) {
                            onDismiss()
                        } else {
                            errorText = tr(
                                "Write a question and an answer first.",
                                "اول یک سؤال و یک جواب بنویسید."
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldShield)
                ) {
                    Text(
                        tr("Save", "ذخیره"),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
            }
        }
    )
}
