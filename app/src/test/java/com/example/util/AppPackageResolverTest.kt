package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure parts of [AppPackageResolver] used by the app picker (#10): custom
 * input resolution and app-family equivalence. These paths never touch a
 * PackageManager, so they run on the JVM.
 */
class AppPackageResolverTest {

    private val installed = listOf(
        ResolvedAppInfo("Instagram", "com.instagram.android", isInstalled = true),
        ResolvedAppInfo("Telegram", "org.telegram.messenger", isInstalled = true),
        ResolvedAppInfo("My Bank", "com.example.bank", isInstalled = true)
    )

    @Test
    fun customInputMatchingAnInstalledNameUsesItsRealPackage() {
        assertEquals(
            "com.example.bank",
            AppPackageResolver.resolvePackageForCustomInput("My Bank", "", installed)
        )
        assertEquals(
            "com.instagram.android",
            AppPackageResolver.resolvePackageForCustomInput("instagram", "", installed)
        )
    }

    @Test
    fun customInputFallsBackToKnownFamilyThenSlug() {
        // Known family, nothing installed: the family's first package wins.
        assertEquals(
            "com.zhiliaoapp.musically",
            AppPackageResolver.resolvePackageForCustomInput("TikTok", "", emptyList())
        )
        // Unknown name, blank package: a stable slug package is generated.
        assertEquals(
            "com.app.someapp",
            AppPackageResolver.resolvePackageForCustomInput("Some App!", "", emptyList())
        )
        // A real package typed by the user is kept as-is.
        assertEquals(
            "org.example.custom",
            AppPackageResolver.resolvePackageForCustomInput("Whatever", "org.example.custom", emptyList())
        )
    }

    @Test
    fun equivalentPackagesCoverKnownVariants() {
        val telegram = AppPackageResolver.getAllEquivalentPackages("org.telegram.messenger", "Telegram")
        assertTrue(telegram.contains("org.telegram.messenger"))
        assertTrue(telegram.contains("org.telegram.plus"))

        val x = AppPackageResolver.getAllEquivalentPackages("com.twitter.android", "x")
        assertTrue(x.contains("com.twitter.android.lite"))

        // An unknown package only ever equals itself.
        assertEquals(
            setOf("com.unknown.app"),
            AppPackageResolver.getAllEquivalentPackages("com.unknown.app", "")
        )
    }
}
