package com.skyras.tv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import android.content.Intent
import com.skyras.tv.core.iptv.PlaylistRepository
import com.skyras.tv.core.model.Channel
import com.skyras.tv.core.model.EpgProgram
import com.skyras.tv.core.prefs.UserPreferencesRecord
import com.skyras.tv.core.prefs.UserPreferencesRepository
import com.skyras.tv.core.prefs.UserPrefsPayload
import com.skyras.tv.demo.DemoCatalog
import com.skyras.tv.playlist.PlaylistLoader
import com.skyras.tv.prefs.PreferenceServices
import com.skyras.tv.prefs.SessionStore
import com.skyras.tv.feature.ai.AiAssistEngine
import com.skyras.tv.feature.ai.MockLlmProvider
import com.skyras.tv.feature.community.CommunityConfig
import com.skyras.tv.feature.community.CommunityRouteState
import com.skyras.tv.feature.community.CommunitySlot
import com.skyras.tv.feature.sportspicks.OutboundAffiliateResult
import com.skyras.tv.feature.sportspicks.SportsPicksAffiliateSlot
import com.skyras.tv.feature.sportspicks.SportsPicksConfig
import com.skyras.tv.player.Media3SkyrasPlayer
import com.skyras.tv.ui.TvFocusController
import com.skyras.tv.ui.TvScreenSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val hideBannerHandler = Handler(Looper.getMainLooper())

    private lateinit var repository: PlaylistRepository
    private lateinit var controller: TvFocusController
    private lateinit var player: Media3SkyrasPlayer
    private lateinit var session: SessionStore
    private lateinit var prefsRepo: UserPreferencesRepository

    private lateinit var playerView: PlayerView
    private lateinit var channelOverlay: View
    private lateinit var channelList: LinearLayout
    private lateinit var nowNextBanner: View
    private lateinit var bannerChannel: TextView
    private lateinit var bannerNow: TextView
    private lateinit var bannerNext: TextView
    private lateinit var featureMenu: View
    private lateinit var menuAi: TextView
    private lateinit var menuBet: TextView
    private lateinit var menuCommunity: TextView
    private lateinit var menuSignOut: TextView
    private lateinit var featurePanel: View
    private lateinit var featurePanelTitle: TextView
    private lateinit var featurePanelBody: TextView

    private var menuIndex = 0
    private var overlayVisible = false
    private var menuVisible = false
    private var panelVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        channelOverlay = findViewById(R.id.channel_overlay)
        channelList = findViewById(R.id.channel_list)
        nowNextBanner = findViewById(R.id.now_next_banner)
        bannerChannel = findViewById(R.id.banner_channel)
        bannerNow = findViewById(R.id.banner_now)
        bannerNext = findViewById(R.id.banner_next)
        featureMenu = findViewById(R.id.feature_menu)
        menuAi = findViewById(R.id.menu_ai)
        menuBet = findViewById(R.id.menu_bet)
        menuCommunity = findViewById(R.id.menu_community)
        menuSignOut = findViewById(R.id.menu_sign_out)
        featurePanel = findViewById(R.id.feature_panel)
        featurePanelTitle = findViewById(R.id.feature_panel_title)
        featurePanelBody = findViewById(R.id.feature_panel_body)

        session = SessionStore(this)
        if (!session.isSignedIn || session.username.isBlank()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        prefsRepo = PreferenceServices.repository(this)

        repository = PlaylistRepository()

        val aiEngine = AiAssistEngine(MockLlmProvider(), repository)
        val sportsSlot = SportsPicksAffiliateSlot(SportsPicksConfig(isEnabled = true))
        val communitySlot = CommunitySlot(CommunityConfig(isEnabled = true))
        player = Media3SkyrasPlayer(this)
        controller = TvFocusController(
            playlistRepository = repository,
            aiAssistEngine = aiEngine,
            sportsPicksSlot = sportsSlot,
            communitySlot = communitySlot,
            playerCore = player
        )

        playerView.player = player.exoPlayer
        renderChannelList()
        loadUserLineup()
    }

    override fun onPause() {
        persistPreferences()
        super.onPause()
    }

    override fun onStop() {
        if (::player.isInitialized) {
            player.pause()
        }
        super.onStop()
    }

    override fun onDestroy() {
        hideBannerHandler.removeCallbacksAndMessages(null)
        uiScope.cancel()
        if (::player.isInitialized) {
            player.release()
        }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (panelVisible) {
                    closePanel()
                    return true
                }
                if (menuVisible) {
                    hideMenu()
                    return true
                }
                if (overlayVisible) {
                    hideOverlay()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (panelVisible) {
                    closePanel()
                    return true
                }
                if (menuVisible) {
                    openSelectedMenu()
                    return true
                }
                if (overlayVisible) {
                    tuneCurrent()
                    hideOverlay()
                    return true
                }
                showOverlay()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (menuVisible) {
                    menuIndex = (menuIndex - 1).coerceAtLeast(0)
                    highlightMenu()
                    return true
                }
                controller.onDpadUp()
                if (overlayVisible) {
                    renderChannelList()
                } else {
                    tuneCurrent()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (menuVisible) {
                    menuIndex = (menuIndex + 1).coerceAtMost(3)
                    highlightMenu()
                    return true
                }
                controller.onDpadDown()
                if (overlayVisible) {
                    renderChannelList()
                } else {
                    tuneCurrent()
                }
                return true
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_TV_CONTENTS_MENU, KeyEvent.KEYCODE_INFO -> {
                if (menuVisible) hideMenu() else showMenu()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun currentChannel(): Channel? {
        val channels = repository.channels.value
        val index = controller.navState.value.focusedChannelIndex
        return channels.getOrNull(index)
    }

    private fun nextProgram(channelId: String, now: Long): EpgProgram? {
        val programs = repository.epgPrograms.value[channelId].orEmpty()
        return programs.filter { it.startEpochMillis > now }.minByOrNull { it.startEpochMillis }
    }

    private fun tuneCurrent() {
        val selected = controller.onDpadSelect() ?: return
        updateBanner(selected)
        showBannerTemporarily()
        renderChannelList()
    }

    private fun updateBanner(channel: Channel) {
        val now = System.currentTimeMillis()
        val nowPlaying = repository.getNowPlaying(channel.id, now)
        val upcoming = nextProgram(channel.id, nowPlaying?.endEpochMillis ?: now)
        val number = channel.channelNumber?.toString() ?: ""
        bannerChannel.text = "$number  ${channel.name}"
        bannerNow.text = nowPlaying?.title ?: "Live demo stream"
        bannerNext.text = upcoming?.title ?: "—"
    }

    private fun renderChannelList() {
        channelList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val focused = controller.navState.value.focusedChannelIndex
        val now = System.currentTimeMillis()
        repository.channels.value.forEachIndexed { index, channel ->
            val row = inflater.inflate(R.layout.item_channel, channelList, false)
            row.findViewById<TextView>(R.id.channel_number).text =
                (channel.channelNumber ?: (index + 1)).toString()
            row.findViewById<TextView>(R.id.channel_name).text = channel.name
            row.findViewById<TextView>(R.id.channel_now).text =
                repository.getNowPlaying(channel.id, now)?.title ?: channel.categoryName
            if (index == focused) {
                row.setBackgroundColor(0xCC1E293B.toInt())
            }
            channelList.addView(row)
        }
    }

    private fun showOverlay() {
        overlayVisible = true
        hideMenu()
        closePanel()
        channelOverlay.visibility = View.VISIBLE
        nowNextBanner.visibility = View.VISIBLE
        renderChannelList()
    }

    private fun hideOverlay() {
        overlayVisible = false
        channelOverlay.visibility = View.GONE
        showBannerTemporarily()
    }

    private fun showMenu() {
        menuVisible = true
        overlayVisible = false
        channelOverlay.visibility = View.GONE
        closePanel()
        controller.navigateTo(TvScreenSection.AI_ASSIST)
        featureMenu.visibility = View.VISIBLE
        highlightMenu()
    }

    private fun hideMenu() {
        menuVisible = false
        featureMenu.visibility = View.GONE
    }

    private fun highlightMenu() {
        val items = listOf(menuAi, menuBet, menuCommunity, menuSignOut)
        items.forEachIndexed { index, view ->
            view.setBackgroundColor(if (index == menuIndex) 0xCC1E293B.toInt() else 0x00000000)
        }
    }

    private fun openSelectedMenu() {
        hideMenu()
        when (menuIndex) {
            0 -> openAiAssist()
            1 -> openBet()
            2 -> openCommunity()
            else -> signOut()
        }
    }

    private fun openAiAssist() {
        controller.navigateTo(TvScreenSection.AI_ASSIST)
        featurePanelTitle.text = getString(R.string.menu_ai)
        featurePanelBody.text = "Asking SKYRAS Offline Mock AI…"
        showPanel()
        uiScope.launch {
            val response = controller.aiAssistEngine.processQuery("what's on right now")
            val matches = response.matchedChannels.joinToString("\n") { "• ${it.name}" }
            featurePanelBody.text = buildString {
                append(response.replyText)
                append("\n\n")
                if (matches.isNotBlank()) {
                    append(matches)
                    append("\n\n")
                }
                append(response.suggestedActions.joinToString("\n") { "→ $it" })
            }
        }
    }

    private fun openBet() {
        controller.navigateTo(TvScreenSection.SPORTS_PICKS)
        val result = controller.sportsPicksSlot.buildAffiliateOutboundUrl(
            partnerId = "draftkings",
            gameContext = "demo",
            userAgeVerified = false,
            userGeoJurisdictionAllowed = false
        )
        val body = when (result) {
            is OutboundAffiliateResult.Allowed -> "${result.partnerName}\n${result.targetUrl}\n${result.disclaimer}"
            is OutboundAffiliateResult.Disabled -> result.message
            is OutboundAffiliateResult.BlockedByGeoGate -> result.message
            is OutboundAffiliateResult.BlockedByAgeGate -> result.message
            is OutboundAffiliateResult.Error -> result.message
        }
        featurePanelTitle.text = getString(R.string.menu_bet)
        featurePanelBody.text = "Feature flag ON (demo). No in-app wagering.\n\n$body"
        showPanel()
    }

    private fun openCommunity() {
        controller.navigateTo(TvScreenSection.COMMUNITY)
        val state = controller.communitySlot.getRouteState()
        featurePanelTitle.text = getString(R.string.menu_community)
        featurePanelBody.text = when (state) {
            is CommunityRouteState.Disabled -> "Community slot is disabled."
            is CommunityRouteState.Active -> "${state.title}\n\n${state.description}\n\n${state.qrCodeNotice}"
        }
        showPanel()
    }

    private fun showPanel() {
        panelVisible = true
        featurePanel.visibility = View.VISIBLE
    }

    private fun closePanel() {
        panelVisible = false
        featurePanel.visibility = View.GONE
        controller.navigateTo(TvScreenSection.LIVE_TV)
    }

    private fun showBannerTemporarily() {
        nowNextBanner.visibility = View.VISIBLE
        hideBannerHandler.removeCallbacksAndMessages(null)
        hideBannerHandler.postDelayed({
            if (!overlayVisible) {
                nowNextBanner.visibility = View.GONE
            }
        }, 4500)
    }

    private fun loadUserLineup() {
        uiScope.launch {
            val stored = prefsRepo.load(session.username)
            if (session.useDemoCatalog || session.m3uUrl.isNullOrBlank()) {
                repository.loadChannels(DemoCatalog.channels())
                repository.loadEpg(DemoCatalog.dummyEpg())
            } else {
                val fetched = runCatching { PlaylistLoader().fetchChannels(session.m3uUrl.orEmpty()) }
                if (fetched.isSuccess) {
                    repository.loadChannels(fetched.getOrThrow())
                } else {
                    repository.loadChannels(DemoCatalog.channels())
                    repository.loadEpg(DemoCatalog.dummyEpg())
                    bannerNow.text = fetched.exceptionOrNull()?.message ?: "Playlist failed; demo lineup"
                }
            }
            stored?.prefs?.favoriteIds?.let { repository.restoreFavorites(it.toSet()) }
            val lastIndex = stored?.prefs?.lastChannelIndex ?: 0
            val lastId = stored?.prefs?.lastChannelId
            val byId = lastId?.let { id -> repository.channels.value.indexOfFirst { it.id == id } } ?: -1
            controller.focusChannelIndex(if (byId >= 0) byId else lastIndex)
            renderChannelList()
            tuneCurrent()
            showBannerTemporarily()
        }
    }

    private fun persistPreferences() {
        if (!::session.isInitialized || !session.isSignedIn) return
        val channel = currentChannel()
        val record = UserPreferencesRecord(
            username = session.username,
            m3uUrl = session.m3uUrl,
            prefs = UserPrefsPayload(
                favoriteIds = repository.favoriteIds.value.toList(),
                lastChannelId = channel?.id,
                lastChannelIndex = controller.navState.value.focusedChannelIndex
            ),
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        uiScope.launch {
            prefsRepo.save(record)
        }
    }

    private fun signOut() {
        persistPreferences()
        session.signOut()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}
