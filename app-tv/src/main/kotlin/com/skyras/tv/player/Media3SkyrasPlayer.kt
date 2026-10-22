package com.skyras.tv.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Android Media3/ExoPlayer implementation of [SkyrasPlayerCore].
 * [HeadlessSkyrasPlayer] remains the JVM unit-test double.
 */
class Media3SkyrasPlayer(context: Context) : SkyrasPlayerCore {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val listeners = mutableListOf<PlaybackListener>()

    override var state: PlayerPlaybackState = PlayerPlaybackState.IDLE
        private set

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state = when (playbackState) {
                    Player.STATE_IDLE -> PlayerPlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlayerPlaybackState.BUFFERING
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) PlayerPlaybackState.PLAYING else PlayerPlaybackState.READY
                    }
                    Player.STATE_ENDED -> PlayerPlaybackState.ENDED
                    else -> PlayerPlaybackState.IDLE
                }
                notifyState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    state = if (isPlaying) PlayerPlaybackState.PLAYING else PlayerPlaybackState.PAUSED
                    notifyState()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                state = PlayerPlaybackState.ERROR
                listeners.forEach { it.onError(error.message ?: "Playback error") }
                notifyState()
            }
        })
    }

    override fun play(url: String, title: String) {
        state = PlayerPlaybackState.BUFFERING
        notifyState()
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun resume() {
        exoPlayer.play()
    }

    override fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        state = PlayerPlaybackState.IDLE
        notifyState()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun addListener(listener: PlaybackListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }

    fun release() {
        exoPlayer.release()
    }

    private fun notifyState() {
        listeners.forEach { it.onStateChanged(state) }
    }
}
