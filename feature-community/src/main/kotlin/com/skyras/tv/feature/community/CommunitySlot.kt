package com.skyras.tv.feature.community

import kotlinx.serialization.Serializable

@Serializable
data class CommunityConfig(
    /**
     * Feature flag: defaults to false (disabled until enabled by operator).
     */
    val isEnabled: Boolean = false,

    /**
     * Target community URL (e.g. Discord server invite, Telegram group, Matrix room, web forum).
     */
    val targetUrl: String = "https://discord.gg/skyras",

    /**
     * Title and description displayed on the placeholder screen.
     */
    val title: String = "SKYRAS Media Community",
    val description: String = "Connect with other SKYRAS TV viewers, discuss live broadcasts, and request new features."
)

sealed class CommunityRouteState {
    data object Disabled : CommunityRouteState()
    data class Active(
        val title: String,
        val description: String,
        val targetUrl: String,
        val qrCodeNotice: String
    ) : CommunityRouteState()
}

/**
 * Community slot placeholder module.
 * Provides route state, navigation visibility check, and configurable deep-link target.
 * No external Discord SDK or network integration is performed here.
 */
class CommunitySlot(
    private var config: CommunityConfig = CommunityConfig()
) {

    fun isSlotVisible(): Boolean = config.isEnabled

    fun updateConfig(newConfig: CommunityConfig) {
        this.config = newConfig
    }

    fun getRouteState(): CommunityRouteState {
        if (!config.isEnabled) {
            return CommunityRouteState.Disabled
        }
        return CommunityRouteState.Active(
            title = config.title,
            description = config.description,
            targetUrl = config.targetUrl,
            qrCodeNotice = "Scan the QR code on your mobile device or open: ${config.targetUrl}"
        )
    }
}
