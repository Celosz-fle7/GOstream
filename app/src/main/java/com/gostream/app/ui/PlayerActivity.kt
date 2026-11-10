package com.gostream.app.ui

import android.content.pm.ActivityInfo
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
import com.gostream.app.data.db.GOStreamDatabase
import com.gostream.app.data.moviebox.MovieBoxClient
import com.gostream.app.databinding.ActivityPlayerBinding
import com.gostream.app.player.MediaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var mediaEngine: MediaEngine
    private var exoPlayer: ExoPlayer? = null

    private var videoId: String = ""
    private var episodeNumber: Int = 1
    private var videoTitle: String = ""
    private var posterUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on & fullscreen landscape
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoId = intent.getStringExtra("extra_video_id") ?: ""
        episodeNumber = intent.getIntExtra("extra_episode_num", 1)
        videoTitle = intent.getStringExtra("extra_title") ?: "GOstream Player"
        posterUrl = intent.getStringExtra("extra_poster") ?: ""

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
                        Player.STATE_ENDED -> {
                            binding.playerLoading.visibility = View.GONE
                            saveCurrentProgress()
                        }
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
            val source = MovieBoxClient.resolveStreamUrl(videoId, episodeNumber)
            if (source != null && !isFinishing) {
                val mediaSource = mediaEngine.buildMediaSource(source)
                exoPlayer?.setMediaSource(mediaSource)
                exoPlayer?.prepare()
                exoPlayer?.play()
            } else {
                binding.playerLoading.visibility = View.GONE
                binding.tvPlayerError.visibility = View.VISIBLE
                binding.tvPlayerError.text = "Tautan streaming tidak tersedia atau dalam antrean decoding."
            }
        }
    }

    private fun saveCurrentProgress() {
        val player = exoPlayer ?: return
        val currentPos = player.currentPosition
        val duration = player.duration

        if (videoId.isNotBlank() && currentPos > 5000) {
            lifecycleScope.launch(Dispatchers.IO) {
                GOStreamDatabase.getInstance(this@PlayerActivity).saveProgress(
                    subjectId = videoId,
                    title = videoTitle.substringBefore(" - Episode"),
                    posterUrl = posterUrl,
                    episodeNumber = episodeNumber,
                    positionMs = currentPos,
                    durationMs = if (duration > 0) duration else 0
                )
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
        saveCurrentProgress()
    }

    override fun onDestroy() {
        saveCurrentProgress()
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
