package com.skyras.tv.ui

import com.skyras.tv.core.iptv.PlaylistRepository
import com.skyras.tv.core.model.Channel
import com.skyras.tv.feature.ai.AiAssistEngine
import com.skyras.tv.feature.community.CommunityRouteState
import com.skyras.tv.feature.community.CommunitySlot
import com.skyras.tv.feature.sportspicks.SportsPicksAffiliateSlot
import com.skyras.tv.player.HeadlessSkyrasPlayer
import com.skyras.tv.player.PlayerPlaybackState
import com.skyras.tv.player.SkyrasPlayerCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TvScreenSection {
    LIVE_TV,
    VOD_MOVIES,
    SERIES,
    EPG_GUIDE,
    FAVORITES,
    AI_ASSIST,
    SPORTS_PICKS,
    COMMUNITY,
    SETTINGS
}

data class TvNavigationState(
    val selectedSection: TvScreenSection = TvScreenSection.LIVE_TV,
    val selectedCategoryIndex: Int = 0,
    val focusedChannelIndex: Int = 0,
    val isSideNavExpanded: Boolean = true
)

/**
 * TV D-Pad and Focus State Controller.
 * Manages 10-foot UI focus traversal, section routing, playback triggers,
 * and integration with AI Assist, Sports Picks, and Community slots.
 */
class TvFocusController(
    val playlistRepository: PlaylistRepository,
    val aiAssistEngine: AiAssistEngine,
    val sportsPicksSlot: SportsPicksAffiliateSlot,
    val communitySlot: CommunitySlot,
    val playerCore: SkyrasPlayerCore = HeadlessSkyrasPlayer()
) {

    private val _navState = MutableStateFlow(TvNavigationState())
    val navState: StateFlow<TvNavigationState> = _navState.asStateFlow()

    fun navigateTo(section: TvScreenSection) {
        _navState.value = _navState.value.copy(
            selectedSection = section,
            focusedChannelIndex = 0
        )
    }

    fun onDpadDown() {
        val current = _navState.value
        val channelCount = playlistRepository.channels.value.size
        if (current.focusedChannelIndex < channelCount - 1) {
            _navState.value = current.copy(focusedChannelIndex = current.focusedChannelIndex + 1)
        }
    }

    fun onDpadUp() {
        val current = _navState.value
        if (current.focusedChannelIndex > 0) {
            _navState.value = current.copy(focusedChannelIndex = current.focusedChannelIndex - 1)
        }
    }

    fun onDpadSelect(): Channel? {
        val channels = playlistRepository.channels.value
        val currentIndex = _navState.value.focusedChannelIndex
        if (currentIndex in channels.indices) {
            val selected = channels[currentIndex]
            playerCore.play(selected.streamUrl, selected.name)
            return selected
        }
        return null
    }

    fun getVisibleSections(): List<TvScreenSection> {
        val sections = mutableListOf(
            TvScreenSection.LIVE_TV,
            TvScreenSection.VOD_MOVIES,
            TvScreenSection.SERIES,
            TvScreenSection.EPG_GUIDE,
            TvScreenSection.FAVORITES,
            TvScreenSection.AI_ASSIST
        )
        if (sportsPicksSlot.isSlotVisible()) {
            sections.add(TvScreenSection.SPORTS_PICKS)
        }
        if (communitySlot.isSlotVisible()) {
            sections.add(TvScreenSection.COMMUNITY)
        }
        sections.add(TvScreenSection.SETTINGS)
        return sections
    }
}
