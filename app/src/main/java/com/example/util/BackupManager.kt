package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper utility for exporting and importing X Blocker full JSON backups
 * via Storage Access Framework (SAF), FileProvider share sheet, or Clipboard.
 */
object BackupManager {

    fun generateDefaultBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "x_blocker_backup_$stamp.json"
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun copyBackupToClipboard(context: Context, jsonContent: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            cm.setPrimaryClip(ClipData.newPlainText("X Blocker Backup JSON", jsonContent))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readTextFromClipboard(context: Context): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return null
            val item = cm.primaryClip?.getItemAt(0) ?: return null
            item.coerceToText(context)?.toString()?.trim()
        } catch (_: Exception) {
            null
        }
    }

    fun shareBackupFile(context: Context, jsonContent: String): Boolean {
        return try {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(dir, generateDefaultBackupFileName())
            file.writeText(jsonContent, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "X Blocker Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Export X Blocker Backup").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
