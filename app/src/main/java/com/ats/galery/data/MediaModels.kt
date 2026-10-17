package com.ats.galery.data

/** Satu item media (foto atau video) dari MediaStore. */
data class MediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val albumId: Long,
    val album: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val dateMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
}

/** Album = folder di perangkat. */
data class Album(
    val id: Long,
    val name: String,
    val count: Int,
    val cover: MediaItem?,
)

enum class MediaSort {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC
}

enum class ThumbRatio {
    SQUARE, PORTRAIT, ORIGINAL
}

/**
 * Mengelompokkan item per hari. Urutan hari mengikuti arah pengurutan terbaru
 * atau terlama, sedangkan urutan di dalam hari tetap seperti sumbernya.
 */
fun List<MediaItem>.groupByDay(sort: MediaSort): List<Pair<Long, List<MediaItem>>> {
    if (isEmpty()) return emptyList()
    val grouped = groupBy { com.ats.galery.util.startOfDay(it.dateMillis) }
    val dayOrder = if (sort == MediaSort.DATE_ASC) {
        grouped.keys.sorted()
    } else {
        grouped.keys.sortedDescending()
    }
    return dayOrder.map { day -> day to grouped.getValue(day) }
}

/** Urutan item yang sama persis dengan tampilan grid (dipakai viewer). */
fun List<MediaItem>.viewerOrder(sort: MediaSort): List<MediaItem> =
    groupByDay(sort).flatMap { it.second }

/** Identifier koleksi yang dibuka di viewer. */
object ViewerSpec {
    const val ALL = "all"
    const val FAVORITES = "favorites"
    const val ALBUM_PREFIX = "album-"
    fun album(id: Long) = "$ALBUM_PREFIX$id"
}
