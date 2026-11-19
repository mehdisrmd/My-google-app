package com.example.data.blocklog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * On-disk home of block-log screenshots (#14): `filesDir/blocklog/`.
 *
 * Privacy contract: this folder is APP-PRIVATE internal storage. Files
 * are written directly with [FileOutputStream] — never through
 * MediaStore — so screenshots of blocked pages can never appear in the
 * phone's gallery or any other app. The folder is also excluded from
 * Auto Backup / device transfer in `res/xml/backup_rules.xml` and
 * `res/xml/data_extraction_rules.xml`, so the images never leave the
 * device through backups either.
 *
 * Only bare file NAMES (see [BlockLogPolicy.screenshotFileName]) are kept
 * in the database; every path is resolved against this folder here.
 */
class ScreenshotStore(context: Context) {

    private val directory = File(context.filesDir, DIRECTORY_NAME)

    /** The private file for [fileName], or null for a blank name. */
    fun fileFor(fileName: String?): File? {
        val name = fileName?.takeIf { it.isNotBlank() } ?: return null
        return File(directory, name)
    }

    /** True when a screenshot file exists for [fileName]. */
    fun exists(fileName: String?): Boolean {
        return try {
            fileFor(fileName)?.isFile == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Stores [bitmap] as a JPEG under [fileName], downscaling so the
     * longest side is at most [BlockLogPolicy.MAX_SCREENSHOT_DIMENSION_PX].
     * Hardware bitmaps (what AccessibilityService.takeScreenshot hands
     * back) are first copied into a software bitmap, because they cannot
     * be read or compressed on the CPU. The caller's bitmap is NOT
     * recycled — ownership stays with the caller.
     *
     * Returns true only when the file was fully written.
     */
    fun saveJpeg(bitmap: Bitmap, fileName: String): Boolean {
        return try {
            if (bitmap.width <= 0 || bitmap.height <= 0) return false
            directory.mkdirs()

            var working = bitmap
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                working = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
            }
            val (targetWidth, targetHeight) = BlockLogPolicy.scaledDimensions(
                working.width,
                working.height
            )
            val scaled = if (targetWidth != working.width || targetHeight != working.height) {
                Bitmap.createScaledBitmap(working, targetWidth, targetHeight, true)
            } else {
                working
            }
            try {
                FileOutputStream(File(directory, fileName)).use { out ->
                    scaled.compress(
                        Bitmap.CompressFormat.JPEG,
                        BlockLogPolicy.SCREENSHOT_JPEG_QUALITY,
                        out
                    )
                }
            } finally {
                if (scaled !== working) scaled.recycle()
                if (working !== bitmap) working.recycle()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Decodes the stored screenshot for display, sampled so the longest
     * side stays near [maxDimensionPx] (decode-time power-of-two sampling
     * — see [BlockLogPolicy.sampleSizeFor]). Returns null when the file
     * is missing or undecodable. Call off the main thread.
     */
    fun loadBitmap(fileName: String?, maxDimensionPx: Int): Bitmap? {
        val file = fileFor(fileName) ?: return null
        return try {
            if (!file.isFile) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = BlockLogPolicy.sampleSizeFor(
                    bounds.outWidth,
                    bounds.outHeight,
                    maxDimensionPx
                )
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: Exception) {
            null
        }
    }

    /** Deletes the screenshot for [fileName]; true when a file was removed. */
    fun delete(fileName: String?): Boolean {
        return try {
            val file = fileFor(fileName) ?: return false
            !file.exists() || file.delete()
        } catch (_: Exception) {
            false
        }
    }

    /** Deletes every stored screenshot (used when the whole log is cleared). */
    fun deleteAll() {
        try {
            directory.listFiles()?.forEach { runCatching { it.delete() } }
        } catch (_: Exception) {
            // Nothing recoverable here; orphaned files are also pruned
            // opportunistically by the incident cap.
        }
    }

    companion object {
        const val DIRECTORY_NAME = "blocklog"
    }
}
