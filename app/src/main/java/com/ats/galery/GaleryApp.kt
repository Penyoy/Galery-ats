package com.ats.galery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache

/**
 * Galery ATS — All The Storage.
 *
 * Aplikasi galeri murni lokal: tanpa izin internet, tanpa jaringan.
 */
class GaleryApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                // Dukungan thumbnail untuk video lewat MediaMetadataRetriever lokal.
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("media_thumb"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()
}
