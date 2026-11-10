package com.gostream.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.gostream.app.R
import com.gostream.app.data.model.EpisodeItem
import com.gostream.app.data.model.VideoItem
import com.gostream.app.databinding.ActivityDetailBinding
import com.gostream.app.ui.adapter.EpisodeAdapter

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var episodeAdapter: EpisodeAdapter

    private var videoItem: VideoItem? = null
    private var modelId: String = "anichin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoItem = intent.getSerializableExtra("extra_video") as? VideoItem
        modelId = intent.getStringExtra("extra_model_id") ?: "anichin"

        setupViews()
        setupEpisodes()
    }

    private fun setupViews() {
        val video = videoItem ?: return
        binding.tvDetailTitle.text = video.title
        binding.tvDetailProvider.text = "Provider: ${modelId.uppercase()}"
        binding.tvDetailOverview.text = video.overview ?: "Sinopsis belum tersedia."

        binding.ivDetailPoster.load(video.posterUrl) {
            crossfade(true)
            placeholder(R.drawable.bg_card_focus)
        }

        binding.btnPlayDirect.setOnClickListener {
            playEpisode("1", "Episode 1")
        }
    }

    private fun setupEpisodes() {
        episodeAdapter = EpisodeAdapter { ep ->
            playEpisode(ep.id, "Episode ${ep.episodeNumber}")
        }
        binding.rvEpisodes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEpisodes.adapter = episodeAdapter

        // Synthetic episode list or detail episodes
        val episodes = (1..12).map {
            EpisodeItem(id = "$it", episodeNumber = it, title = "Episode $it")
        }
        episodeAdapter.submitList(episodes)
    }

    private fun playEpisode(episodeId: String, title: String) {
        val video = videoItem ?: return
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("extra_model_id", modelId)
            putExtra("extra_episode_id", episodeId)
            putExtra("extra_video_id", video.id)
            putExtra("extra_title", "${video.title} - $title")
        }
        startActivity(intent)
    }
}
