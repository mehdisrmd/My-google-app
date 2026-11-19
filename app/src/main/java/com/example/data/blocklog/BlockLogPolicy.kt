package com.example.data.blocklog

import com.example.data.model.BlockIncident

/**
 * Pure decision logic for the block-event log (#14): when a screenshot
 * capture may be attempted, how screenshot files are named and sized, how
 * the stored history is capped, and how a lockdown's trigger text maps to
 * the short "reason" label shown in the log.
 *
 * Everything here is side-effect free so it can be unit-tested without a
 * device; the Android-facing pieces live in [ScreenshotStore] and in
 * ShieldAccessibilityService.
 */
object BlockLogPolicy {

    /**
     * Hard cap on stored block records. The history is a review log, not
     * an archive: once it grows past this many rows the oldest records
     * (and their screenshot files) are pruned after each new insert.
     */
    const val MAX_INCIDENTS = 200

    /**
     * Minimum gap between two screenshot captures. The system itself
     * rate-limits AccessibilityService.takeScreenshot and answers with
     * ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT when called too often;
     * this local floor keeps rapid back-to-back block episodes (grace
     * re-blocks, section re-entries) from burning that budget — later
     * episodes in a burst are simply logged without an image.
     */
    const val MIN_CAPTURE_INTERVAL_MS = 5_000L

    /**
     * Longest side, in pixels, of a stored screenshot. Full phone frames
     * (1080×2400) are downscaled to this bound before JPEG compression so
     * a full 200-record log stays in the tens-of-MB range, not hundreds.
     */
    const val MAX_SCREENSHOT_DIMENSION_PX = 1080

    /** JPEG quality used when storing screenshots. */
    const val SCREENSHOT_JPEG_QUALITY = 75

    /**
     * True when a new capture may start: none is already in flight and at
     * least [MIN_CAPTURE_INTERVAL_MS] passed since the last attempt. A
     * [lastCaptureAttemptMs] of 0 (never attempted) always allows one.
     */
    fun shouldAttemptCapture(
        nowMs: Long,
        lastCaptureAttemptMs: Long,
        captureInFlight: Boolean
    ): Boolean {
        if (captureInFlight) return false
        if (lastCaptureAttemptMs <= 0L) return true
        return nowMs - lastCaptureAttemptMs >= MIN_CAPTURE_INTERVAL_MS
    }

    /**
     * Screenshot file NAME (stored in BlockIncident.screenshotPath and
     * resolved against the private blocklog folder by [ScreenshotStore]).
     * The timestamp keeps names chronologically sortable; the per-process
     * sequence makes same-millisecond captures collision-free.
     */
    fun screenshotFileName(timestampMs: Long, sequence: Long): String =
        "block_${timestampMs}_${sequence}.jpg"

    /**
     * Target pixel size for storing a [srcWidth]×[srcHeight] frame: the
     * longest side is capped at [maxDimensionPx] and the aspect ratio is
     * preserved. Sources already within the cap are returned unchanged
     * (never upscaled); degenerate inputs (≤ 0) pass through untouched so
     * the caller can reject them.
     */
    fun scaledDimensions(
        srcWidth: Int,
        srcHeight: Int,
        maxDimensionPx: Int = MAX_SCREENSHOT_DIMENSION_PX
    ): Pair<Int, Int> {
        if (srcWidth <= 0 || srcHeight <= 0 || maxDimensionPx <= 0) {
            return srcWidth to srcHeight
        }
        val longest = maxOf(srcWidth, srcHeight)
        if (longest <= maxDimensionPx) return srcWidth to srcHeight
        val scale = maxDimensionPx.toDouble() / longest.toDouble()
        val targetWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val targetHeight = (srcHeight * scale).toInt().coerceAtLeast(1)
        return targetWidth to targetHeight
    }

    /**
     * BitmapFactory-style sample size (a power of two): the smallest
     * power of two that brings the longest side of a [srcWidth]×[srcHeight]
     * image to at most [maxDimensionPx] when decoded. Returns 1 when the
     * source already fits or is degenerate.
     */
    fun sampleSizeFor(
        srcWidth: Int,
        srcHeight: Int,
        maxDimensionPx: Int
    ): Int {
        if (srcWidth <= 0 || srcHeight <= 0 || maxDimensionPx <= 0) return 1
        var sampleSize = 1
        var longest = maxOf(srcWidth, srcHeight)
        while (longest > maxDimensionPx && sampleSize <= (1 shl 15)) {
            longest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * The records to prune so the log holds at most [maxCount] rows.
     * [incidentsNewestFirst] MUST be ordered newest-first (the DAO's
     * `ORDER BY timestamp DESC` order); the victims are the oldest tail
     * beyond the cap. Returns an empty list when already within the cap.
     */
    fun pruneVictims(
        incidentsNewestFirst: List<BlockIncident>,
        maxCount: Int = MAX_INCIDENTS
    ): List<BlockIncident> {
        val cap = maxCount.coerceAtLeast(0)
        if (incidentsNewestFirst.size <= cap) return emptyList()
        return incidentsNewestFirst.drop(cap)
    }

    /**
     * Short reason label for a lockdown trigger text. The service passes
     * an explicit reason where the call site knows it precisely; this
     * classifier covers the trigger-word conventions the rest of the
     * service already uses, so every recorded block still gets a readable
     * reason in the log.
     */
    fun reasonFor(triggerWord: String): String = when {
        triggerWord.startsWith("Adult Site Blocked:") -> "Adult site"
        // #45 (batch 2): force-stop attempts get their own reason instead
        // of the generic tamper label, so the log shows what was tried.
        triggerWord == "Prevent Force Stop Guard" -> "Force stop attempt blocked"
        // #46 (batch 3): a data-cap warning releases per transport when
        // the transport is observed off; the reason records who won the
        // race — the app's background shutoff or the user by hand.
        triggerWord == "Data Cap: Mobile Off By App" -> "Mobile data turned off by the app — daily cap reached"
        triggerWord == "Data Cap: Mobile Off Manual" -> "Mobile data turned off manually — daily cap reached"
        triggerWord == "Data Cap: Wi-Fi Off By App" -> "Wi-Fi turned off by the app — daily cap reached"
        triggerWord == "Data Cap: Wi-Fi Off Manual" -> "Wi-Fi turned off manually — daily cap reached"
        triggerWord.startsWith("Daily Limit") -> "Daily app limit reached"
        triggerWord.startsWith("Section Blocked:") -> "Blocked app section"
        triggerWord.startsWith("Smart Focus:") -> "Scheduled focus session"
        triggerWord.startsWith("Prevent ") && triggerWord.endsWith("Guard") ->
            "Tamper attempt"
        triggerWord.startsWith("Emergency Panic") -> "Manual panic lock"
        else -> "Blocked keyword"
    }
}
