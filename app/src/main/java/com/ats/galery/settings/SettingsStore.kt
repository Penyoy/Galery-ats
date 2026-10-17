package com.ats.galery.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ats.galery.data.MediaSort
import com.ats.galery.data.ThumbRatio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "galery_settings")

/**
 * Penyimpanan preferensi (DataStore) + daftar favorit.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = intPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val SPEED = floatPreferencesKey("animation_speed")
        val ENTRANCE = intPreferencesKey("grid_entrance")
        val COLUMNS = intPreferencesKey("columns")
        val CORNER = intPreferencesKey("corner_radius")
        val SPACING = intPreferencesKey("spacing")
        val RATIO = intPreferencesKey("thumb_ratio")
        val SORT = intPreferencesKey("sort")
        val DURATION_BADGE = booleanPreferencesKey("duration_badge")
        val FAV_STAR = booleanPreferencesKey("fav_star")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val FAVORITES = stringSetPreferencesKey("favorites")
    }

    val settings: Flow<Settings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            Settings(
                themeMode = ThemeMode.entries.getOrElse(p[Keys.THEME] ?: 0) { ThemeMode.SYSTEM },
                dynamicColor = p[Keys.DYNAMIC] ?: false,
                animationsEnabled = p[Keys.ANIMATIONS] ?: true,
                animationSpeed = (p[Keys.SPEED] ?: 1f).coerceIn(0.5f, 2f),
                gridEntrance = GridEntrance.entries.getOrElse(p[Keys.ENTRANCE] ?: GridEntrance.CASCADE.ordinal) { GridEntrance.CASCADE },
                columns = (p[Keys.COLUMNS] ?: 3).coerceIn(2, 5),
                cornerRadiusDp = (p[Keys.CORNER] ?: 12).coerceIn(0, 28),
                spacingDp = (p[Keys.SPACING] ?: 4).coerceIn(0, 14),
                thumbRatio = ThumbRatio.entries.getOrElse(p[Keys.RATIO] ?: 0) { ThumbRatio.SQUARE },
                sort = MediaSort.entries.getOrElse(p[Keys.SORT] ?: 0) { MediaSort.DATE_DESC },
                showDurationBadge = p[Keys.DURATION_BADGE] ?: true,
                showFavoriteStar = p[Keys.FAV_STAR] ?: true,
                autoPip = p[Keys.AUTO_PIP] ?: true,
                confirmDelete = p[Keys.CONFIRM_DELETE] ?: true,
            )
        }

    val favorites: Flow<Set<Long>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            (p[Keys.FAVORITES] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
        }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.ordinal }

    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = value }

    suspend fun setAnimationsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.ANIMATIONS] = value }

    suspend fun setAnimationSpeed(value: Float) = context.dataStore.edit { it[Keys.SPEED] = value.coerceIn(0.5f, 2f) }

    suspend fun setGridEntrance(value: GridEntrance) = context.dataStore.edit { it[Keys.ENTRANCE] = value.ordinal }

    suspend fun setColumns(value: Int) = context.dataStore.edit { it[Keys.COLUMNS] = value.coerceIn(2, 5) }

    suspend fun setCornerRadius(value: Int) = context.dataStore.edit { it[Keys.CORNER] = value.coerceIn(0, 28) }

    suspend fun setSpacing(value: Int) = context.dataStore.edit { it[Keys.SPACING] = value.coerceIn(0, 14) }

    suspend fun setThumbRatio(value: ThumbRatio) = context.dataStore.edit { it[Keys.RATIO] = value.ordinal }

    suspend fun setSort(value: MediaSort) = context.dataStore.edit { it[Keys.SORT] = value.ordinal }

    suspend fun setShowDurationBadge(value: Boolean) = context.dataStore.edit { it[Keys.DURATION_BADGE] = value }

    suspend fun setShowFavoriteStar(value: Boolean) = context.dataStore.edit { it[Keys.FAV_STAR] = value }

    suspend fun setAutoPip(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_PIP] = value }

    suspend fun setConfirmDelete(value: Boolean) = context.dataStore.edit { it[Keys.CONFIRM_DELETE] = value }

    suspend fun toggleFavorite(id: Long) = context.dataStore.edit { p ->
        val current = p[Keys.FAVORITES] ?: emptySet()
        val key = id.toString()
        p[Keys.FAVORITES] = if (current.contains(key)) current - key else current + key
    }

    suspend fun resetSettings() = context.dataStore.edit { p ->
        val favorites = p[Keys.FAVORITES]
        p.clear()
        if (favorites != null) p[Keys.FAVORITES] = favorites
    }
}
