package com.skyras.tv.feature.community

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommunitySlotTest {

    @Test
    fun testDefaultFeatureFlagDisabled() {
        val slot = CommunitySlot()
        assertFalse(slot.isSlotVisible())
        assertEquals(CommunityRouteState.Disabled, slot.getRouteState())
    }

    @Test
    fun testCustomConfigWhenEnabled() {
        val slot = CommunitySlot(
            CommunityConfig(
                isEnabled = true,
                targetUrl = "https://discord.gg/skyras-vip",
                title = "SKYRAS VIP Lounge"
            )
        )
        assertTrue(slot.isSlotVisible())

        val state = slot.getRouteState()
        assertTrue(state is CommunityRouteState.Active)
        val active = state as CommunityRouteState.Active
        assertEquals("SKYRAS VIP Lounge", active.title)
        assertEquals("https://discord.gg/skyras-vip", active.targetUrl)
        assertTrue(active.qrCodeNotice.contains("discord.gg/skyras-vip"))
    }
}
