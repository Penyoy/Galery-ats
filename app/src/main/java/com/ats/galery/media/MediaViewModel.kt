package com.ats.galery.media

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ats.galery.data.Album
import com.ats.galery.data.MediaItem
import com.ats.galery.data.MediaRepository
import com.ats.galery.data.MediaSort
import com.ats.galery.data.ViewerSpec
import com.ats.galery.data.viewerOrder
import com.ats.galery.settings.GridEntrance
import com.ats.galery.settings.Settings
import com.ats.galery.settings.SettingsStore
import com.ats.galery.settings.ThemeMode
import com.ats.galery.data.ThumbRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Seluruh state yang dipakai UI. */
data class MediaUiState(
    val media: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val settings: Settings = Settings(),
    val loading: Boolean = true,
) {
    fun favoritesList(): List<MediaItem> = media.filter { it.id in favorites }

    fun albumItems(albumId: Long): List<MediaItem> = media.filter { it.albumId == albumId }

    /** Daftar item untuk sebuah koleksi viewer. */
    fun bySpec(spec: String): List<MediaItem> = when {
        spec == ViewerSpec.FAVORITES -> favoritesList().viewerOrder(settings.sort)
        spec.startsWith(ViewerSpec.ALBUM_PREFIX) ->
            albumItems(spec.removePrefix(ViewerSpec.ALBUM_PREFIX).toLongOrNull() ?: -1L)
                .viewerOrder(settings.sort)
        else -> media.viewerOrder(settings.sort)
    }

    fun isFavorite(id: Long): Boolean = id in favorites
}

private fun MediaSort.comparator(): Comparator<MediaItem> = when (this) {
    MediaSort.DATE_DESC -> compareByDescending { it.dateMillis }
    MediaSort.DATE_ASC -> compareBy { it.dateMillis }
    MediaSort.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    MediaSort.NAME_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
    MediaSort.SIZE_DESC -> compareByDescending { it.sizeBytes }
    MediaSort.SIZE_ASC -> compareBy { it.sizeBytes }
}

class MediaViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)
    private val store = SettingsStore(app)

    private val mediaFlow = MutableStateFlow<List<MediaItem>>(emptyList())
    private val loading = MutableStateFlow(true)

    private val settingsFlow = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val favoritesFlow = store.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val ui: StateFlow<MediaUiState> =
        combine(mediaFlow, settingsFlow, favoritesFlow, loading) { media, settings, favorites, isLoading ->
            val sorted = media.sortedWith(settings.sort.comparator())
            MediaUiState(
                media = sorted,
                albums = sorted
                    .groupBy { it.albumId }
                    .map { (id, list) ->
                        Album(
                            id = id,
                            name = list.first().album,
                            count = list.size,
                            cover = list.firstOrNull { !it.isVideo } ?: list.firstOrNull(),
                        )
                    }
                    .sortedBy { it.name.lowercase() },
                favorites = favorites,
                settings = settings,
                loading = isLoading,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            mediaFlow.value = runCatching { repository.loadAll() }.getOrDefault(emptyList())
            loading.value = false
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { store.toggleFavorite(id) }
    }

    // ---- Pengaturan ----------------------------------------------------------

    fun setThemeMode(value: ThemeMode) = launchStore { this.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) = launchStore { this.setDynamicColor(value) }
    fun setAnimationsEnabled(value: Boolean) = launchStore { this.setAnimationsEnabled(value) }
    fun setAnimationSpeed(value: Float) = launchStore { this.setAnimationSpeed(value) }
    fun setGridEntrance(value: GridEntrance) = launchStore { this.setGridEntrance(value) }
    fun setColumns(value: Int) = launchStore { this.setColumns(value) }
    fun setCornerRadius(value: Int) = launchStore { this.setCornerRadius(value) }
    fun setSpacing(value: Int) = launchStore { this.setSpacing(value) }
    fun setThumbRatio(value: ThumbRatio) = launchStore { this.setThumbRatio(value) }
    fun setSort(value: MediaSort) = launchStore { this.setSort(value) }
    fun setShowDurationBadge(value: Boolean) = launchStore { this.setShowDurationBadge(value) }
    fun setShowFavoriteStar(value: Boolean) = launchStore { this.setShowFavoriteStar(value) }
    fun setAutoPip(value: Boolean) = launchStore { this.setAutoPip(value) }
    fun setConfirmDelete(value: Boolean) = launchStore { this.setConfirmDelete(value) }
    fun resetSettings() = launchStore { this.resetSettings() }

    private fun launchStore(block: suspend SettingsStore.() -> Unit) {
        viewModelScope.launch { store.block() }
    }

    // ---- Hapus ---------------------------------------------------------------

    /** Intent permintaan hapus (Android 11+) yang harus diluncurkan dari UI. */
    fun buildDeleteRequest(items: List<MediaItem>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || items.isEmpty()) return null
        val uris = items.map { Uri.parse(it.uri) }
        return runCatching {
            MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, uris).intentSender
        }.getOrNull()
    }

    /** Hapus langsung untuk Android 8–10 (memakai izin tulis legacy). */
    fun deleteDirect(items: List<MediaItem>, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                items.forEach { item ->
                    runCatching {
                        getApplication<Application>().contentResolver.delete(Uri.parse(item.uri), null, null)
                    }
                }
            }
            refresh()
            onDone()
        }
    }
}
