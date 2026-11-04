package com.example.data.local

import com.example.data.model.SectionBlockRule

object DefaultSectionRules {
    fun getDefaultRules(): List<SectionBlockRule> {
        return listOf(
            SectionBlockRule(
                targetPackage = "com.instagram.android",
                sectionName = "Instagram Reels",
                triggerPattern = "reels|ریلز",
                viewIdPattern = "clips_viewer|reel_viewer|clips_tab",
                isEnabled = true
            ),
            SectionBlockRule(
                targetPackage = "org.telegram.messenger",
                sectionName = "Telegram Search Section",
                triggerPattern = "search|جستجو|global search|جستجوی جهانی",
                viewIdPattern = "search_src_text|chat_search",
                isEnabled = true
            ),
            SectionBlockRule(
                targetPackage = "com.google.android.youtube",
                sectionName = "YouTube Shorts",
                triggerPattern = "shorts",
                viewIdPattern = "reel_player|shorts_tab",
                isEnabled = true
            ),
            SectionBlockRule(
                targetPackage = "com.zhiliaoapp.musically",
                sectionName = "TikTok Infinite Feed",
                triggerPattern = "for you",
                viewIdPattern = "feed_tab",
                isEnabled = false
            )
        )
    }
}
