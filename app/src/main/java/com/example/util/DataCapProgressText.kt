package com.example.util

/**
 * #60 (batch 5): the text lines of the data-cap progress readout —
 * one line per capped transport, used verbatim by the standing
 * notification (LockdownNotificationManager) and available to the
 * data-limits card so both surfaces always agree.
 *
 * Pure formatting (no Android types): the caller supplies today's
 * usage bytes and the caps; a transport with no cap (0 MB) gets no
 * line, so an empty result means "nothing to report" and the
 * notification is cancelled instead of posted.
 */
object DataCapProgressText {

    fun lines(
        wifiUsedBytes: Long,
        wifiCapMb: Int,
        mobileUsedBytes: Long,
        mobileCapMb: Int,
        isPersian: Boolean
    ): List<String> {
        val lines = mutableListOf<String>()
        line(
            labelEn = "Wi-Fi",
            labelFa = "وای‌فای",
            usedBytes = wifiUsedBytes,
            capMb = wifiCapMb,
            isPersian = isPersian
        )?.let { lines.add(it) }
        line(
            labelEn = "Mobile data",
            labelFa = "اینترنت موبایل",
            usedBytes = mobileUsedBytes,
            capMb = mobileCapMb,
            isPersian = isPersian
        )?.let { lines.add(it) }
        return lines
    }

    private fun line(
        labelEn: String,
        labelFa: String,
        usedBytes: Long,
        capMb: Int,
        isPersian: Boolean
    ): String? {
        val capBytes = DataUsageBudget.mbToBytes(capMb)
        if (capBytes <= 0L) return null
        val used = DataUsageBudget.formatBytes(usedBytes)
        val cap = DataUsageBudget.formatBytes(capBytes)
        val left = DataUsageBudget.formatBytes(
            DataUsageBudget.remainingBytes(usedBytes, capBytes)
        )
        val percent = DataUsageBudget.usedPercent(usedBytes, capBytes)
        return if (isPersian) {
            "$labelFa: $used از $cap مصرف شده · $left مانده ($percent٪)"
        } else {
            "$labelEn: $used of $cap used · $left left ($percent%)"
        }
    }
}
