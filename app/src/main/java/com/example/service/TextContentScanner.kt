package com.example.service

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.BlockedCategory
import com.example.data.model.BlockedKeyword
import com.example.data.model.SectionBlockRule
import java.util.Locale

object TextContentScanner {

    data class MatchResult(
        val matchedKeyword: String,
        val category: BlockedCategory,
        val snippet: String
    )

    fun scanForSectionRule(
        root: AccessibilityNodeInfo?,
        rules: List<SectionBlockRule>,
        currentPackage: String
    ): SectionBlockRule? {
        if (root == null || rules.isEmpty()) return null
        val applicable = rules.filter {
            it.isEnabled && com.example.util.AppPackageResolver.matchesPackageOrVariant(
                ruleTargetPackage = it.targetPackage,
                ruleSectionOrAppName = it.sectionName,
                candidatePackage = currentPackage
            )
        }
        if (applicable.isEmpty()) return null

        // Collect a quick snapshot of all screen text to check if Telegram Search filter tabs are visible
        val screenSnapshotBuilder = StringBuilder()
        collectText(root, screenSnapshotBuilder, maxDepth = 10, currentDepth = 0)
        val screenLower = screenSnapshotBuilder.toString().lowercase(Locale.ROOT)

        return findMatchingSectionNode(
            node = root,
            rules = applicable,
            screenLower = screenLower,
            maxDepth = 14,
            currentDepth = 0
        )
    }

    private fun findMatchingSectionNode(
        node: AccessibilityNodeInfo?,
        rules: List<SectionBlockRule>,
        screenLower: String,
        maxDepth: Int,
        currentDepth: Int
    ): SectionBlockRule? {
        if (node == null || currentDepth > maxDepth) return null

        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""
        val text = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
        val desc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.lowercase(Locale.ROOT) ?: ""
        } else {
            ""
        }
        val className = node.className?.toString()?.lowercase(Locale.ROOT) ?: ""

        val isEditText = className.contains("edittext")
        val isFocused = node.isFocused || node.isAccessibilityFocused
        val isSelected = node.isSelected

        for (rule in rules) {
            val isSearchRule = rule.triggerPattern.contains("search", ignoreCase = true) ||
                rule.sectionName.contains("search", ignoreCase = true)
            val isInstagramReelsRule = rule.targetPackage.contains("instagram", ignoreCase = true) ||
                rule.sectionName.contains("reels", ignoreCase = true)

            if (isSearchRule) {
                // CAREFUL TELEGRAM SEARCH BOX DETECTION:
                // Must ONLY trigger when the user enters the Search box in Telegram,
                // NEVER when opening Telegram chat list or typing a normal chat message!
                val isChatMessageInput = text.contains("message") ||
                    text.contains("پیام") ||
                    hint.contains("message") ||
                    hint.contains("پیام") ||
                    desc.contains("message")

                val hasExplicitSearchViewId = viewId.isNotEmpty() && (
                    viewId.contains("search_src_text") ||
                        viewId.contains("chat_search") ||
                        viewId.contains("search_field") ||
                        viewId.contains("search_edit")
                    )

                val hasTelegramSearchFilterTabsOnScreen =
                    screenLower.contains("global search") ||
                        screenLower.contains("جستجوی جهانی") ||
                        screenLower.contains("search messages") ||
                        screenLower.contains("recent searches") ||
                        (screenLower.contains("downloads") && screenLower.contains("links") && screenLower.contains("media")) ||
                        (screenLower.contains("رسانه") && screenLower.contains("لینک"))

                val editTextHasSearchLabel = isEditText && !isChatMessageInput && (
                    text.contains("search") ||
                        text.contains("جستجو") ||
                        hint.contains("search") ||
                        hint.contains("جستجو") ||
                        desc.contains("search") ||
                        desc.contains("جستجو") ||
                        viewId.contains("search") ||
                        (isFocused && hasTelegramSearchFilterTabsOnScreen)
                    )

                if (hasExplicitSearchViewId || editTextHasSearchLabel || (isEditText && hasTelegramSearchFilterTabsOnScreen && !isChatMessageInput)) {
                    return rule
                }
                continue
            }

            if (isInstagramReelsRule) {
                // CAREFUL INSTAGRAM REELS DETECTION:
                // Must ONLY trigger when the user is actually inside the Reels section/tab,
                // NEVER when opening Instagram Home/DM where the unselected Reels bottom tab icon sits!
                if (viewId.isNotEmpty()) {
                    val isReelsActiveViewerId = viewId.contains("clips_viewer") ||
                        viewId.contains("reel_viewer") ||
                        viewId.contains("clips_video_container") ||
                        viewId.contains("clips_swipe_refresh")
                    val isReelsBottomTabSelected = viewId.contains("clips_tab") && isSelected
                    if (isReelsActiveViewerId || isReelsBottomTabSelected) {
                        return rule
                    }
                }
                // Or if the selected Reels tab / active Reels header is selected
                val matchesReelsLabel = text == "reels" || text == "ریلز" || desc == "reels" || desc == "ریلز"
                if (matchesReelsLabel && isSelected) {
                    return rule
                }
                continue
            }

            // 1. Generic Section Rule: Check View ID pattern
            if (rule.viewIdPattern.isNotBlank() && viewId.isNotEmpty()) {
                val viewMatches = matchesPatternSafely(viewId, rule.viewIdPattern)
                if (viewMatches) {
                    val isTabId = viewId.contains("tab")
                    if (!isTabId || isSelected) {
                        return rule
                    }
                }
            }

            // 2. Generic Section Rule: Check Trigger Pattern on node text / contentDescription
            if (rule.triggerPattern.isNotBlank()) {
                val textMatches = text.isNotEmpty() && matchesPatternSafely(text, rule.triggerPattern)
                val descMatches = desc.isNotEmpty() && matchesPatternSafely(desc, rule.triggerPattern)

                if (textMatches || descMatches) {
                    val isUnselectedTabIcon = descMatches && !textMatches && node.isClickable && !isSelected
                    if (!isUnselectedTabIcon) {
                        return rule
                    }
                }
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val match = findMatchingSectionNode(child, rules, screenLower, maxDepth, currentDepth + 1)
            if (match != null) return match
        }
        return null
    }

