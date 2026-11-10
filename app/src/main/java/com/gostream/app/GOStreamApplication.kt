package com.gostream.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class GOStreamApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    /**
     * Ultra-light Coil image caching policy to keep overall storage minimal (< 50MB)
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // Max 15% RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(15L * 1024 * 1024) // Cap image disk cache at 15 MB
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: GOStreamApplication
            private set
    }
}
