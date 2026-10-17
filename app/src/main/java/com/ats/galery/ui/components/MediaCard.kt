package com.ats.galery.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ats.galery.data.MediaItem
import com.ats.galery.data.ThumbRatio
import com.ats.galery.settings.GridEntrance
import com.ats.galery.util.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.min

/** Item setelah indeks ini tampil langsung tanpa animasi masuk (hemat frame). */
private const val MAX_ANIMATED_INDEX = 24
private const val MAX_STAGGER_STEPS = 8

/**
 * Satu kartu media di grid.
 *
 * Optimasi: satu nilai animasi saja, permintaan gambar diingat (tidak dibuat
 * ulang tiap recomposition), dan layer grafis hanya dipasang selama animasi
 * masuk atau saat kartu ditekan — sehingga gulir tetap ringan.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    item: MediaItem,
    cornerRadiusDp: Int,
    ratio: ThumbRatio,
    showDurationBadge: Boolean,
    showFavoriteStar: Boolean,
    isFavorite: Boolean,
    entrance: GridEntrance,
    staggerIndex: Int,
    entranceDurationMs: Int,
    staggerUnitMs: Long,
    animatedPreview: Boolean,
    isPreview: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val animated = entranceDurationMs > 0 && staggerIndex < MAX_ANIMATED_INDEX

    // rememberSaveable: state dipertahankan saat item keluar viewport,
    // sehingga tidak ada animasi masuk berulang ketika pengguna scroll balik.
    var appeared by rememberSaveable(item.id) { mutableStateOf(!animated) }
    LaunchedEffect(item.id) {
        if (animated && !appeared) {
            delay(staggerUnitMs * min(staggerIndex, MAX_STAGGER_STEPS))
            appeared = true
        }
    }

    // Layer grafis hanya selama animasi masuk berjalan.
    var entranceDone by remember(item.id) { mutableStateOf(!animated || appeared) }
    LaunchedEffect(appeared, animated) {
        if (!animated) {
            entranceDone = true
        } else if (appeared) {
            delay(entranceDurationMs.toLong() + 60L)
            entranceDone = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = entranceDurationMs.coerceAtLeast(1),
            easing = LinearOutSlowInEasing,
        ),
        label = "entrance",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && animated) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press",
    )

    val startScale = when (entrance) {
        GridEntrance.ZOOM -> 0.88f
        GridEntrance.CASCADE -> 0.96f
        GridEntrance.FADE -> 1f
    }
    val startTranslationY = if (entrance == GridEntrance.CASCADE) 26f else 0f
    val layerActive = animated && (!entranceDone || pressed)

    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    val layerModifier = if (layerActive) {
        Modifier.graphicsLayer {
            val scale = (startScale + (1f - startScale) * if (entranceDone) 1f else progress) * pressScale
            scaleX = scale
            scaleY = scale
            translationY = if (entranceDone) 0f else (1f - progress) * startTranslationY
            alpha = if (entranceDone) 1f else progress
        }
    } else {
        Modifier
    }

    // Permintaan gambar diingat agar tidak dibangun ulang setiap recomposition.
    val request = remember(item.uri, animated) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(animated)
            .build()
    }

    Box(
        modifier = Modifier
            .aspectRatio(ratioModifier(item, ratio))
            .then(layerModifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = onToggleFavorite,
            ),
    ) {
        AsyncImage(
            model = request,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Pratinjau gerak: hanya satu video berjalan di seluruh grid.
        val previewing = isPreview && item.isVideo && animatedPreview
        if (previewing) {
            VideoPreview(uri = item.uri, modifier = Modifier.fillMaxSize())
        }

        if (item.isVideo && !previewing) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (item.isVideo && showDurationBadge && item.durationMs > 0L) {
            Text(
                text = formatDuration(item.durationMs),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }

        if (showFavoriteStar && isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Favorit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun ratioModifier(item: MediaItem, ratio: ThumbRatio): Float = when (ratio) {
    ThumbRatio.SQUARE -> 1f
    ThumbRatio.PORTRAIT -> 3f / 4f
    ThumbRatio.ORIGINAL -> item.aspectRatio.coerceIn(0.5f, 2.2f)
}
