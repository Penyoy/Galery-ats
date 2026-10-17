package com.ats.galery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ats.galery.data.MediaItem
import com.ats.galery.data.groupByDay
import com.ats.galery.settings.Settings

/**
 * Grid media berkelompok per hari dengan animasi masuk berjenjang.
 * [onOpen] menerima daftar terurut yang sama dengan tampilan beserta indeksnya.
 */
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    settings: Settings,
    favorites: Set<Long>,
    contentPadding: PaddingValues,
    onOpen: (order: List<MediaItem>, index: Int) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    header: (LazyGridScope.() -> Unit)? = null,
) {
    val sections = remember(items, settings.sort) { items.groupByDay(settings.sort) }
    val ordered = remember(sections) { sections.flatMap { it.second } }

    val durationMs = settings.duration(240)
    val staggerUnitMs = if (durationMs <= 0) 0L
    else (24f / settings.animationSpeed).toLong()

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(settings.spacingDp.dp),
        verticalArrangement = Arrangement.spacedBy(settings.spacingDp.dp),
    ) {
        header?.invoke(this)

        var running = 0
        sections.forEach { (day, dayItems) ->
            item(key = "header-$day", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(text = com.ats.galery.util.formatDayLabel(day), count = dayItems.size)
            }
            itemsIndexed(dayItems, key = { _, item -> item.id }) { indexInSection, item ->
                val globalIndex = running + indexInSection
                MediaCard(
                    item = item,
                    cornerRadiusDp = settings.cornerRadiusDp,
                    ratio = settings.thumbRatio,
                    showDurationBadge = settings.showDurationBadge,
                    showFavoriteStar = settings.showFavoriteStar,
                    isFavorite = item.id in favorites,
                    entrance = settings.gridEntrance,
                    staggerIndex = globalIndex,
                    entranceDurationMs = durationMs,
                    staggerUnitMs = staggerUnitMs,
                    onOpen = { onOpen(ordered, globalIndex) },
                    onToggleFavorite = { onToggleFavorite(item) },
                )
            }
            running += dayItems.size
        }
    }
}
