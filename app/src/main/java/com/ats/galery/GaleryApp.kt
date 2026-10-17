package com.ats.galery

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Galery ATS — All The Storage.
 *
 * Aplikasi galeri murni lokal: tanpa izin internet, tanpa jaringan.
 */
class GaleryApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                // Thumbnail video lewat MediaMetadataRetriever lokal.
                add(VideoFrameDecoder.Factory())
                // Animasi GIF/WebP: ImageDecoder di Android 9+, GifDecoder di bawahnya.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.22)
                    .build()
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
