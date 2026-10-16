package com.skyras.tv.feature.sportspicks

import kotlinx.serialization.Serializable

@Serializable
data class SportsPicksConfig(
    /**
     * Feature flag: defaults to false (strictly disabled until enabled by user/operator).
     */
    val isEnabled: Boolean = false,

    /**
     * Affiliate campaign identifier for SKYRAS Media tracking.
     */
    val affiliateTag: String = "skyras_media",

    /**
     * Configured licensed sportsbooks with affiliate referral URLs.
     */
    val partners: List<SportsbookPartner> = listOf(
        SportsbookPartner(
            id = "draftkings",
            name = "DraftKings Sportsbook",
            affiliateBaseUrl = "https://sportsbook.draftkings.com/r/affiliate",
            disclaimer = "21+ only. Gambling Problem? Call 1-800-GAMBLER. Regulated in eligible US states."
        ),
        SportsbookPartner(
            id = "fanduel",
            name = "FanDuel Sportsbook",
            affiliateBaseUrl = "https://sportsbook.fanduel.com/r/affiliate",
            disclaimer = "21+ only. Gambling Problem? Call 1-800-GAMBLER. Regulated in eligible US states."
        )
    )
)

@Serializable
data class SportsbookPartner(
    val id: String,
    val name: String,
    val affiliateBaseUrl: String,
    val disclaimer: String
)

/**
 * Sports Picks & Bet Affiliate Slot Module.
 *
 * IMPORTANT COMPLIANCE NOTICE:
 * - NO real-money wagering logic exists inside this application.
 * - This module is an external link-out affiliate stub only.
 * - All actions link out to officially licensed external sportsbook websites.
 * - STRICT TODO: Before production launch, enforce IP-based geolocation gating
 *   and verified age-gating (21+ in the US) to comply with state gaming commission rules.
 */
class SportsPicksAffiliateSlot(
    private var config: SportsPicksConfig = SportsPicksConfig()
) {

    fun isSlotVisible(): Boolean = config.isEnabled

    fun updateConfig(newConfig: SportsPicksConfig) {
        this.config = newConfig
    }

    /**
     * Generates a tracked outbound affiliate URL for a partner sportsbook.
     * Throws IllegalStateException if feature flag is disabled.
     */
    fun buildAffiliateOutboundUrl(
        partnerId: String,
        gameContext: String? = null,
        userAgeVerified: Boolean = false,
        userGeoJurisdictionAllowed: Boolean = false
    ): OutboundAffiliateResult {
        if (!config.isEnabled) {
            return OutboundAffiliateResult.Disabled(
                "Sports Picks feature flag is currently disabled."
            )
        }

        // TODO: Enforce strict geo-fence verification (State Gaming Commission compliance)
        if (!userGeoJurisdictionAllowed) {
            return OutboundAffiliateResult.BlockedByGeoGate(
                "Access to licensed sportsbooks is restricted in your current jurisdiction. [TODO: Integrate GeoComply / IP service]"
            )
        }

        // TODO: Enforce strict age verification (21+ in US jurisdictions)
        if (!userAgeVerified) {
            return OutboundAffiliateResult.BlockedByAgeGate(
                "You must be 21+ to view licensed sportsbook offers. [TODO: Age gate modal confirmation required]"
            )
        }

        val partner = config.partners.find { it.id == partnerId }
            ?: return OutboundAffiliateResult.Error("Partner '$partnerId' not found.")

        val queryParams = mutableListOf<String>()
        queryParams.add("tag=${config.affiliateTag}")
        if (!gameContext.isNullOrBlank()) {
            queryParams.add("event=${gameContext.trim().replace(" ", "+")}")
        }

        val fullUrl = "${partner.affiliateBaseUrl}?${queryParams.joinToString("&")}"
        return OutboundAffiliateResult.Allowed(
            partnerName = partner.name,
            targetUrl = fullUrl,
            disclaimer = partner.disclaimer
        )
    }
}

sealed class OutboundAffiliateResult {
    data class Allowed(val partnerName: String, val targetUrl: String, val disclaimer: String) : OutboundAffiliateResult()
    data class Disabled(val message: String) : OutboundAffiliateResult()
    data class BlockedByGeoGate(val message: String) : OutboundAffiliateResult()
    data class BlockedByAgeGate(val message: String) : OutboundAffiliateResult()
    data class Error(val message: String) : OutboundAffiliateResult()
}
