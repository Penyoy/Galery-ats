package com.ats.galery.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import com.ats.galery.data.MediaItem
import com.ats.galery.data.ViewerSpec
import com.ats.galery.media.MediaUiState
import com.ats.galery.ui.components.EmptyState
import com.ats.galery.ui.components.MediaGrid

/** Tab "Foto": seluruh foto & video. */
@Composable
fun GalleryTab(
    ui: MediaUiState,
    contentPadding: PaddingValues,
    onOpen: (spec: String, order: List<MediaItem>, index: Int) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
) {
    if (ui.media.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.PhotoLibrary,
            title = "Belum ada media",
            subtitle = "Foto dan video di perangkat akan muncul di sini.",
        )
    } else {
        MediaGrid(
            items = ui.media,
            settings = ui.settings,
            favorites = ui.favorites,
            contentPadding = contentPadding,
            onOpen = { order, index -> onOpen(ViewerSpec.ALL, order, index) },
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier,
        )
    }
}

/** Tab "Favorit". */
@Composable
fun FavoritesTab(
    ui: MediaUiState,
    contentPadding: PaddingValues,
    onOpen: (spec: String, order: List<MediaItem>, index: Int) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
) {
    val items = ui.favoritesList()
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.StarBorder,
            title = "Belum ada favorit",
            subtitle = "Tekan lama sebuah item untuk menandainya sebagai favorit.",
        )
    } else {
        MediaGrid(
            items = items,
            settings = ui.settings,
            favorites = ui.favorites,
            contentPadding = contentPadding,
            onOpen = { order, index -> onOpen(ViewerSpec.FAVORITES, order, index) },
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier,
        )
    }
}
