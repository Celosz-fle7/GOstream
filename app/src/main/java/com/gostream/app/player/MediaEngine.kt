package com.gostream.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.gostream.app.data.model.StreamSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@UnstableApi
class MediaEngine(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Builds ExoPlayer with RAM-only buffering configuration.
     * ZERO DISK CACHE to prevent storage ballooning!
     */
    fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // Min buffer: 15s in RAM
                30000, // Max buffer: 30s in RAM
                2500,  // Playback start buffer: 2.5s
                5000   // Rebuffer playback: 5s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    /**
     * Resolves the proper MediaSource (Progressive MP4, HLS m3u8, or DASH mpd)
     */
    fun buildMediaSource(source: StreamSource): MediaSource {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("CineFlow/0.2.9 (com.cineflow.app; Android 14; SDK 34)")

        if (source.headers.isNotEmpty()) {
            dataSourceFactory.setDefaultRequestProperties(source.headers)
        }

        val uri = Uri.parse(source.url)
        val mediaItem = MediaItem.fromUri(uri)

        return when {
            source.isHls || source.url.contains(".m3u8") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            source.isDash || source.url.contains(".mpd") -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }
}
