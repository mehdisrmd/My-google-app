package com.example.data.blocklist

/**
 * One upstream blocklist repository (#13): a display name, the repository
 * address (shown in the app, #38), plus the URLs to try in order — the
 * canonical raw URL first, then mirrors of the same repository, because
 * any single host can be slow or filtered on some networks Mehdi's users
 * are on.
 */
data class BlocklistSource(
    val name: String,
    val repositoryUrl: String,
    val urls: List<String>
)

/**
 * The adult domain list sources (#12/#13, replaced in batch 2 #38, and
 * corrected the same evening after the lists were measured line by line).
 *
 * Mehdi's correction: famous repositories that filter ONLY pornographic
 * content — the batch-1 merge over-blocked useful sites. Measurement of
 * the actual lists (2026-11-19) found the over-blocker was The Block List
 * Project's porn.txt (161,825 *.tumblr.com and 70,275 *.blogspot.com
 * entries — effectively all of Tumblr and Blogspot) and, to a lesser
 * degree, the OISD NSFW full list (59,719 *.blogspot.com entries). Both
 * are dropped. The final set is exactly two curated porn-only lists:
 *
 * 1. **StevenBlack hosts — porn alternate** (github.com/StevenBlack/hosts,
 *    the most widely used hosts-file project): ~152k domains, ZERO
 *    blogspot entries. It bundles the base unified list (ads/malware/
 *    tracking subdomains) under the porn set, but never a bare useful
 *    site.
 * 2. **HaGeZi NSFW** (github.com/hagezi/dns-blocklists): ~86k domains,
 *    updated several times a day, a dedicated standalone adult-content
 *    list; only 322 *.blogspot.com and 3 *.tumblr.com entries, and zero
 *    hits on probed useful apex domains (github, google, youtube,
 *    wikipedia, reddit, …).
 *
 * Merged: ~224k domains (the batch-1 merge had ~1.23M). The user's own
 * added domains merge on top of these at update time (#38).
 */
object BlocklistSources {

    /** The list sources since the #38 correction: two, both porn-only. */
    val SOURCES: List<BlocklistSource> = listOf(
        BlocklistSource(
            name = "StevenBlack hosts (porn)",
            repositoryUrl = "https://github.com/StevenBlack/hosts",
            urls = listOf(
                "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts",
                "https://cdn.jsdelivr.net/gh/StevenBlack/hosts@master/alternates/porn/hosts"
            )
        ),
        BlocklistSource(
            name = "HaGeZi NSFW",
            repositoryUrl = "https://github.com/hagezi/dns-blocklists",
            urls = listOf(
                "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/nsfw-onlydomains.txt",
                "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@main/wildcard/nsfw-onlydomains.txt"
            )
        )
    )

    /**
     * Tiny built-in fallback used only when the very first download fails
     * (offline first launch): the domains that used to ship as the Adult
     * Preset before #12 replaced it with the downloadable list. It keeps the
     * "Block all adult sites" toggle meaningful offline until a real update
     * lands; it is NOT a substitute for the downloaded list.
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
