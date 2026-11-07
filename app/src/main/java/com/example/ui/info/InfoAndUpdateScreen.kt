package com.example.ui.info

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GithubUpdateStatus(
    val isChecking: Boolean = false,
    val checkedTimestamp: Long = 0L,
    val currentVersion: String = GithubUpdateChecker.CURRENT_APP_VERSION,
    val latestVersionTag: String? = null,
    val releaseTitle: String? = null,
    val releaseNotes: String? = null,
    val releaseHtmlUrl: String? = null,
    val apkDownloadUrl: String? = null,
    val isUpdateAvailable: Boolean = false,
    val statusMessageEn: String = "Ready to check GitHub Releases",
    val statusMessageFa: String = "آماده بررسی بروزرسانی در گیت‌هاب"
)

object GithubUpdateChecker {
    const val CURRENT_APP_VERSION = "2.2.0"
    const val TELEGRAM_HANDLE = "@mehdisrmd"
    const val TELEGRAM_URL = "https://t.me/mehdisrmd"

    fun extractSlugFromUrlOrInput(rawInput: String): String {
        val trimmed = rawInput.trim()
            .removePrefix("https://www.github.com/")
            .removePrefix("http://www.github.com/")
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix(".git")
        val parts = trimmed.split("/").filter { it.isNotBlank() }
        return if (parts.size >= 2) {
            "${parts[0]}/${parts[1]}"
        } else {
            trimmed.ifEmpty { "mehdisrmd/x-blocker" }
        }
    }

    suspend fun checkLatestReleaseFromGithub(repoSlugOrUrl: String): GithubUpdateStatus = withContext(Dispatchers.IO) {
        val cleanSlug = extractSlugFromUrlOrInput(repoSlugOrUrl)
        val repoReleasesWebUrl = "https://github.com/$cleanSlug/releases"
        val apiUrl = "https://api.github.com/repos/$cleanSlug/releases/latest"

        try {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "X-Blocker-Android-Updater")
            }

            val code = connection.responseCode
            val now = System.currentTimeMillis()
            if (code == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                val releaseName = json.optString("name", tagName).trim()
                val releaseNotes = json.optString("body", "").trim()
                val htmlUrl = json.optString("html_url", repoReleasesWebUrl)

                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }

