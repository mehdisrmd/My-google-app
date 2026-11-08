package com.example.service

import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.BlockedCategory
import com.example.data.model.BlockedKeyword
import com.example.data.model.SectionBlockRule
import com.example.util.AppPackageResolver

data class ScanMatchResult(
    val matchedKeyword: String,
    val category: BlockedCategory = BlockedCategory.ADULT_EXPLICIT
)

object TextContentScanner {

    fun extractScreenText(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val results = mutableListOf<String>()
        traverseNode(root, results, depth = 0)
        return results
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        results: MutableList<String>,
        depth: Int
    ) {
        if (node == null || depth > 30) return

        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
            results.add(it)
        }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
            results.add(it)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                results.add(it)
            }
        }
        node.viewIdResourceName?.trim()?.takeIf { it.isNotEmpty() }?.let {
            results.add(it)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                traverseNode(child, results, depth + 1)
            }
        }
    }

    /**
     * Detects if the user is attempting to uninstall or force-stop X Blocker in Android Settings
     * or Package Installer while Prevent Uninstall is enabled.
     */
    fun isAttemptingToUninstallOrDisableXBlocker(
        root: AccessibilityNodeInfo?,
        currentPackage: String
    ): Boolean {
        if (root == null) return false
        val pkgLower = currentPackage.lowercase()
        val isInstallerOrSettings = pkgLower.contains("packageinstaller") ||
            pkgLower == "com.android.settings" ||
            pkgLower.contains("miui.securitycenter") ||
            pkgLower.contains("permissioncontroller")

        if (!isInstallerOrSettings) return false

        val texts = extractScreenText(root)
        val combinedLower = texts.joinToString(" | ").lowercase()
        val mentionsOurApp = combinedLower.contains("x blocker") ||
            combinedLower.contains("com.aistudio.shieldblock") ||
            combinedLower.contains("shieldaccessibilityservice")

        if (!mentionsOurApp) return false

        if (pkgLower.contains("packageinstaller")) {
            return true
        }

        val hasDangerAction = combinedLower.contains("do you want to uninstall") ||
            combinedLower.contains("uninstall this app") ||
            combinedLower.contains("آیا می‌خواهید این برنامه را حذف کنید") ||
            combinedLower.contains("حذف نصب شود")
        return hasDangerAction
    }

    /**
     * Checks if the current screen in `currentPackage` matches any enabled `SectionBlockRule`.
     */
    fun findMatchingSectionRule(
        root: AccessibilityNodeInfo?,
        currentPackage: String,
        rules: List<SectionBlockRule>
    ): SectionBlockRule? {
        if (root == null || rules.isEmpty()) return null

        val applicableRules = rules.filter { rule ->
            rule.isEnabled && AppPackageResolver.matchesPackageOrVariant(
                ruleTargetPackage = rule.targetPackage,
                ruleSectionOrAppName = rule.sectionName,
                candidatePackage = currentPackage
            )
        }
        if (applicableRules.isEmpty()) return null

        val allScreenTexts = extractScreenText(root)
        val combinedScreenLower = allScreenTexts.joinToString(" | ").lowercase()

        for (rule in applicableRules) {
            val textTokens = rule.triggerPattern.split("|")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
            val viewIdTokens = rule.viewIdPattern.split("|")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

            if (findMatchingSectionNode(
                    node = root,
                    rule = rule,
                    textTokens = textTokens,
                    viewIdTokens = viewIdTokens,
                    screenLower = combinedScreenLower,
                    depth = 0
                )
            ) {
                return rule
            }
        }
        return null
    }

    /**
     * Text-based section match helper (also verifies that Telegram Archived Chats are never blocked).
     */
    fun findFirstSectionMatch(
        rawScreenText: String,
        currentPackage: String,
        rules: List<SectionBlockRule>
    ): ScanMatchResult? {
        if (rawScreenText.isBlank() || rules.isEmpty()) return null
        val normalizedScreen = normalizeText(rawScreenText)

        // Never trigger on Telegram Archived Chats
        if (currentPackage.contains("telegram", ignoreCase = true) &&
            (normalizedScreen.contains("archived chats") ||
                normalizedScreen.contains("archive") ||
                normalizedScreen.contains("ارشیو") ||
                normalizedScreen.contains("آرشیو"))
        ) {
            return null
        }

        val applicableRules = rules.filter { rule ->
            rule.isEnabled && AppPackageResolver.matchesPackageOrVariant(
                ruleTargetPackage = rule.targetPackage,
                ruleSectionOrAppName = rule.sectionName,
                candidatePackage = currentPackage
            )
        }
        for (rule in applicableRules) {
            val tokens = rule.triggerPattern.split("|")
                .map { normalizeText(it) }
                .filter { it.isNotEmpty() }
            if (tokens.any { token -> normalizedScreen.contains(token) }) {
                return ScanMatchResult(
                    matchedKeyword = "${rule.sectionName} (${rule.targetPackage})",
                    category = BlockedCategory.CUSTOM_KEYWORD
                )
            }
        }
        return null
    }

    private fun findMatchingSectionNode(
        node: AccessibilityNodeInfo?,
        rule: SectionBlockRule,
        textTokens: List<String>,
        viewIdTokens: List<String>,
        screenLower: String,
        depth: Int
    ): Boolean {
        if (node == null || depth > 30) return false

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.trim()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
        val hint = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString()?.trim()?.lowercase() ?: ""
        } else {
            ""
        }
        val className = node.className?.toString()?.lowercase() ?: ""

        val secNameLower = rule.sectionName.lowercase()
        val isSearchRule = secNameLower.contains("search") ||
            rule.triggerPattern.lowercase().contains("search") ||
            secNameLower.contains("جستجو")
        val isReelsRule = secNameLower.contains("reel") ||
            rule.targetPackage.lowercase().contains("instagram")

        if (isSearchRule) {
            // CRITICAL FIX (Item 3): Never block when the user opens Telegram Archived Chats!
            val isInsideArchivedChats = screenLower.contains("archived chats") ||
                screenLower.contains("archived") ||
                screenLower.contains("archive") ||
                screenLower.contains("آرشیو") ||
                screenLower.contains("ارشیو") ||
                screenLower.contains("چت‌های آرشیو") ||
                screenLower.contains("گفتگوهای آرشیو")

            if (isInsideArchivedChats && !node.isFocused) {
                return false
            }

            // Only trigger when an EditText search field is actively focused by the user
            // or when Global Search results are displayed alongside an active EditText.
            val isEditText = className.contains("edittext")
            val hasSearchLabelOrId = textTokens.any { token ->
                text.contains(token) || desc.contains(token) || hint.contains(token)
            } || viewIdTokens.any { token -> viewId.contains(token) } ||
                viewId.contains("search_src_text") ||
                viewId.contains("search_field")

            val hasGlobalSearchSectionHeader =
                screenLower.contains("global search") || screenLower.contains("جستجوی جهانی")

            if (isEditText && node.isFocused && (hasSearchLabelOrId || hasGlobalSearchSectionHeader || !isInsideArchivedChats)) {
                return true
            }
            if (isEditText && hasGlobalSearchSectionHeader && !isInsideArchivedChats) {
                return true
            }
        } else if (isReelsRule) {
            val isDedicatedReelsPlayerView = viewId.contains("clips_viewer") ||
                viewId.contains("reel_viewer") ||
                viewId.contains("clips_video_container") ||
                viewId.contains("WatchReels".lowercase())

            val isReelsTabActivelySelected = node.isSelected && (
                viewId.contains("clips_tab") ||
                    desc == "reels" ||
                    text == "reels" ||
                    desc.contains("ریلز")
                )

            if (isDedicatedReelsPlayerView || isReelsTabActivelySelected) {
                return true
            }
        } else {
            if (viewId.isNotEmpty() && viewIdTokens.any { token -> viewId.contains(token) }) {
                return true
            }
            if (textTokens.any { token ->
                    (text.isNotEmpty() && text == token) ||
                        (desc.isNotEmpty() && desc == token)
                }
            ) {
                return true
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                if (findMatchingSectionNode(child, rule, textTokens, viewIdTokens, screenLower, depth + 1)) {
                    return true
                }
            }
        }
        return false
    }

    fun normalizeText(raw: String): String {
        val withoutDiacritics = raw
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('\u200C', ' ')
            .lowercase()

        return buildString(withoutDiacritics.length) {
            for (ch in withoutDiacritics) {
                when (ch) {
                    '0' -> append('o')
                    '1' -> append('i')
                    '3' -> append('e')
                    '4' -> append('a')
                    '5' -> append('s')
                    '@' -> append('a')
                    '$' -> append('s')
                    else -> append(ch)
                }
            }
        }
    }

    fun findFirstMatch(
        rawText: String,
        keywords: List<BlockedKeyword>
    ): ScanMatchResult? {
        if (rawText.isBlank() || keywords.isEmpty()) return null
        val lowerRaw = rawText.lowercase()
        val normalized = normalizeText(rawText)

        for (keyword in keywords) {
            if (!keyword.isEnabled) continue
            val targetRaw = keyword.text.trim().lowercase()
            val targetNorm = normalizeText(targetRaw)
            if (targetNorm.isEmpty()) continue

            if (keyword.isRegex) {
                try {
                    if (Regex(targetRaw, RegexOption.IGNORE_CASE).containsMatchIn(lowerRaw)) {
                        return ScanMatchResult(keyword.text, keyword.category)
                    }
                } catch (_: Exception) {
                }
            } else {
                when (keyword.category) {
                    BlockedCategory.ADULT_DOMAIN,
                    BlockedCategory.CUSTOM_DOMAIN -> {
                        if (lowerRaw.contains(targetRaw) || normalized.contains(targetNorm)) {
                            return ScanMatchResult(keyword.text, keyword.category)
                        }
                    }

                    BlockedCategory.ADULT_EXPLICIT,
                    BlockedCategory.CUSTOM_KEYWORD -> {
                        val escaped = Regex.escape(targetNorm)
                        val boundaryRegex = Regex("(^|[^a-zA-Z0-9\\u0600-\\u06FF])$escaped([^a-zA-Z0-9\\u0600-\\u06FF]|$)")
                        if (boundaryRegex.containsMatchIn(normalized)) {
                            return ScanMatchResult(keyword.text, keyword.category)
                        }
                    }
                }
            }
        }
        return null
    }

    fun findMatchingKeyword(
        screenTexts: List<String>,
        keywords: List<BlockedKeyword>
    ): String? {
        if (screenTexts.isEmpty() || keywords.isEmpty()) return null
        val combined = screenTexts.joinToString(separator = " \n ")
        return findFirstMatch(combined, keywords)?.matchedKeyword
    }
}
