package com.gostream.app.ui

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.gostream.app.R
import com.gostream.app.data.api.CineFlowApiClient
import com.gostream.app.databinding.ActivityPlayerBinding
import com.gostream.app.player.MediaEngine
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var mediaEngine: MediaEngine
    private var exoPlayer: ExoPlayer? = null

    private var modelId: String = ""
    private var episodeId: String = ""
    private var videoId: String = ""
    private var videoTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on & fullscreen landscape
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modelId = intent.getStringExtra("extra_model_id") ?: ""
        episodeId = intent.getStringExtra("extra_episode_id") ?: ""
        videoId = intent.getStringExtra("extra_video_id") ?: ""
        videoTitle = intent.getStringExtra("extra_title") ?: "GOstream Player"

        setupCustomControls()
        initPlayer()
        resolveAndPlay()
    }

    private fun setupCustomControls() {
        val tvTitle = binding.playerView.findViewById<TextView>(R.id.tvVideoTitle)
        tvTitle?.text = videoTitle

        val btnBack = binding.playerView.findViewById<ImageButton>(R.id.btnBack)
        btnBack?.setOnClickListener { finish() }
    }

    private fun initPlayer() {
        mediaEngine = MediaEngine(this)
        exoPlayer = mediaEngine.createPlayer().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> binding.playerLoading.visibility = View.VISIBLE
                        Player.STATE_READY -> binding.playerLoading.visibility = View.GONE
                        Player.STATE_ENDED -> binding.playerLoading.visibility = View.GONE
                        Player.STATE_IDLE -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.playerLoading.visibility = View.GONE
                    binding.tvPlayerError.visibility = View.VISIBLE
                    binding.tvPlayerError.text = "Playback Error: ${error.message}"
                }
            })
        }
        binding.playerView.player = exoPlayer
    }

    private fun resolveAndPlay() {
        binding.playerLoading.visibility = View.VISIBLE
        binding.tvPlayerError.visibility = View.GONE

        lifecycleScope.launch {
            val source = CineFlowApiClient.getStreamSource(modelId, episodeId, videoId)
            if (source != null && !isFinishing) {
                val mediaSource = mediaEngine.buildMediaSource(source)
                exoPlayer?.setMediaSource(mediaSource)
                exoPlayer?.prepare()
                exoPlayer?.play()
            } else {
                binding.playerLoading.visibility = View.GONE
                binding.tvPlayerError.visibility = View.VISIBLE
                binding.tvPlayerError.text = "Gagal mendapatkan tautan streaming dari gateway provider."
            }
        }
    }

    /**
     * D-Pad Remote Navigation for Android TV
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    exoPlayer?.let {
                        val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                        it.seekTo(newPos)
                        binding.playerView.showController()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    exoPlayer?.let {
                        val newPos = (it.currentPosition + 10000).coerceAtMost(it.duration)
                        it.seekTo(newPos)
                        binding.playerView.showController()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    exoPlayer?.let {
                        if (it.isPlaying) it.pause() else it.play()
                        binding.playerView.showController()
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    exoPlayer?.let {
                        if (it.isPlaying) it.pause() else it.play()
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
