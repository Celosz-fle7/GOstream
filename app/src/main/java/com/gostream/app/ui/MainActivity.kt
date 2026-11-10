package com.gostream.app.ui

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.gostream.app.R
import com.gostream.app.data.api.CineFlowApiClient
import com.gostream.app.data.model.ProviderItem
import com.gostream.app.data.model.VideoItem
import com.gostream.app.databinding.ActivityMainBinding
import com.gostream.app.ui.adapter.ProviderAdapter
import com.gostream.app.ui.adapter.TvVideoAdapter
import com.gostream.app.ui.adapter.VideoAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var providerAdapter: ProviderAdapter
    private lateinit var mobileVideoAdapter: VideoAdapter
    private lateinit var tvVideoAdapter: TvVideoAdapter

    private var currentModelId: String = "anichin"
    private var isTvMode: Boolean = false
    private var currentFocusedVideo: VideoItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detectDeviceMode()
        setupViews()
        loadProviders()
    }

    private fun detectDeviceMode() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTelevision = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val isLargeLandscape = resources.configuration.screenWidthDp >= 900

        isTvMode = isTelevision || isLargeLandscape
    }

    private fun setupViews() {
        if (isTvMode) {
            // ANDROID TV MODE: Ambient Hero Backdrop + Horizontal Shelf
            binding.ivAmbientBackdrop.visibility = View.VISIBLE
            binding.vAmbientGradient.visibility = View.VISIBLE
            binding.layoutTvContainer.visibility = View.VISIBLE
            binding.layoutMobileContainer.visibility = View.GONE

            providerAdapter = ProviderAdapter { selectedProvider ->
                currentModelId = selectedProvider.id
                loadVideos(selectedProvider.id)
            }
            binding.rvTvProviders.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvTvProviders.adapter = providerAdapter

            tvVideoAdapter = TvVideoAdapter(
                onItemClick = { video -> openDetail(video) },
                onItemFocused = { video -> updateTvHeroSection(video) }
            )
            binding.rvTvVideos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvTvVideos.adapter = tvVideoAdapter

            binding.btnHeroPlay.setOnClickListener {
                currentFocusedVideo?.let { openPlayer(it) }
            }
            binding.btnHeroDetail.setOnClickListener {
                currentFocusedVideo?.let { openDetail(it) }
            }

        } else {
            // MOBILE MODE: 3-Grid System
            binding.ivAmbientBackdrop.visibility = View.GONE
            binding.vAmbientGradient.visibility = View.GONE
            binding.layoutTvContainer.visibility = View.GONE
            binding.layoutMobileContainer.visibility = View.VISIBLE

            providerAdapter = ProviderAdapter { selectedProvider ->
                currentModelId = selectedProvider.id
                loadVideos(selectedProvider.id)
            }
            binding.rvMobileProviders.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvMobileProviders.adapter = providerAdapter

            mobileVideoAdapter = VideoAdapter { video ->
                openDetail(video)
            }
            // Strict 3-grid layout for Mobile
            binding.rvMobileVideos.layoutManager = GridLayoutManager(this, 3)
            binding.rvMobileVideos.adapter = mobileVideoAdapter
        }
    }

    private fun updateTvHeroSection(video: VideoItem) {
        currentFocusedVideo = video

        binding.tvHeroTitle.text = video.title
        binding.tvHeroOverview.text = video.overview ?: "Sinopsis belum tersedia untuk judul ini."
        binding.tvHeroProviderBadge.text = currentModelId.uppercase()
        binding.tvHeroMeta.text = "${video.status ?: "Tersedia"} • Ultra HD • Audio Stereo"

        // Smooth crossfade ambient backdrop (Netflix / YouTube TV style)
        binding.ivAmbientBackdrop.load(video.posterUrl) {
            crossfade(300)
            placeholder(R.drawable.bg_ambient_gradient)
            error(R.drawable.bg_ambient_gradient)
        }
    }

    private fun openDetail(video: VideoItem) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra("extra_video", video)
            putExtra("extra_model_id", currentModelId)
        }
        startActivity(intent)
    }

    private fun openPlayer(video: VideoItem) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("extra_model_id", currentModelId)
            putExtra("extra_episode_id", "1")
            putExtra("extra_video_id", video.id)
            putExtra("extra_title", "${video.title} - Episode 1")
        }
        startActivity(intent)
    }

    private fun loadProviders() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val providers = CineFlowApiClient.getProviders()
            binding.progressBar.visibility = View.GONE

            if (providers.isNotEmpty()) {
                providerAdapter.submitList(providers)
                val defaultProvider = providers.first()
                currentModelId = defaultProvider.id
                loadVideos(defaultProvider.id)
            } else {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Gagal memuat provider gateway"
            }
        }
    }

    private fun loadVideos(modelId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            val videos = CineFlowApiClient.getVideos(modelId)
            binding.progressBar.visibility = View.GONE

            if (videos.isNotEmpty()) {
                if (isTvMode) {
                    tvVideoAdapter.submitList(videos)
                    // Auto-select first video for Hero preview
                    videos.firstOrNull()?.let { updateTvHeroSection(it) }
                } else {
                    mobileVideoAdapter.submitList(videos)
                }
            } else {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Belum ada konten untuk kategori ini"
            }
        }
    }
}
