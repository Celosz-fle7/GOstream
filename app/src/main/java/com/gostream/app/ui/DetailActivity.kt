package com.gostream.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.gostream.app.R
import com.gostream.app.data.model.EpisodeItem
import com.gostream.app.data.model.VideoItem
import com.gostream.app.data.moviebox.MovieBoxClient
import com.gostream.app.databinding.ActivityDetailBinding
import com.gostream.app.ui.adapter.EpisodeAdapter
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var episodeAdapter: EpisodeAdapter

    private var videoItem: VideoItem? = null
    private var platform: String = "FILM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoItem = intent.getSerializableExtra("extra_video") as? VideoItem
        platform = intent.getStringExtra("extra_platform") ?: "FILM"

        setupViews()
        setupEpisodes()
    }

    private fun setupViews() {
        val video = videoItem ?: return
        binding.tvDetailTitle.text = video.title
        binding.tvDetailProvider.text = "Platform: $platform"
        binding.tvDetailOverview.text = if (!video.overview.isNullOrBlank()) video.overview else "Sinopsis belum tersedia untuk judul ini."

        binding.ivDetailPoster.load(video.posterUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_card_focus)
            error(R.drawable.bg_card_focus)
        }

        binding.btnPlayDirect.setOnClickListener {
            playEpisode(1, "Episode 1")
        }
    }

    private fun setupEpisodes() {
        val video = videoItem ?: return

        episodeAdapter = EpisodeAdapter { ep ->
            playEpisode(ep.episodeNumber, ep.title)
        }
        binding.rvEpisodes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEpisodes.adapter = episodeAdapter

        lifecycleScope.launch {
            val episodes = MovieBoxClient.fetchEpisodes(video.id)
            if (episodes.isNotEmpty()) {
                episodeAdapter.submitList(episodes)
            } else {
                // Default 1 episode if none returned
                episodeAdapter.submitList(listOf(EpisodeItem("1", 1, "Episode 1")))
            }
        }
    }

    private fun playEpisode(episodeNumber: Int, title: String) {
        val video = videoItem ?: return
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("extra_video_id", video.id)
            putExtra("extra_episode_num", episodeNumber)
            putExtra("extra_title", "${video.title} - $title")
            putExtra("extra_poster", video.posterUrl)
        }
        startActivity(intent)
    }
}
