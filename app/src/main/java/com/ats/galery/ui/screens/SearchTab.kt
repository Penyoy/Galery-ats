package com.ats.galery.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ats.galery.data.MediaItem
import com.ats.galery.data.ViewerSpec
import com.ats.galery.media.MediaUiState
import com.ats.galery.ui.components.EmptyState
import com.ats.galery.ui.components.MediaGrid

/**
 * Tab "Cari": pencarian nama berkas dan nama album.
 * Kata kunci disimpan di ViewModel agar urutan hasil di viewer tetap sama.
 */
@Composable
fun SearchTab(
    ui: MediaUiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onOpen: (spec: String, order: List<MediaItem>, index: Int) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val results = remember(ui.media, ui.searchQuery, ui.settings.sort) { ui.searchResults() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        OutlinedTextField(
            value = ui.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Cari foto, video, atau album") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (ui.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Bersihkan")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                ui.searchQuery.isBlank() -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Cari di galeri",
                    subtitle = "Ketik nama berkas atau nama album untuk menemukan media.",
                )

                results.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Tidak ditemukan",
                    subtitle = "Tidak ada media yang cocok dengan \"${ui.searchQuery}\".",
                )

                else -> MediaGrid(
                    items = results,
                    settings = ui.settings,
                    favorites = ui.favorites,
                    contentPadding = PaddingValues(
                        start = 6.dp,
                        end = 6.dp,
                        bottom = contentPadding.calculateBottomPadding() + 6.dp,
                    ),
                    onOpen = { _, index -> onOpen(ViewerSpec.SEARCH, results, index) },
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
