package com.example.data.local

import com.example.data.model.SectionBlockRule

object DefaultSectionRules {
    fun getDefaultRules(): List<SectionBlockRule> {
        return listOf(
            SectionBlockRule(
                targetPackage = "com.instagram.android",
                sectionName = "Instagram Reels & Explore",
                triggerPattern = "reels|ریلز|clips_viewer",
                viewIdPattern = "clips_viewer|reel_viewer|clips_video_container|clips_tab",
                isEnabled = false
            ),
            SectionBlockRule(
                targetPackage = "org.telegram.messenger",
                sectionName = "Telegram Search Box",
                triggerPattern = "search|جستجو|global search|جستجوی جهانی|search messages",
                viewIdPattern = "search_src_text|chat_search|search_field",
                isEnabled = false
            ),
            SectionBlockRule(
                targetPackage = "com.google.android.youtube",
                sectionName = "YouTube Shorts Feed",
                triggerPattern = "shorts|reel_player",
                viewIdPattern = "reel_recycler|reel_player_page_container|shorts_tab",
                isEnabled = false
            )
        )
    }
}