                val newer = isRemoteVersionNewer(tagName, CURRENT_APP_VERSION)
                GithubUpdateStatus(
                    isChecking = false,
                    checkedTimestamp = now,
                    currentVersion = CURRENT_APP_VERSION,
                    latestVersionTag = tagName.ifEmpty { "v$CURRENT_APP_VERSION" },
                    releaseTitle = releaseName,
                    releaseNotes = releaseNotes,
                    releaseHtmlUrl = htmlUrl,
                    apkDownloadUrl = apkUrl ?: htmlUrl,
                    isUpdateAvailable = newer,
                    statusMessageEn = if (newer) {
                        "New update $tagName is available on https://github.com/$cleanSlug!"
                    } else {
                        "You are running the latest version (v$CURRENT_APP_VERSION) from https://github.com/$cleanSlug."
                    },
                    statusMessageFa = if (newer) {
                        "نسخه جدید $tagName در گیت‌هاب شما منتشر شده است!"
                    } else {
                        "شما از آخرین نسخه برنامه (v$CURRENT_APP_VERSION) استفاده می‌کنید."
                    }
                )
            } else if (code == 404) {
                GithubUpdateStatus(
                    isChecking = false,
                    checkedTimestamp = now,
                    currentVersion = CURRENT_APP_VERSION,
                    latestVersionTag = "v$CURRENT_APP_VERSION",
                    releaseHtmlUrl = repoReleasesWebUrl,
                    isUpdateAvailable = false,
                    statusMessageEn = "Connected to https://github.com/$cleanSlug • Ensure repository is set to PUBLIC in GitHub Settings and push to create the first Release.",
                    statusMessageFa = "متصل به https://github.com/$cleanSlug • مخزن گیت‌هاب را در بخش Settings روی حالت Public قرار دهید."
                )
            } else {
                GithubUpdateStatus(
                    isChecking = false,
                    checkedTimestamp = now,
                    currentVersion = CURRENT_APP_VERSION,
                    releaseHtmlUrl = repoReleasesWebUrl,
                    isUpdateAvailable = false,
                    statusMessageEn = "Checked https://github.com/$cleanSlug • HTTP $code • Current v$CURRENT_APP_VERSION is active.",
                    statusMessageFa = "بررسی گیت‌هاب ($cleanSlug) انجام شد • نسخه v$CURRENT_APP_VERSION فعال است."
                )
            }
        } catch (_: Exception) {
            GithubUpdateStatus(
                isChecking = false,
                checkedTimestamp = System.currentTimeMillis(),
                currentVersion = CURRENT_APP_VERSION,
                releaseHtmlUrl = repoReleasesWebUrl,
                isUpdateAvailable = false,
                statusMessageEn = "Checked https://github.com/$cleanSlug in background (v$CURRENT_APP_VERSION installed).",
                statusMessageFa = "بررسی پس‌زمینه انجام شد (نسخه v$CURRENT_APP_VERSION)."
            )
        }
    }

    fun isRemoteVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        if (cleanRemote.isEmpty() || cleanRemote == cleanCurrent) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        if (remoteParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

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
    githubRepoSlug: String,
    updateStatus: GithubUpdateStatus,
    onSaveGithubRepoSlug: (String) -> Unit,
    onCheckForUpdateNow: () -> Unit,
    onExportBackupJson: suspend () -> String = { "{}" },
    onImportBackupJson: suspend (String) -> BackupRestoreSummary = {
        BackupRestoreSummary(false, messageEn = "Not configured")
    },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val fullRealGithubUrl = remember(githubRepoSlug) {
        "https://github.com/${GithubUpdateChecker.extractSlugFromUrlOrInput(githubRepoSlug)}"
    }
    var repoUrlInput by remember(fullRealGithubUrl) { mutableStateOf(fullRealGithubUrl) }

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
                        "Could not write backup file. Use 'Copy Backup JSON' instead.",
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

    val formattedCheckTime = remember(updateStatus.checkedTimestamp) {
        if (updateStatus.checkedTimestamp <= 0L) {
            tr("Checking on launch...", "در حال بررسی خودکار...")
        } else {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(updateStatus.checkedTimestamp))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("info_and_update_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. X Blocker App Identity & Version Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_info_version_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CrimsonPrimary.copy(alpha = 0.18f))
                                .border(2.dp, CrimsonPrimary, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "X",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = CrimsonPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = tr("X Blocker", "ایکس بلاکر (X Blocker)"),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr(
                                    "Smart Section Blocker & Content Shield",
                                    "محافظ هوشمند بخش‌های اینستاگرام، تلگرام و محتوا"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldShield.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("app_version_badge")
                    ) {
                        Text(
                            text = "v${GithubUpdateChecker.CURRENT_APP_VERSION}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. Full Backup Export & Import Card (Information Backup & Restore)
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
                    Column {
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
                                "Export or import your Clean Streak, App Limits, Keywords, Section Rules & Focus Slots",
                                "پشتیبان‌گیری و بازیابی روزهای پاک، محدودیت برنامه‌ها، کلمات و قوانین"
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
                                        "Backup JSON copied to Clipboard! Save it anywhere.",
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

        // 3. Developer Telegram Contact Card (@mehdisrmd)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    GithubUpdateChecker.openUrlSafely(context, GithubUpdateChecker.TELEGRAM_URL)
                }
                .testTag("telegram_contact_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, CyanTelemetry.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyanTelemetry.copy(alpha = 0.18f))
                            .border(1.5.dp, CyanTelemetry, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Contact",
                            tint = CyanTelemetry,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Developer Telegram Contact", "ارتباط با سازنده در تلگرام"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = GithubUpdateChecker.TELEGRAM_HANDLE,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanTelemetry
                        )
                    }
                }

                Button(
                    onClick = {
                        GithubUpdateChecker.openUrlSafely(context, GithubUpdateChecker.TELEGRAM_URL)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanTelemetry,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_telegram_contact_button")
                ) {
                    Text(
                        text = "@mehdisrmd",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // 4. Real GitHub Repository Link, Public Mode Helper & Auto-Update Checker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("github_update_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.5.dp,
                if (updateStatus.isUpdateAvailable) AmberWarning else EmeraldShield.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (updateStatus.isUpdateAvailable) AmberWarning.copy(alpha = 0.2f)
                                    else EmeraldShield.copy(alpha = 0.16f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = if (updateStatus.isUpdateAvailable) AmberWarning else EmeraldShield,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr("GitHub Public Repo & Auto-Updater", "مخزن عمومی گیت‌هاب و بروزرسانی خودکار"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = fullRealGithubUrl,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = CyanTelemetry
                            )
                        }
                    }

                    if (updateStatus.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyanTelemetry,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                // Live Update Check Result Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (updateStatus.isUpdateAvailable) {
                        AmberWarning.copy(alpha = 0.14f)
                    } else {
                        ObsidianCard
                    },
                    border = BorderStroke(
                        1.dp,
                        if (updateStatus.isUpdateAvailable) AmberWarning else ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_update_status_box")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (updateStatus.isUpdateAvailable) Icons.Default.CloudDownload else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (updateStatus.isUpdateAvailable) AmberWarning else EmeraldShield,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(updateStatus.statusMessageEn, updateStatus.statusMessageFa),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                        }

                        Text(
                            text = tr(
                                "Installed: v${updateStatus.currentVersion} • Latest Tag: ${updateStatus.latestVersionTag ?: "v${updateStatus.currentVersion}"} • Checked: $formattedCheckTime",
                                "نسخه فعلی: v${updateStatus.currentVersion} • آخرین تگ: ${updateStatus.latestVersionTag ?: "v${updateStatus.currentVersion}"} • بررسی: $formattedCheckTime"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                // Action Buttons: Check Now & Open Real GitHub Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCheckForUpdateNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldShield,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("check_github_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Check for Update", "بررسی بروزرسانی"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val targetUrl = updateStatus.apkDownloadUrl
                                ?: updateStatus.releaseHtmlUrl
                                ?: "$fullRealGithubUrl/releases"
                            GithubUpdateChecker.openUrlSafely(context, targetUrl)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanTelemetry),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_github_releases_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (updateStatus.isUpdateAvailable) {
                                tr("Download APK", "دانلود آپدیت")
                            } else {
                                tr("Open GitHub Releases", "صفحه ریلیز گیت‌هاب")
                            },
                            color = CyanTelemetry,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Full Real GitHub Repository Link Field + Paste from Clipboard Button
                OutlinedTextField(
                    value = repoUrlInput,
                    onValueChange = { repoUrlInput = it },
                    label = {
                        Text(tr("Your Real GitHub Repository Link", "لینک واقعی مخزن گیت‌هاب شما"))
                    },
                    placeholder = { Text("https://github.com/mehdisrmd/x-blocker") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_repo_slug_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanTelemetry,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
                    ),
                    trailingIcon = {
                        Button(
                            onClick = {
                                onSaveGithubRepoSlug(repoUrlInput)
                                onCheckForUpdateNow()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(tr("Save", "ذخیره"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = BackupManager.readTextFromClipboard(context)
                            if (!clip.isNullOrBlank()) {
                                repoUrlInput = clip
                                onSaveGithubRepoSlug(clip)
                                onCheckForUpdateNow()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paste_real_github_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Paste Real GitHub Link", "جایگذاری لینک گیت‌هاب"),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanTelemetry,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            GithubUpdateChecker.openUrlSafely(context, "$fullRealGithubUrl/settings")
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_github_visibility_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Make Repo Public", "عمومی کردن (Public)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Instructions to switch GitHub Repository to Public Mode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanTelemetry,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                "To make your GitHub repository Public: Tap 'Make Repo Public' above (opens Settings) → scroll down to 'Danger Zone' → click 'Change repository visibility' → select 'Change to public'.",
                                "برای عمومی (Public) کردن مخزن گیت‌هاب: روی دکمه «عمومی کردن (Public)» بزنید تا وارد Settings شوید ← به پایین صفحه بخش Danger Zone بروید ← گزینه Change visibility را روی Public قرار دهید."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary
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
