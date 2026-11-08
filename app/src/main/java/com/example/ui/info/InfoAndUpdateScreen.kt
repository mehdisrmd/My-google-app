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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    const val CURRENT_APP_VERSION = "0.4"
    const val TELEGRAM_HANDLE = "@mehdisrmd"
    const val TELEGRAM_URL = "https://t.me/mehdisrmd"
    const val DEFAULT_GITHUB_REPO_SLUG = "mehdisrmd/My-google-app"
    const val DEFAULT_GITHUB_REPO_URL = "https://github.com/mehdisrmd/My-google-app"

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
            trimmed.ifEmpty { DEFAULT_GITHUB_REPO_SLUG }
        }
    }

    private fun parseReleaseJson(
        json: JSONObject,
        cleanSlug: String,
        repoReleasesWebUrl: String,
        now: Long
    ): GithubUpdateStatus {
        val tagName = json.optString("tag_name", "").trim()
        val releaseName = json.optString("name", tagName).trim()
        val releaseNotes = json.optString("body", "").trim()
        val htmlUrl = json.optString("html_url", repoReleasesWebUrl)

        var apkUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            val deviceAbis = runCatching { android.os.Build.SUPPORTED_ABIS?.toList().orEmpty() }.getOrDefault(emptyList())
            val apkAssets = mutableListOf<Pair<String, String>>()
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    apkAssets.add(name to downloadUrl)
                }
            }
            val matchedByAbi = deviceAbis.firstNotNullOfOrNull { abi ->
                apkAssets.firstOrNull { (name, _) -> name.contains(abi, ignoreCase = true) }?.second
            }
            val universalApk = apkAssets.firstOrNull { (name, _) ->
                name.contains("universal", ignoreCase = true)
            }?.second
            apkUrl = matchedByAbi ?: universalApk ?: apkAssets.firstOrNull()?.second
        }

        val versionCandidate = tagName.ifEmpty { releaseName }
        val newer = isRemoteVersionNewer(versionCandidate, CURRENT_APP_VERSION)
        val displayTag = tagName.ifEmpty { "v$CURRENT_APP_VERSION" }

        return GithubUpdateStatus(
            isChecking = false,
            checkedTimestamp = now,
            currentVersion = CURRENT_APP_VERSION,
            latestVersionTag = displayTag,
            releaseTitle = releaseName.ifEmpty { "Release $displayTag" },
            releaseNotes = releaseNotes,
            releaseHtmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl ?: htmlUrl,
            isUpdateAvailable = newer,
            statusMessageEn = if (newer) {
                "New update $displayTag is available on GitHub!"
            } else {
                "You are running the latest version (v$CURRENT_APP_VERSION)."
            },
            statusMessageFa = if (newer) {
                "نسخه جدید $displayTag در گیت‌هاب آماده دانلود است!"
            } else {
                "شما از آخرین نسخه برنامه (v$CURRENT_APP_VERSION) استفاده می‌کنید."
            }
        )
    }

    suspend fun checkLatestReleaseFromGithub(
        repoSlugOrUrl: String = DEFAULT_GITHUB_REPO_SLUG
    ): GithubUpdateStatus = withContext(Dispatchers.IO) {
        val cleanSlug = extractSlugFromUrlOrInput(repoSlugOrUrl)
        val repoReleasesWebUrl = "https://github.com/$cleanSlug/releases"
        val latestApiUrl = "https://api.github.com/repos/$cleanSlug/releases/latest"
        val allReleasesApiUrl = "https://api.github.com/repos/$cleanSlug/releases"

        try {
            val connection = (URL(latestApiUrl).openConnection() as HttpURLConnection).apply {
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
                return@withContext parseReleaseJson(json, cleanSlug, repoReleasesWebUrl, now)
            }

            // Fallback: check /releases list in case the latest release is a pre-release
            val listConn = (URL(allReleasesApiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "X-Blocker-Android-Updater")
            }
            if (listConn.responseCode == 200) {
                val listBody = listConn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(listBody)
                if (arr.length() > 0) {
                    val firstRelease = arr.optJSONObject(0)
                    if (firstRelease != null) {
                        return@withContext parseReleaseJson(firstRelease, cleanSlug, repoReleasesWebUrl, now)
                    }
                }
            }

            GithubUpdateStatus(
                isChecking = false,
                checkedTimestamp = now,
                currentVersion = CURRENT_APP_VERSION,
                latestVersionTag = "v$CURRENT_APP_VERSION",
                releaseHtmlUrl = repoReleasesWebUrl,
                isUpdateAvailable = false,
                statusMessageEn = "You are on the latest version (v$CURRENT_APP_VERSION).",
                statusMessageFa = "شما از آخرین نسخه برنامه (v$CURRENT_APP_VERSION) استفاده می‌کنید."
            )
        } catch (_: Exception) {
            GithubUpdateStatus(
                isChecking = false,
                checkedTimestamp = System.currentTimeMillis(),
                currentVersion = CURRENT_APP_VERSION,
                releaseHtmlUrl = repoReleasesWebUrl,
                isUpdateAvailable = false,
                statusMessageEn = "Checked https://github.com/$cleanSlug (v$CURRENT_APP_VERSION installed).",
                statusMessageFa = "بررسی بروزرسانی انجام شد (نسخه v$CURRENT_APP_VERSION فعال است)."
            )
        }
    }

    fun isRemoteVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val numberRegex = Regex("""\d+(\.\d+)+""")
        val extractedRemote = numberRegex.find(remoteTag)?.value
            ?: remoteTag.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        if (extractedRemote.isEmpty() || extractedRemote == cleanCurrent) return false

        val remoteParts = extractedRemote.split(".").mapNotNull { it.toIntOrNull() }
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

