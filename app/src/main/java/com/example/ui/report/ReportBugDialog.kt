package com.example.ui.report

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary

/** Where shake-to-report mails go (#22 — the developer's own inbox). */
const val REPORT_BUG_EMAIL = "mehdi.sarmadi2019@gmail.com"

/** Fixed subject of every shake-to-report mail (#22 — exactly this). */
const val REPORT_BUG_SUBJECT = "گزارش خطا"

private const val GMAIL_PACKAGE = "com.google.android.gm"

/**
 * Shake-to-report box (#22): opened by a deliberate HARD shake inside the
 * app (see [com.example.util.ShakeDetector]). The user describes the
 * problem, taps Send, and a Gmail draft opens addressed to the developer
 * with the fixed subject «گزارش خطا» and the typed text as the body.
 *
 * This is an ordinary text field on purpose: the #5/#8 anti-cheat rules
 * apply to type-to-confirm fields only — here the user WANTS paste and
 * voice typing to work.
 */
@Composable
fun ReportBugDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reportText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_bug_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianBg,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = tr("Report a Problem", "گزارش خطا"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = ObsidianTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        "What went wrong? Describe it and tap Send — Gmail opens ready to send it to the developer.",
                        "چه مشکلی پیش اومده؟ بنویس و ارسال رو بزن — جیمیل آمادهٔ فرستادن به توسعه‌دهنده باز می‌شه."
                    ),
                    fontSize = 12.sp,
                    color = ObsidianTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    placeholder = {
                        Text(tr("Describe the problem…", "مشکل رو اینجا بنویس…"))
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_bug_text_field")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("report_bug_cancel_button")
                    ) {
                        Text(
                            tr("Cancel", "لغو"),
                            color = EmeraldShield,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            val sent = openReportEmail(context, reportText)
                            if (sent) {
                                onDismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    tr(
                                        "No email app found on this phone",
                                        "برنامهٔ ایمیلی روی گوشی پیدا نشد"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = reportText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.testTag("report_bug_send_button")
                    ) {
                        Text(
                            tr("Send", "ارسال"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Opens the report as an email draft: Gmail when it is installed (the
 * owner's instruction), otherwise any mail app via a chooser. Returns
 * false only when no mail app exists at all.
 */
private fun openReportEmail(context: android.content.Context, body: String): Boolean {
    val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$REPORT_BUG_EMAIL")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_BUG_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, REPORT_BUG_SUBJECT)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    // Prefer Gmail: if it can handle the mailto intent, address it directly.
    val gmailIntent = Intent(mailIntent).setPackage(GMAIL_PACKAGE)
    if (gmailIntent.resolveActivity(context.packageManager) != null) {
        return runCatching {
            context.startActivity(gmailIntent)
            true
        }.getOrDefault(false)
    }
    if (mailIntent.resolveActivity(context.packageManager) == null) return false
    return runCatching {
        context.startActivity(
            Intent.createChooser(mailIntent, REPORT_BUG_SUBJECT)
        )
        true
    }.getOrDefault(false)
}
