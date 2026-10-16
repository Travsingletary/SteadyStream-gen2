package com.skyras.tv.feature.sportspicks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SportsPicksAffiliateSlotTest {

    @Test
    fun testFeatureFlagDefaultsToFalse() {
        val slot = SportsPicksAffiliateSlot()
        assertFalse(slot.isSlotVisible())

        val result = slot.buildAffiliateOutboundUrl("draftkings")
        assertTrue(result is OutboundAffiliateResult.Disabled)
    }

    @Test
    fun testGeoGateAndAgeGateBlocksWhenNotVerified() {
        val slot = SportsPicksAffiliateSlot(SportsPicksConfig(isEnabled = true))
        assertTrue(slot.isSlotVisible())

        // Blocked by geo gate
        val geoBlocked = slot.buildAffiliateOutboundUrl(
            partnerId = "draftkings",
            userAgeVerified = true,
            userGeoJurisdictionAllowed = false
        )
        assertTrue(geoBlocked is OutboundAffiliateResult.BlockedByGeoGate)

        // Blocked by age gate
        val ageBlocked = slot.buildAffiliateOutboundUrl(
            partnerId = "draftkings",
            userAgeVerified = false,
            userGeoJurisdictionAllowed = true
        )
        assertTrue(ageBlocked is OutboundAffiliateResult.BlockedByAgeGate)
    }

    @Test
    fun testGeneratesAffiliateOutboundUrlWhenCompliant() {
        val slot = SportsPicksAffiliateSlot(
            SportsPicksConfig(
                isEnabled = true,
                affiliateTag = "skyras_vip_2026"
            )
        )

        val result = slot.buildAffiliateOutboundUrl(
            partnerId = "draftkings",
            gameContext = "Lakers vs Celtics",
            userAgeVerified = true,
            userGeoJurisdictionAllowed = true
        )

        assertTrue(result is OutboundAffiliateResult.Allowed)
        val allowed = result as OutboundAffiliateResult.Allowed
        assertEquals("DraftKings Sportsbook", allowed.partnerName)
        assertTrue(allowed.targetUrl.contains("tag=skyras_vip_2026"))
        assertTrue(allowed.targetUrl.contains("event=Lakers+vs+Celtics"))
        assertTrue(allowed.disclaimer.contains("1-800-GAMBLER"))
    }
}