/**
 * Material Design 3 Update Available Panel / Dialog.
 * Automatically shown when the user enters the app and a new release exists on
 * https://github.com/mehdisrmd/My-google-app.
 */
@Composable
fun GithubUpdateAvailablePanelDialog(
    updateStatus: GithubUpdateStatus,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val downloadTargetUrl = updateStatus.apkDownloadUrl
        ?: updateStatus.releaseHtmlUrl
        ?: "${GithubUpdateChecker.DEFAULT_GITHUB_REPO_URL}/releases"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        modifier = Modifier.testTag("github_update_available_panel_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                    contentDescription = "X Blocker Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, AmberWarning, RoundedCornerShape(14.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tr("NEW UPDATE AVAILABLE", "بروزرسانی جدید منتشر شد"),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Black,
                            color = AmberWarning
                        )
                    }
                    Text(
                        text = updateStatus.releaseTitle?.takeIf { it.isNotBlank() }
                            ?: tr("X Blocker ${updateStatus.latestVersionTag ?: ""}", "ایکس بلاکر ${updateStatus.latestVersionTag ?: ""}"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = ObsidianTextPrimary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarning.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tr("Installed", "نسخه فعلی"),
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianTextSecondary
                            )
                            Text(
                                text = "v${updateStatus.currentVersion}",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                        }
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = AmberWarning
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = tr("Latest Release", "نسخه جدید"),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning
                            )
                            Text(
                                text = updateStatus.latestVersionTag ?: "New",
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                color = EmeraldShield
                            )
                        }
                    }
                }

                if (!updateStatus.releaseNotes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tr("What's New:", "تغییرات نسخه جدید:"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanTelemetry
                            )
                            Text(
                                text = updateStatus.releaseNotes.take(400),
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = tr(
                            "A newer build of X Blocker is available on GitHub (mehdisrmd/My-google-app). Tap below to download and install the update.",
                            "نسخه جدیدتری از برنامه ایکس بلاکر در مخزن گیت‌هاب شما منتشر شده است. برای دریافت و نصب روی دکمه زیر بزنید."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    GithubUpdateChecker.openUrlSafely(context, downloadTargetUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldShield,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("update_panel_download_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("Download Update", "دانلود و نصب آپدیت"),
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("update_panel_later_button")
            ) {
                Text(
                    text = tr("Later", "بعداً"),
                    color = ObsidianTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
fun InfoAndUpdateScreen(
    githubRepoSlug: String,
    updateStatus: GithubUpdateStatus,
    onSaveGithubRepoSlug: (String) -> Unit,
    onCheckForUpdateNow: () -> Unit,
    onOpenUpdatePanel: () -> Unit = {},
    onExportBackupJson: suspend () -> String = { "{}" },
    onImportBackupJson: suspend (String) -> BackupRestoreSummary = {
        BackupRestoreSummary(false, messageEn = "Not configured")
    },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val fullRealGithubUrl = GithubUpdateChecker.DEFAULT_GITHUB_REPO_URL

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

    val formattedCheckTime = remember(updateStatus.checkedTimestamp) {
        if (updateStatus.checkedTimestamp <= 0L) {
            tr("Auto-checking...", "در حال بررسی خودکار...")
        } else {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(updateStatus.checkedTimestamp))
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
                                text = "v${GithubUpdateChecker.CURRENT_APP_VERSION}",
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
                                GithubUpdateChecker.openUrlSafely(context, GithubUpdateChecker.TELEGRAM_URL)
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
                                        text = GithubUpdateChecker.TELEGRAM_HANDLE,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Black,
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

        // 2. Streamlined GitHub Auto-Update Card (https://github.com/mehdisrmd/My-google-app)
        // Extra buttons & manual URL inputs under this section have been removed as requested.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("github_update_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(
                1.5.dp,
                if (updateStatus.isUpdateAvailable) AmberWarning else EmeraldShield.copy(alpha = 0.55f)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("App Update Checker", "بررسی خودکار بروزرسانی برنامه"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = CyanTelemetry,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
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
                    }

                    if (updateStatus.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = CyanTelemetry,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                // Live Update Check Status Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
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
                        .clickable(enabled = updateStatus.isUpdateAvailable) {
                            onOpenUpdatePanel()
                        }
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
                                "Current: v${updateStatus.currentVersion} • Latest: ${updateStatus.latestVersionTag ?: "v${updateStatus.currentVersion}"} • Checked: $formattedCheckTime",
                                "نسخه فعلی: v${updateStatus.currentVersion} • آخرین نسخه: ${updateStatus.latestVersionTag ?: "v${updateStatus.currentVersion}"} • زمان بررسی: $formattedCheckTime"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                // Single clean primary action button (or Download Update if an update exists)
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

                    if (updateStatus.isUpdateAvailable) {
                        Button(
                            onClick = onOpenUpdatePanel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberWarning,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_github_releases_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("Show Update Panel", "مشاهده پنل آپدیت"),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                GithubUpdateChecker.openUrlSafely(context, "$fullRealGithubUrl/releases")
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.7f)),
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
                                text = tr("GitHub Releases", "صفحه ریلیز گیت‌هاب"),
                                color = CyanTelemetry,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
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
                                "Export or import your Clean Streak, Best Chain, Coins, Limits & Rules",
                                "پشتیبان‌گیری و بازیابی روزهای پاک، بهترین زنجیره، سکه‌ها و قوانین"
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
