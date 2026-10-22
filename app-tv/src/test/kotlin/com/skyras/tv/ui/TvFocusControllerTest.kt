package com.skyras.tv.ui

import com.skyras.tv.core.iptv.PlaylistRepository
import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.ContentType
import com.skyras.tv.feature.ai.AiAssistEngine
import com.skyras.tv.feature.ai.MockLlmProvider
import com.skyras.tv.feature.community.CommunityConfig
import com.skyras.tv.feature.community.CommunitySlot
import com.skyras.tv.feature.sportspicks.SportsPicksAffiliateSlot
import com.skyras.tv.feature.sportspicks.SportsPicksConfig
import com.skyras.tv.player.PlayerPlaybackState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TvFocusControllerTest {

    @Test
    fun testNavigationAndDpadTraversal() {
        val repo = PlaylistRepository()
        val channels = listOf(
            Channel("1", "SKYRAS Live 1", "http://mock/1", type = ContentType.LIVE),
            Channel("2", "SKYRAS Live 2", "http://mock/2", type = ContentType.LIVE),
            Channel("3", "SKYRAS Live 3", "http://mock/3", type = ContentType.LIVE)
        )
        repo.loadChannels(channels)

        val aiEngine = AiAssistEngine(MockLlmProvider(), repo)
        val sportsSlot = SportsPicksAffiliateSlot(SportsPicksConfig(isEnabled = false))
        val communitySlot = CommunitySlot(CommunityConfig(isEnabled = false))

        val controller = TvFocusController(repo, aiEngine, sportsSlot, communitySlot)

        // Initial state
        assertEquals(0, controller.navState.value.focusedChannelIndex)
        assertEquals(TvScreenSection.LIVE_TV, controller.navState.value.selectedSection)

        // D-Pad Down twice
        controller.onDpadDown()
        assertEquals(1, controller.navState.value.focusedChannelIndex)
        controller.onDpadDown()
        assertEquals(2, controller.navState.value.focusedChannelIndex)

        // D-Pad Up
        controller.onDpadUp()
        assertEquals(1, controller.navState.value.focusedChannelIndex)

        // Select channel triggers player
        val selected = controller.onDpadSelect()
        assertEquals("SKYRAS Live 2", selected?.name)
        assertEquals(PlayerPlaybackState.PLAYING, controller.playerCore.state)

        controller.focusChannelIndex(0)
        assertEquals(0, controller.navState.value.focusedChannelIndex)
        controller.focusChannelIndex(99)
        assertEquals(2, controller.navState.value.focusedChannelIndex)

        // Feature flags off -> Sports Picks & Community absent from visible sections
        val sectionsNoFlags = controller.getVisibleSections()
        assertFalse(sectionsNoFlags.contains(TvScreenSection.SPORTS_PICKS))
        assertFalse(sectionsNoFlags.contains(TvScreenSection.COMMUNITY))

        // Enable feature flags
        sportsSlot.updateConfig(SportsPicksConfig(isEnabled = true))
        communitySlot.updateConfig(CommunityConfig(isEnabled = true))

        val sectionsWithFlags = controller.getVisibleSections()
        assertTrue(sectionsWithFlags.contains(TvScreenSection.SPORTS_PICKS))
        assertTrue(sectionsWithFlags.contains(TvScreenSection.COMMUNITY))
    }
}
