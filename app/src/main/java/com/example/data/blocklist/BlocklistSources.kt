package com.example.data.blocklist

/**
 * One upstream blocklist repository (#13): a display name plus the URLs to
 * try in order — the canonical raw URL first, then a jsDelivr CDN mirror of
 * the same repository, because raw.githubusercontent.com is slow or filtered
 * on some networks Mehdi's users are on.
 */
data class BlocklistSource(
    val name: String,
    val urls: List<String>
)

/**
 * The multi-repo source set for the adult domain list (#12/#13). The lists
 * are merged and deduplicated by [AdultBlocklistUpdater]; no single repo is
 * trusted to be complete or reachable on its own.
 */
object BlocklistSources {

    val SOURCES: List<BlocklistSource> = listOf(
        BlocklistSource(
            name = "StevenBlack hosts (porn)",
            urls = listOf(
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts",
                "https://cdn.jsdelivr.net/gh/StevenBlack/hosts@master/alternates/porn/hosts"
            )
        ),
        BlocklistSource(
            name = "The Block List Project (porn)",
            urls = listOf(
                "https://blocklistproject.github.io/Lists/porn.txt",
                "https://cdn.jsdelivr.net/gh/blocklistproject/Lists@master/porn.txt"
            )
        ),
        BlocklistSource(
            name = "OISD (nsfw)",
            urls = listOf(
                "https://raw.githubusercontent.com/sjhgvr/oisd/main/oisd_nsfw.txt",
                "https://cdn.jsdelivr.net/gh/sjhgvr/oisd@main/oisd_nsfw.txt"
            )
        )
    )

    /**
     * Tiny built-in fallback used only when the very first download fails
     * (offline first launch): the domains that used to ship as the Adult
     * Preset before #12 replaced it with the downloadable list. It keeps the
     * "Block all adult sites" toggle meaningful offline until a real update
     * lands; it is NOT a substitute for the merged list.
     */
    val FALLBACK_SEED_DOMAINS: Set<String> = setOf(
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
}