    /**
     * Evaluates section block rules against simulated screen text and package name (used in Test Sandbox & tests).
     */
    fun findFirstSectionMatch(
        rawScreenText: String,
        currentPackage: String,
        rules: List<SectionBlockRule>
    ): MatchResult? {
        if (rawScreenText.isBlank() || rules.isEmpty()) return null
        val lowerText = rawScreenText.lowercase(Locale.ROOT)
        for (rule in rules) {
            if (!rule.isEnabled) continue
            val pkgMatches = currentPackage == "*" ||
                rule.targetPackage.equals(currentPackage, ignoreCase = true) ||
                rule.targetPackage == "*" ||
                lowerText.contains(rule.targetPackage.lowercase(Locale.ROOT))
            if (!pkgMatches) continue

            if (rule.triggerPattern.isNotBlank() && matchesPatternSafely(lowerText, rule.triggerPattern)) {
                return MatchResult(
                    matchedKeyword = "Section Blocked: ${rule.sectionName}",
                    category = BlockedCategory.CUSTOM_KEYWORD,
                    snippet = rawScreenText.take(60)
                )
            }
            if (rule.viewIdPattern.isNotBlank() && matchesPatternSafely(lowerText, rule.viewIdPattern)) {
                return MatchResult(
                    matchedKeyword = "Section Blocked: ${rule.sectionName}",
                    category = BlockedCategory.CUSTOM_KEYWORD,
                    snippet = rawScreenText.take(60)
                )
            }
        }
        return null
    }

    private fun matchesPatternSafely(target: String, pattern: String): Boolean {
        return try {
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(target)
        } catch (_: Exception) {
            target.contains(pattern.lowercase(Locale.ROOT))
        }
    }

    fun scanNodeTree(
        root: AccessibilityNodeInfo?,
        activeKeywords: List<BlockedKeyword>,
        ignorePackage: String
    ): MatchResult? {
        if (root == null) return null
        if (root.packageName != null && root.packageName.toString() == ignorePackage) {
            return null
        }

        val textCollector = StringBuilder()
        collectText(root, textCollector, maxDepth = 15, currentDepth = 0)
        val fullText = textCollector.toString()
        if (fullText.isBlank()) return null

        return findFirstMatch(fullText, activeKeywords)
    }

