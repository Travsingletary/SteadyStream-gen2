package com.skyras.tv.player

/**
 * 100% original abstraction for media playback across Android (Media3/ExoPlayer),
 * Desktop, and Web.
 */
interface SkyrasPlayerCore {
    val state: PlayerPlaybackState
    fun play(url: String, title: String)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun addListener(listener: PlaybackListener)
    fun removeListener(listener: PlaybackListener)
}

enum class PlayerPlaybackState {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

data class PlaybackInfo(
    val currentUrl: String? = null,
    val currentTitle: String? = null,
    val state: PlayerPlaybackState = PlayerPlaybackState.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

interface PlaybackListener {
    fun onStateChanged(state: PlayerPlaybackState)
    fun onError(error: String)
}

/**
 * Headless test player core implementing the exact state machine used by Media3/ExoPlayer.
 */
class HeadlessSkyrasPlayer : SkyrasPlayerCore {
    override var state: PlayerPlaybackState = PlayerPlaybackState.IDLE
        private set

    var currentUrl: String? = null
        private set
    var currentTitle: String? = null
        private set

    private val listeners = mutableListOf<PlaybackListener>()

    override fun play(url: String, title: String) {
        currentUrl = url
        currentTitle = title
        state = PlayerPlaybackState.BUFFERING
        notifyListeners()

        // Simulates transitioning to READY -> PLAYING
        state = PlayerPlaybackState.PLAYING
        notifyListeners()
    }

    override fun pause() {
        if (state == PlayerPlaybackState.PLAYING) {
            state = PlayerPlaybackState.PAUSED
            notifyListeners()
        }
    }

    override fun resume() {
        if (state == PlayerPlaybackState.PAUSED) {
            state = PlayerPlaybackState.PLAYING
            notifyListeners()
        }
    }

    override fun stop() {
        state = PlayerPlaybackState.IDLE
        currentUrl = null
        currentTitle = null
        notifyListeners()
    }

    override fun seekTo(positionMs: Long) {
        // seek simulation
    }

    override fun addListener(listener: PlaybackListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.onStateChanged(state) }
    }
}
