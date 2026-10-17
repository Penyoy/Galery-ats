package com.ats.galery.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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

/**
 * Satu kartu media di grid. Mendukung animasi masuk berjenjang, efek tekan,
 * badge durasi video, dan penanda favorit.
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
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val animated = entranceDurationMs > 0

    var appeared by remember(item.id) { mutableStateOf(!animated) }
    LaunchedEffect(item.id) {
        if (animated && !appeared) {
            delay(staggerUnitMs * min(staggerIndex, 14))
            appeared = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(entranceDurationMs.coerceAtLeast(1), easing = FastOutSlowInEasing),
        label = "cardAlpha",
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (appeared) 1f else when (entrance) {
            GridEntrance.ZOOM -> 0.84f
            GridEntrance.CASCADE -> 0.94f
            GridEntrance.FADE -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "cardScale",
    )
    val entranceY by animateFloatAsState(
        targetValue = if (appeared) 0f else if (entrance == GridEntrance.CASCADE) 42f else 0f,
        animationSpec = tween(entranceDurationMs.coerceAtLeast(1), easing = FastOutSlowInEasing),
        label = "cardY",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "pressScale",
    )

    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    Box(
        modifier = Modifier
            .aspectRatio(ratioModifier(item, ratio))
            .graphicsLayer {
                scaleX = entranceScale * pressScale
                scaleY = entranceScale * pressScale
                translationY = entranceY
                this.alpha = alpha
            }
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
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(animated)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (item.isVideo) {
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

            if (showDurationBadge && item.durationMs > 0L) {
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