    private fun collectText(
        node: AccessibilityNodeInfo?,
        collector: StringBuilder,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (node == null || currentDepth > maxDepth) return

        val text = node.text
        if (!text.isNullOrBlank()) {
            collector.append(text).append(" ")
        }
        val desc = node.contentDescription
        if (!desc.isNullOrBlank()) {
            collector.append(desc).append(" ")
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, collector, maxDepth, currentDepth + 1)
        }
    }

    fun findFirstMatch(rawText: String, keywords: List<BlockedKeyword>): MatchResult? {
        if (rawText.isBlank() || keywords.isEmpty()) return null

        val lowerRaw = rawText.lowercase(Locale.ROOT)
        val normalized = normalizeText(lowerRaw)

        // Prioritize specific domain matches and longer phrases first
        val sortedKeywords = keywords.sortedWith(
            compareByDescending<BlockedKeyword> {
                it.category == BlockedCategory.ADULT_DOMAIN || it.category == BlockedCategory.CUSTOM_DOMAIN
            }.thenByDescending { it.text.length }
        )

        for (item in sortedKeywords) {
            if (!item.isEnabled) continue
            val pattern = item.text.lowercase(Locale.ROOT).trim()
            if (pattern.isEmpty()) continue

            // 1. Direct domain or URL substring match
            if (item.category == BlockedCategory.ADULT_DOMAIN ||
                item.category == BlockedCategory.CUSTOM_DOMAIN ||
                pattern.contains(".")
            ) {
                val strippedDomain = pattern.replace(".", "")
                if (lowerRaw.contains(pattern) || (strippedDomain.length >= 5 && normalized.contains(strippedDomain))) {
                    return MatchResult(
                        matchedKeyword = item.text,
                        category = item.category,
                        snippet = extractSnippet(rawText, pattern)
                    )
                }
            }

            // 2. Keyword / Phrase match (supports both English and Persian with normalized text)
            val normalizedPattern = normalizeText(pattern)
            if (normalizedPattern.isEmpty()) continue

            if (lowerRaw.contains(pattern) || normalized.contains(normalizedPattern)) {
                if (normalizedPattern.length <= 3) {
                    val tokenBoundaryRegex = Regex("(?<=^|\\s)${Regex.escape(normalizedPattern)}(?=$|\\s)")
                    if (tokenBoundaryRegex.containsMatchIn(normalized)) {
                        return MatchResult(
                            matchedKeyword = item.text,
                            category = item.category,
                            snippet = extractSnippet(rawText, pattern)
                        )
                    }
                } else {
                    return MatchResult(
                        matchedKeyword = item.text,
                        category = item.category,
                        snippet = extractSnippet(rawText, pattern)
                    )
                }
            }
        }
        return null
    }

    fun normalizeText(input: String): String {
        val sb = StringBuilder(input.length)
        var lastWasSpace = true
        for (ch in input) {
            val mapped: Char? = when (ch) {
                '0' -> 'o'
                '@' -> 'a'
                '$' -> 's'
                '1', '!', '|' -> 'i'
                '3' -> 'e'
                '5' -> 's'
                // Persian / Arabic normalizations
                'ي', 'ى' -> 'ی'
                'ك' -> 'ک'
                'ة' -> 'ه'
                'ؤ', 'إ', 'أ', 'آ' -> 'ا'
                '\u200c', '\u200b', '\u200d' -> ' ' // Half-space / zero-width space
                // Arabic/Persian diacritics to ignore
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652' -> null
                ' ', '\t', '\n', '\r', '-', '_', '.', '*', '/', '\\', ',', ';', ':', '(', ')', '[', ']', '{', '}', '"', '\'' -> ' '
                else -> if (ch.isLetterOrDigit()) ch else ' '
            }
            if (mapped != null) {
                if (mapped == ' ') {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                } else {
                    sb.append(mapped)
                    lastWasSpace = false
                }
            }
        }
        return sb.toString().trim()
    }

    private fun extractSnippet(fullText: String, matched: String): String {
        val idx = fullText.indexOf(matched, ignoreCase = true)
        if (idx == -1) return fullText.take(60).replace("\n", " ").trim()
        val start = (idx - 20).coerceAtLeast(0)
        val end = (idx + matched.length + 20).coerceAtMost(fullText.length)
        return fullText.substring(start, end).replace("\n", " ").trim()
    }
}
