package com.example.data.local

import com.example.data.model.BlockedCategory
import com.example.data.model.BlockedKeyword

object DefaultKeywords {
    fun getPreloadedKeywords(): List<BlockedKeyword> {
        val adultWords = listOf(
            // English adult keywords
            "porn",
            "xxx",
            "nsfw",
            "hentai",
            "erotic",
            "hardcore porn",
            "softcore porn",
            "camgirl",
            "gangbang",
            "creampie",
            "blowjob",
            "dildo",
            "masturbat",
            "deepthroat",
            "threesome",
            "incest",
            "playboy",
            "nude photos",
            "boobs",
            "vagina",
            "penis",
            "fetish",
            "bdsm",
            "adult webcam",
            "sex video",
            "sex movie",
            "free sex",
            "milf porn",
            "teen porn",
            "anal sex",
            "shemale",
            "escort service",
            "erotica",
            "hardcore sex",
            "webcam model",
            "sexy video",
            "horny girls",
            "onlyfans leak",
            "strip show",
            "adult content",
            "adult game",
            "nudist",
            "erotic story",
            "sex chat",
            "adult chat",
            "sex hookup",
            "booty call",
            "erotic massage",

            // Persian (Farsi) adult keywords
            "پورن",
            "سکسی",
            "فیلم سوپر",
            "فیلم سکس",
            "عکس لخت",
            "پورنهاب",
            "کس",
            "کیر",
            "جنده",
            "جندق",
            "سکس چت",
            "داستان سکسی",
            "صیغه ساعتی",
            "دوست یابی سکسی",
            "همجنس باز",
            "شهوتی",
            "لزبین",
            "سکس ضربدری",
            "گی",
            "زن لخت",
            "سکس ایرانی",
            "کون",
            "ساک زدن",
            "فیلم خارجی سکسی",
            "سکس بدون فیلتر",
            "کلیپ سکسی",
            "سوپر ایرانی",
            "دختر حشری",
            "کانال سکسی",
            "گروه سکسی",
            "عکس سوپر",
            "چت سکسی",
            "پوزیشن سکس",
            "ارضا شدن",
            "جلق زدن",
            "خودارضایی"
        )

        val adultDomains = listOf(
            "pornhub.com",
            "xvideos.com",
            "xnxx.com",
            "redtube.com",
            "youporn.com",
            "chaturbate.com",
            "stripchat.com",
            "livejasmin.com",
            "cam4.com",
            "eporner.com",
            "hqporner.com",
            "spankbang.com",
            "tubegalore.com",
            "xhamster.com",
            "beeg.com",
            "brazzers.com",
            "youjizz.com",
            "rule34.xxx",
            "onlyfans.com",
            "camsoda.com",
            "bongacams.com",
            "flirt4free.com",
            "slutload.com",
            "fuq.com",
            "extremetube.com",
            "tnaflix.com",
            "nuvid.com",
            "sunporno.com",
            "porntrex.com",
            "txxx.com",
            "upornia.com",
            "cliphunter.com",
            "drtuber.com",
            "fapdu.com",
            "heavy-r.com",
            "tblop.com",
            "yespornplease.com"
        )

        val list = mutableListOf<BlockedKeyword>()
        for (w in adultWords) {
            list.add(
                BlockedKeyword(
                    text = w,
                    category = BlockedCategory.ADULT_EXPLICIT,
                    isEnabled = true,
                    isRegex = false
                )
            )
        }
        for (d in adultDomains) {
            list.add(
                BlockedKeyword(
                    text = d,
                    category = BlockedCategory.ADULT_DOMAIN,
                    isEnabled = true,
                    isRegex = false
                )
            )
        }
        return list
    }
}
