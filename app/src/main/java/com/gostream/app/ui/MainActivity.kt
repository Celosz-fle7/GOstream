package com.gostream.app.ui

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gostream.app.data.api.CineFlowApiClient
import com.gostream.app.data.model.ProviderItem
import com.gostream.app.data.model.VideoItem
import com.gostream.app.databinding.ActivityMainBinding
import com.gostream.app.ui.adapter.ProviderAdapter
import com.gostream.app.ui.adapter.VideoAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var providerAdapter: ProviderAdapter
    private lateinit var videoAdapter: VideoAdapter

    private var currentModelId: String = "anichin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupResponsiveLayout()
        loadProviders()
    }

    private fun setupAdapters() {
        providerAdapter = ProviderAdapter { selectedProvider ->
            currentModelId = selectedProvider.id
            loadVideos(selectedProvider.id)
        }
        binding.rvProviders.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvProviders.adapter = providerAdapter

        videoAdapter = VideoAdapter { video ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("extra_video", video)
                putExtra("extra_model_id", currentModelId)
            }
            startActivity(intent)
        }
        binding.rvVideos.adapter = videoAdapter
    }

    private fun setupResponsiveLayout() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val spanCount = if (isTv) 5 else 3

        binding.rvVideos.layoutManager = GridLayoutManager(this, spanCount)
        binding.tvDeviceMode.text = if (isTv) "Mode: Android TV (D-Pad)" else "Mode: Android Mobile"
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
                videoAdapter.submitList(videos)
            } else {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Belum ada konten untuk kategori ini"
            }
        }
    }
}
