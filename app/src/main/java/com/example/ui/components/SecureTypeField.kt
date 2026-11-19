package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.example.MainActivity
import com.example.util.SecureTextInputFilter
import com.example.util.TextDiff

/**
 * Anti-cheat text field for type-to-confirm gates (batch #5, #8, #9).
 *
 * Used wherever the user must reproduce a long text key by key before a
 * protection can be turned off:
 * - #5: while this field is on screen the hosting window carries
 *   FLAG_SECURE (screenshots / screen recording show black), and paste is
 *   blocked — the text toolbar (which hosts the Paste action) is hidden and
 *   bulk insertions are rejected by [SecureTextInputFilter].
 * - #8: voice typing is discouraged via plain text keyboard options, and
 *   dictation commits — which arrive as bulk insertions — are rejected by
 *   the same filter. Compose/Android has no API to fully disable an IME's
 *   voice mode; the bulk-insert rejection is the enforcement layer.
 * - #9: when [targetSentence] is given, the part of the typed text that
 *   differs from the target is rendered with an orange underline, updated
 *   live (see [TextDiff]).
 */
@Composable
fun SecureTypeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    targetSentence: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    SecureWindowEffect()

    val underlineRange = remember(value, targetSentence) {
        targetSentence?.let { TextDiff.diff(value, it).underlineRange }
    }
    val visualTransformation = remember(underlineRange) {
        if (underlineRange == null) {
            VisualTransformation.None
        } else {
            MismatchUnderlineTransformation(underlineRange)
        }
    }

    // Hide the floating text toolbar inside this field: it is the UI that
    // offers Paste, so suppressing it removes the visible paste path. The
    // bulk-insert filter below blocks paste content even when an IME or a
    // hardware keyboard injects it without the toolbar.
    CompositionLocalProvider(LocalTextToolbar provides EmptyTextToolbar) {
        OutlinedTextField(
            value = value,
            onValueChange = { proposed ->
                onValueChange(SecureTextInputFilter.filter(value, proposed))
            },
            modifier = modifier,
            placeholder = placeholder,
            label = label,
            singleLine = singleLine,
            shape = shape,
            colors = colors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            visualTransformation = visualTransformation
        )
    }
}

/** Orange used to mark the part of the typed text that differs (#9). */
private val MismatchOrange = Color(0xFFFF6D00)

/**
 * Applies an orange underline (and matching text color, since Compose ties
 * underline color to text color) over [range] of the field's text. The text
 * itself is unchanged, so [OffsetMapping.Identity] keeps cursor mapping exact.
 */
private class MismatchUnderlineTransformation(
    private val range: IntRange
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val start = range.first.coerceIn(0, text.length)
        val end = (range.last + 1).coerceIn(start, text.length)
        if (end <= start) return TransformedText(text, OffsetMapping.Identity)
        val highlighted = AnnotatedString.Builder(text).apply {
            addStyle(
                SpanStyle(
                    color = MismatchOrange,
                    textDecoration = TextDecoration.Underline
                ),
                start,
                end
            )
        }.toAnnotatedString()
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

/** A [TextToolbar] that never shows a menu — no Copy/Paste/Cut affordance. */
private object EmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        // Intentionally empty: secure fields expose no text toolbar menu.
    }

    override fun hide() = Unit
}

/**
 * Sets FLAG_SECURE on the hosting activity's window while composed (#5),
 * and restores the window's previous secure state when disposed:
 * MainActivity re-syncs the flag from the user's Blackout Privacy Shield
 * preference; any other host gets its pre-existing flag state back.
 */
@Composable
private fun SecureWindowEffect() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val window = activity?.window
        val wasSecure = window != null &&
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (window != null) {
                when {
                    activity is MainActivity -> activity.syncWindowSecureFlag()
                    !wasSecure -> window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
