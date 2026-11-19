package com.example.data.blocklist

/**
 * One upstream blocklist repository (#13): a display name plus the URLs to
 * try in order — the canonical raw URL first, then mirrors of the same
 * repository, because any single host can be slow or filtered on some
 * networks Mehdi's users are on.
 */
data class BlocklistSource(
    val name: String,
    val urls: List<String>
)

/**
 * The adult domain list source (#12/#13, replaced in batch 2 #38).
 *
 * Batch 1 merged THREE repositories (StevenBlack's porn alternates, The
 * Block List Project, OISD NSFW). That over-blocked: StevenBlack's
 * "alternates/porn" file is the full unified hosts list PLUS porn, so
 * ordinary sites were blocked too. Mehdi's correction: exactly ONE
 * famous, strictly porn-only list.
 *
 * The single source is **OISD NSFW** (oisd.nl, by sjhgvr) — one of the
 * most widely used adult-content blocklists (bundled by Mullvad,
 * Technitium, OPNsense and the Pi-hole community), ~488k domains,
 * updated daily, curated specifically to contain ONLY adult content
 * with low collateral damage. StevenBlack's porn variant was the other
 * candidate and was rejected: its "porn" alternate still builds on the
 * unified list, which is what caused the over-blocking.
 *
 * The URLs are formats of the SAME list, tried in order: the official
 * plain-domain build first, then the project's own GitHub mirror
 * (sjhgvr/oisd), then a CDN mirror of that repository.
 */
object BlocklistSources {

    /** The one and only list source since #38. */
    val SOURCE: BlocklistSource = BlocklistSource(
        name = "OISD NSFW",
        urls = listOf(
            "https://nsfw.oisd.nl/domainswild2",
            "https://raw.githubusercontent.com/sjhgvr/oisd/main/oisd_nsfw.txt",
            "https://cdn.jsdelivr.net/gh/sjhgvr/oisd@main/oisd_nsfw.txt"
        )
    )

    /** Kept as a list for the updater's existing source loop: one entry. */
    val SOURCES: List<BlocklistSource> = listOf(SOURCE)

    /** The repository address, shown in the Adult Block card (#38). */
    const val SOURCE_REPOSITORY_URL = "https://github.com/sjhgvr/oisd"

    /** The list project's home, shown alongside the repository. */
    const val SOURCE_HOME_URL = "https://oisd.nl"

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
