package com.example.ui.i18n

import com.example.ui.theme.AppThemeState

/**
 * Bilingual English & Persian (فارسی) string helper with automatic RTL support.
 */
object AppStrings {
    val isFa: Boolean
        get() = AppThemeState.isPersianLanguage

    fun tr(en: String, fa: String): String = if (isFa) fa else en
}
