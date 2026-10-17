package com.ats.galery.settings

import com.ats.galery.data.MediaSort
import com.ats.galery.data.ThumbRatio

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class GridEntrance { FADE, ZOOM, CASCADE }

/** Semua preferensi tampilan aplikasi. */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val animationsEnabled: Boolean = true,
    /** 0.5x (lambat) .. 2.0x (cepat). */
    val animationSpeed: Float = 1f,
    val gridEntrance: GridEntrance = GridEntrance.CASCADE,
    val columns: Int = 3,
    val cornerRadiusDp: Int = 12,
    val spacingDp: Int = 4,
    val thumbRatio: ThumbRatio = ThumbRatio.SQUARE,
    val sort: MediaSort = MediaSort.DATE_DESC,
    val showDurationBadge: Boolean = true,
    val showFavoriteStar: Boolean = true,
    val autoPip: Boolean = true,
    val confirmDelete: Boolean = true,
) {
    /** Durasi animasi dasar (ms) setelah dikalikan kecepatan. */
    fun duration(base: Int): Int =
        if (!animationsEnabled) 0 else (base / animationSpeed).toInt().coerceIn(40, 900)
}
