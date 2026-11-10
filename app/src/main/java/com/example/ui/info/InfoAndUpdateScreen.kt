package com.example.ui.info

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.ShieldPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.BackupRestoreSummary
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary
import com.example.ui.theme.SeasonalMotifCanvas
import com.example.ui.theme.SeasonalPalettes
import com.example.ui.theme.SeasonalTheme
import com.example.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Static app identity shown on the Info screen.
 *
 * There is intentionally NO update checker anymore: the in-app GitHub
 * update feature was removed completely at the owner's request (no
 * release checks, no status, no update prompts). The GitHub entry on
 * this screen is a plain outbound link to the repository, nothing more.
 */
object AppInfo {
    const val CURRENT_APP_VERSION = "0.4"
    const val TELEGRAM_HANDLE = "@mehdisrmd"
    const val TELEGRAM_URL = "https://t.me/mehdisrmd"
    const val GITHUB_REPO_URL = "https://github.com/mehdisrmd/My-google-app"

    fun openUrlSafely(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

@Composable
fun InfoAndUpdateScreen(
    onExportBackupJson: suspend () -> String = { "{}" },
    onImportBackupJson: suspend (String) -> BackupRestoreSummary = {
        BackupRestoreSummary(false, messageEn = "Not configured")
    },
    preferences: ShieldPreferences? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val effectivePrefs = remember(context, preferences) {
        preferences ?: ShieldPreferences(context)
    }
    val seasonalThemeName by effectivePrefs.seasonalThemeName.collectAsStateWithLifecycle()
    val selectedSeasonalTheme = SeasonalTheme.fromName(seasonalThemeName)

    val fullRealGithubUrl = AppInfo.GITHUB_REPO_URL

    var backupStatusBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showPasteBackupDialog by remember { mutableStateOf(false) }

    // SAF Launcher to Save Backup JSON File
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val json = onExportBackupJson()
                val ok = BackupManager.writeBackupToUri(context, uri, json)
                backupStatusBanner = if (ok) {
                    true to tr(
                        "Full backup exported & saved to file successfully!",
                        "فایل پشتیبان کامل اطلاعات با موفقیت ذخیره شد!"
                    )
                } else {
                    false to tr(
                        "Could not write backup file. Use 'Copy JSON' instead.",
                        "خطا در ذخیره فایل. می‌توانید از دکمه کپی پشتیبان استفاده کنید."
                    )
                }
            }
        }
    }

    // SAF Launcher to Import Backup JSON File
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = BackupManager.readBackupFromUri(context, uri)
                if (content.isNullOrBlank()) {
                    backupStatusBanner = false to tr(
                        "Selected file is empty or unreadable.",
                        "فایل انتخاب‌شده خالی یا غیرقابل خواندن است."
                    )
                } else {
                    val summary = onImportBackupJson(content)
                    backupStatusBanner = summary.success to tr(summary.messageEn, summary.messageFa)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("info_and_update_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Redesigned Material Design 3 Top Identity & Developer Header Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_info_version_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                IndigoAccent.copy(alpha = 0.16f),
                                EmeraldShield.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        IndigoAccent.copy(alpha = 0.55f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                                contentDescription = "X Blocker Minimal White Icon",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tr("X Blocker", "ایکس بلاکر"),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = ObsidianTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = EmeraldShield,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = tr(
                                        "Material 3 Content, Reels & Search Shield",
                                        "محافظ هوشمند محتوا، ریلز اینستاگرام و جستجوی تلگرام"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldShield.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.65f)),
                            modifier = Modifier.testTag("app_version_badge")
                        ) {
                            Text(
                                text = "v${AppInfo.CURRENT_APP_VERSION}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = EmeraldShield,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))

                    // Developer Telegram Contact Row inside Top Info Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppInfo.openUrlSafely(context, AppInfo.TELEGRAM_URL)
                            }
                            .testTag("telegram_contact_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(CyanTelemetry.copy(alpha = 0.18f))
                                        .border(1.dp, CyanTelemetry, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Telegram Contact",
                                        tint = CyanTelemetry,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tr("Developer Telegram", "تلگرام سازنده برنامه"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ObsidianTextSecondary
                                    )
                                    Text(
                                        text = AppInfo.TELEGRAM_HANDLE,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Black,
                                        color = CyanTelemetry
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    AppInfo.openUrlSafely(context, AppInfo.TELEGRAM_URL)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanTelemetry,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                modifier = Modifier.testTag("open_telegram_contact_button")
                            ) {
                                Text(
                                    text = "@mehdisrmd",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1b. Appearance — Seasonal Theme Picker (Spring / Summer / Autumn /
        // Winter palettes through the same Liquid Glass theme layer; the
        // original Purple Glass stays the default.)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("seasonal_theme_picker_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IndigoAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = IndigoAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Seasonal Theme", "تم فصلی"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "Recolor the whole app — glass surfaces stay, the palette changes.",
                                "رنگ کل برنامه عوض می‌شود — سطوح شیشه‌ای می‌مانند، فقط پالت رنگ تغییر می‌کند."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                val themeOptions = listOf(
                    SeasonalTheme.PURPLE_GLASS to tr("Purple Glass (Default)", "بنفش شیشه‌ای (پیش‌فرض)"),
                    SeasonalTheme.SPRING to tr("Spring", "بهار"),
                    SeasonalTheme.SUMMER to tr("Summer", "تابستان"),
                    SeasonalTheme.AUTUMN to tr("Autumn", "پاییز"),
                    SeasonalTheme.WINTER to tr("Winter", "زمستان")
                )
                themeOptions.forEach { (theme, label) ->
                    val isSelected = theme == selectedSeasonalTheme
                    val paletteSet = SeasonalPalettes.getValue(theme).dark
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldShield else ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { effectivePrefs.setSeasonalTheme(theme) }
                            .testTag("seasonal_theme_option_${theme.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Motif preview: SEE the season's element
                            // (green leaves / sun / yellow autumn leaves /
                            // snow) before choosing the theme.
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(paletteSet.bg)
                            ) {
                                SeasonalMotifCanvas(
                                    theme = theme,
                                    palette = paletteSet,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            // Two-tone season swatch: primary + shield accent
                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(paletteSet.primary)
                                        .border(1.dp, ObsidianBorder, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(paletteSet.shield)
                                        .border(1.dp, ObsidianBorder, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = ObsidianTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. GitHub Repository Card — a plain outbound link only.
        // (The in-app update checker was removed completely at the owner's
        // request: no release checks, no update status, no update prompts.)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("github_repo_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, EmeraldShield.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldShield.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("GitHub", "گیت‌هاب"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = fullRealGithubUrl,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = CyanTelemetry,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        AppInfo.openUrlSafely(context, fullRealGithubUrl)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_github_repo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        tint = CyanTelemetry,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Open GitHub", "مشاهده در گیت‌هاب"),
                        color = CyanTelemetry,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // 3. Full Backup Export & Import Card (Information Backup & Restore)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_export_import_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IndigoAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup & Restore",
                            tint = IndigoAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(
                                "Backup & Restore App Information",
                                "پشتیبان‌گیری و بازیابی کامل اطلاعات (Export / Import)"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "Export or import your Clean Streak, Best Chain, Limits & Rules",
                                "پشتیبان‌گیری و بازیابی روزهای پاک، بهترین زنجیره، محدودیت‌ها و قوانین"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                // Row 1: Save Backup File & Load Backup File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            exportFileLauncher.launch(BackupManager.generateDefaultBackupFileName())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldShield,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Export Backup", "خروجی پشتیبان (Export)"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_backup_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Import Backup", "ورود پشتیبان (Import)"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Row 2: Copy JSON Backup / Paste & Restore JSON Backup / Share File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val json = onExportBackupJson()
                                val copied = BackupManager.copyBackupToClipboard(context, json)
                                backupStatusBanner = if (copied) {
                                    true to tr(
                                        "Backup JSON copied to Clipboard!",
                                        "متن پشتیبان JSON در کلیپ‌بورد کپی شد!"
                                    )
                                } else {
                                    false to tr("Failed to copy to clipboard.", "خطا در کپی به کلیپ‌بورد.")
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanTelemetry),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_backup_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Copy JSON", "کپی پشتیبان"),
                            color = CyanTelemetry,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { showPasteBackupDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberWarning),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paste_restore_backup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("Paste Restore", "بازیابی با متن"),
                            color = AmberWarning,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val json = onExportBackupJson()
                                BackupManager.shareBackupFile(context, json)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.testTag("share_backup_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Backup File",
                            tint = ObsidianTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                backupStatusBanner?.let { (isSuccess, msg) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) EmeraldShield.copy(alpha = 0.16f) else CrimsonPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (isSuccess) EmeraldShield else CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_status_banner")
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) EmeraldShield else CrimsonPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }

    if (showPasteBackupDialog) {
        var pastedJson by remember { mutableStateOf(BackupManager.readTextFromClipboard(context) ?: "") }
        AlertDialog(
            onDismissRequest = { showPasteBackupDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = tr("Restore Backup from JSON", "بازیابی پشتیبان با متن JSON"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            "Paste your exported X Blocker backup JSON below to restore all rules, limits, and clean streak:",
                            "متن JSON پشتیبان قبلی خود را در کادر زیر قرار دهید تا تمام اطلاعات بازیابی شوند:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                    OutlinedTextField(
                        value = pastedJson,
                        onValueChange = { pastedJson = it },
                        placeholder = { Text("{\"backupFormatVersion\": 1, ...}") },
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("paste_backup_json_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanTelemetry,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = ObsidianTextPrimary,
                            unfocusedTextColor = ObsidianTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val summary = onImportBackupJson(pastedJson)
                            backupStatusBanner = summary.success to tr(summary.messageEn, summary.messageFa)
                            showPasteBackupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldShield),
                    modifier = Modifier.testTag("confirm_paste_restore_button")
                ) {
                    Text(tr("Restore Now", "بازیابی اطلاعات"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteBackupDialog = false }) {
                    Text(tr("Cancel", "انصراف"), color = ObsidianTextSecondary)
                }
            }
        )
    }
}
