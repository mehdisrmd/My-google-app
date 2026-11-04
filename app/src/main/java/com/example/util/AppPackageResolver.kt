package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.AppUsageLimit
import java.util.Locale

data class ResolvedAppInfo(
    val appName: String,
    val packageName: String,
    val isInstalled: Boolean
)

object AppPackageResolver {

    /**
     * Known popular apps and all their real-world package name variants/clients.
     * This ensures that setting a limit on e.g. "Telegram", "Instagram", "YouTube", "Chrome",
     * or "TikTok" works across official apps, Lite versions, regional variants, and custom clients.
     */
    private val KNOWN_APP_FAMILIES: Map<String, List<String>> = mapOf(
        "Instagram" to listOf(
            "com.instagram.android",
            "com.instagram.lite"
        ),
        "Telegram" to listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "org.telegram.plus",
            "tw.nekomimi.nekogram",
            "ir.ilmili.telegraph"
        ),
        "YouTube" to listOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube.tv",
            "app.revanced.android.youtube"
        ),
        "TikTok" to listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically.go"
        ),
        "Chrome" to listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "org.chromium.chrome",
            "com.android.browser"
        ),
        "X (Twitter)" to listOf(
            "com.twitter.android",
            "com.twitter.android.lite"
        ),
        "WhatsApp" to listOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        ),
        "Snapchat" to listOf(
            "com.snapchat.android"
        ),
        "Reddit" to listOf(
            "com.reddit.frontpage"
        ),
        "Discord" to listOf(
            "com.discord"
        ),
        "Netflix" to listOf(
            "com.netflix.mediaclient"
        ),
        "Twitch" to listOf(
            "tv.twitch.android.app"
        ),
        "Threads" to listOf(
            "com.instagram.barcelona"
        ),
        "Pinterest" to listOf(
            "com.pinterest"
        ),
        "Facebook" to listOf(
            "com.facebook.katana",
            "com.facebook.lite",
            "com.facebook.orca"
        ),
        "Spotify" to listOf(
            "com.spotify.music",
            "com.spotify.lite"
        ),
        "Settings" to listOf(
            "com.android.settings"
        ),
        "Messages" to listOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging"
        ),
        "Clock" to listOf(
            "com.google.android.deskclock",
            "com.android.deskclock"
        ),
        "Contacts" to listOf(
            "com.google.android.contacts",
            "com.android.contacts"
        ),
        "Files" to listOf(
            "com.google.android.documentsui",
            "com.android.documentsui",
            "com.google.android.apps.nbu.files"
        ),
        "Camera" to listOf(
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.android.camera"
        ),
        "Play Store" to listOf(
            "com.android.vending"
        )
    )

    /**
     * System UI and keyboard packages that emit AccessibilityEvents while the user is typing or
     * interacting with the status bar, which must NEVER be treated as switching away from the
     * current foreground app.
     */
    private val TRANSIENT_SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.touchtype.swiftkey",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "android"
    )

    fun isTransientSystemPackage(packageName: String, ownPackageName: String): Boolean {
        if (packageName.isBlank() || packageName == ownPackageName) return true
        if (packageName in TRANSIENT_SYSTEM_PACKAGES) return true
        if (packageName.contains("inputmethod", ignoreCase = true) ||
            packageName.contains("keyboard", ignoreCase = true)
        ) {
            return true
        }
        return false
    }

    /**
     * Queries all launchable & browsable apps currently installed on the device.
     */
    fun queryInstalledApps(context: Context): List<ResolvedAppInfo> {
        return try {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            val resolveInfos = (pm.queryIntentActivities(launcherIntent, 0) +
                pm.queryIntentActivities(browserIntent, 0))

            resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName?.trim() ?: return@mapNotNull null
                if (pkg.isEmpty() || pkg == context.packageName) return@mapNotNull null
                val rawLabel = runCatching { info.loadLabel(pm)?.toString()?.trim() }.getOrNull()
                val label = if (!rawLabel.isNullOrEmpty()) rawLabel else pkg.substringAfterLast('.')
                ResolvedAppInfo(
                    appName = label,
                    packageName = pkg,
                    isInstalled = true
                )
            }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase(Locale.ROOT) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns popular preset apps enriched with whether each app (or an alias variant) is installed on this device.
     */
    fun getPopularPresetApps(context: Context): List<ResolvedAppInfo> {
        val installedByPkg = queryInstalledApps(context).associateBy { it.packageName.lowercase(Locale.ROOT) }
        val popularNames = listOf(
            "Instagram",
            "Telegram",
            "YouTube",
            "TikTok",
            "Chrome",
            "X (Twitter)",
            "WhatsApp",
            "Reddit",
            "Discord",
            "Snapchat",
            "Netflix",
            "Twitch",
            "Threads",
            "Pinterest",
            "Facebook",
            "Spotify"
        )

        return popularNames.map { name ->
            val variants = KNOWN_APP_FAMILIES[name].orEmpty()
            val installedVariant = variants.firstOrNull { installedByPkg.containsKey(it.lowercase(Locale.ROOT)) }
            val chosenPkg = installedVariant ?: variants.firstOrNull() ?: "com.app.${name.lowercase(Locale.ROOT)}"
            ResolvedAppInfo(
                appName = name,
                packageName = chosenPkg,
                isInstalled = installedVariant != null || isDirectPackageInstalled(context, chosenPkg)
            )
        }
    }

    fun isDirectPackageInstalled(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            try {
                context.packageManager.getLaunchIntentForPackage(packageName) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    fun isAppLimitInstalledOnDevice(context: Context, packageName: String, appName: String = ""): Boolean {
        val equivalents = getAllEquivalentPackages(packageName, appName)
        for (pkg in equivalents) {
            if (isDirectPackageInstalled(context, pkg)) return true
        }
        val installed = queryInstalledApps(context)
        return installed.any {
            it.packageName.equals(packageName, ignoreCase = true) ||
                (appName.isNotBlank() && it.appName.equals(appName.trim(), ignoreCase = true))
        }
    }

    fun launchLimitedApp(context: Context, packageName: String, appName: String = ""): Boolean {
        val equivalents = getAllEquivalentPackages(packageName, appName)
        for (pkg in equivalents) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    /**
     * Returns all package names that belong to the same app family as [packageName] or [appName].
     */
    fun getAllEquivalentPackages(packageName: String, appName: String = ""): Set<String> {
        val result = linkedSetOf<String>()
        val cleanPkg = packageName.trim()
        if (cleanPkg.isNotEmpty()) {
            result.add(cleanPkg)
        }
        val cleanName = appName.trim()

        for ((familyName, pkgList) in KNOWN_APP_FAMILIES) {
            val matchesPkg = pkgList.any { it.equals(cleanPkg, ignoreCase = true) }
            val matchesName = cleanName.isNotEmpty() && (
                familyName.equals(cleanName, ignoreCase = true) ||
                    (familyName == "X (Twitter)" && (cleanName.equals("twitter", ignoreCase = true) || cleanName.equals("x", ignoreCase = true)))
                )
            if (matchesPkg || matchesName) {
                result.addAll(pkgList)
            }
        }
        return result
    }

    /**
     * Checks if an active foreground [candidatePackage] matches the given [limit] by exact package,
     * known alias family, or installed app label.
     */
    fun matchesAppLimit(
        limit: AppUsageLimit,
        candidatePackage: String,
        context: Context? = null
    ): Boolean {
        if (candidatePackage.isBlank()) return false
        if (limit.packageName.equals(candidatePackage, ignoreCase = true)) return true

        val equivalents = getAllEquivalentPackages(limit.packageName, limit.appName)
        if (equivalents.any { it.equals(candidatePackage, ignoreCase = true) }) {
            return true
        }

        // Also match if telegram/instagram/youtube/chrome/tiktok prefix matches
        val lowerCandidate = candidatePackage.lowercase(Locale.ROOT)
        val lowerLimitPkg = limit.packageName.lowercase(Locale.ROOT)
        if (lowerLimitPkg.contains("telegram") && lowerCandidate.contains("telegram")) return true
        if (lowerLimitPkg.contains("instagram") && lowerCandidate.contains("instagram")) return true

        if (context != null && limit.appName.isNotBlank()) {
            val label = getInstalledAppLabel(context, candidatePackage)
            if (label != null && label.equals(limit.appName.trim(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun matchesPackageOrVariant(
        ruleTargetPackage: String,
        ruleSectionOrAppName: String,
        candidatePackage: String
    ): Boolean {
        if (ruleTargetPackage == "*") return true
        if (ruleTargetPackage.equals(candidatePackage, ignoreCase = true)) return true
        val equivalents = getAllEquivalentPackages(ruleTargetPackage, ruleSectionOrAppName)
        if (equivalents.any { it.equals(candidatePackage, ignoreCase = true) }) return true

        val lowerCandidate = candidatePackage.lowercase(Locale.ROOT)
        val lowerRulePkg = ruleTargetPackage.lowercase(Locale.ROOT)
        if (lowerRulePkg.contains("telegram") && lowerCandidate.contains("telegram")) return true
        if (lowerRulePkg.contains("instagram") && lowerCandidate.contains("instagram")) return true
        return false
    }

    /**
     * Resolves the best real package name when a user enters a custom app name or package.
     */
    fun resolvePackageForCustomInput(
        appNameInput: String,
        packageInput: String,
        installedApps: List<ResolvedAppInfo>
    ): String {
        val cleanName = appNameInput.trim()
        val cleanPkg = packageInput.trim()

        // 1. If user typed an app name that matches an installed app on the device, use its real package!
        if (cleanName.isNotEmpty()) {
            val exactInstalled = installedApps.firstOrNull {
                it.appName.equals(cleanName, ignoreCase = true)
            }
            if (exactInstalled != null && (cleanPkg.isEmpty() || !cleanPkg.contains("."))) {
                return exactInstalled.packageName
            }

            // 2. Check known app families by name
            for ((familyName, pkgList) in KNOWN_APP_FAMILIES) {
                if (familyName.equals(cleanName, ignoreCase = true) ||
                    familyName.lowercase(Locale.ROOT).contains(cleanName.lowercase(Locale.ROOT))
                ) {
                    val installedMatch = pkgList.firstOrNull { candidate ->
                        installedApps.any { it.packageName.equals(candidate, ignoreCase = true) }
                    }
                    if (installedMatch != null && (cleanPkg.isEmpty() || !cleanPkg.contains("."))) {
                        return installedMatch
                    }
                    if (cleanPkg.isEmpty() || !cleanPkg.contains(".")) {
                        return pkgList.first()
                    }
                }
            }

            // 3. Partial match on installed app name if package was left blank
            if (cleanPkg.isEmpty()) {
                val partialInstalled = installedApps.firstOrNull {
                    it.appName.contains(cleanName, ignoreCase = true) ||
                        it.packageName.contains(cleanName, ignoreCase = true)
                }
                if (partialInstalled != null) {
                    return partialInstalled.packageName
                }
            }
        }

        if (cleanPkg.isNotEmpty() && cleanPkg.contains(".")) {
            return cleanPkg
        }

        val slug = cleanName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        return if (slug.isNotEmpty()) "com.app.$slug" else "com.app.custom"
    }

    private fun getInstalledAppLabel(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo)?.toString()?.trim()
        } catch (_: Exception) {
            null
        }
    }
}
