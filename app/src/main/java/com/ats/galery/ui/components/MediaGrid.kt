package com.ats.galery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ats.galery.data.MediaItem
import com.ats.galery.data.groupByDay
import com.ats.galery.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/** Lebar minimum satu kartu saat layar lebar (tablet/lanskap). */
private val WIDE_CELL_MIN = 132.dp

/** Ambang lebar layar yang dianggap "lebar". */
private val WIDE_SCREEN = 600.dp

/** Jeda sebelum pratinjau video mulai, agar tidak berjalan saat masih menggulir. */
private const val PREVIEW_DELAY_MS = 280L

/**
 * Grid media berkelompok per hari dengan animasi masuk berjenjang.
 * [onOpen] menerima daftar terurut yang sama dengan tampilan beserta indeksnya.
 *
 * Di layar lebar jumlah kolom menyesuaikan sendiri (fleksibel). Bila pratinjau
 * gerak aktif, hanya satu video yang dipratinjau: video pertama yang terlihat
 * setelah gulir benar-benar berhenti.
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

    val gridState = rememberLazyGridState()
    val videoIds = remember(items) { items.filter { it.isVideo }.map { it.id }.toSet() }
    var previewId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(gridState, videoIds, settings.animatedPreviews) {
        if (!settings.animatedPreviews || videoIds.isEmpty()) {
            previewId = null
            return@LaunchedEffect
        }
        snapshotFlow {
            if (gridState.isScrollInProgress) emptyList()
            else gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long }
        }
            .distinctUntilChanged()
            .collect { visible ->
                previewId = null
                val next = visible.firstOrNull { it in videoIds }
                if (next != null) {
                    delay(PREVIEW_DELAY_MS)
                    previewId = next
                }
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= WIDE_SCREEN) {
            GridCells.Adaptive(minSize = WIDE_CELL_MIN)
        } else {
            GridCells.Fixed(settings.columns)
        }

        LazyVerticalGrid(
            columns = columns,
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(settings.spacingDp.dp),
            verticalArrangement = Arrangement.spacedBy(settings.spacingDp.dp),
        ) {
            header?.invoke(this)

            var running = 0
            sections.forEach { (day, dayItems) ->
                // Nilai offset dikunci per-bagian. Tanpa ini, lambda item (yang baru
                // dieksekusi saat masuk viewport) akan membaca `running` yang sudah
                // bernilai total akhir sehingga item yang dibuka jadi salah.
                val sectionOffset = running
                item(key = "header-$day", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(text = com.ats.galery.util.formatDayLabel(day), count = dayItems.size)
                }
                itemsIndexed(dayItems, key = { _, item -> item.id }) { indexInSection, item ->
                    val globalIndex = sectionOffset + indexInSection
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
                        animatedPreview = settings.animatedPreviews,
                        isPreview = item.id == previewId,
                        onOpen = { onOpen(ordered, globalIndex) },
                        onToggleFavorite = { onToggleFavorite(item) },
                    )
                }
                running += dayItems.size
            }
        }
    }
}
