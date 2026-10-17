package com.ats.galery.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Seberapa besar gerakan jari diteruskan menjadi regangan. */
private const val PULL_FACTOR = 0.45f

/** Batas maksimum regangan (dalam dp). */
private const val MAX_STRETCH_DP = 64f

/**
 * Efek "melar" ala pegas: ketika gulir sudah mentok di ujung atas/bawah, sisa
 * tarikan diserap menjadi regangan konten, lalu kembali dengan animasi pegas.
 * Tidak memakai subcomposition dan hanya menulis state di fase gambar.
 */
@Composable
fun EdgeStretch(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val stretch = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val maxStretch = with(LocalDensity.current) { MAX_STRETCH_DP.dp.toPx() }

    val connection = remember(enabled, maxStretch, scope) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero
                val current = stretch.floatValue
                // Saat pengguna menggulir balik, kembalikan regangan lebih dulu.
                if (current == 0f || available.y * current >= 0f) return Offset.Zero
                val target = current + available.y * PULL_FACTOR
                val clamped = if (current > 0f) target.coerceAtLeast(0f) else target.coerceAtMost(0f)
                stretch.floatValue = clamped
                return Offset(0f, (clamped - current) / PULL_FACTOR)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y == 0f) return Offset.Zero
                stretch.floatValue = (stretch.floatValue + available.y * PULL_FACTOR)
                    .coerceIn(-maxStretch, maxStretch)
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val start = stretch.floatValue
                if (start != 0f) {
                    scope.launch {
                        animate(
                            initialValue = start,
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) { value, _ -> stretch.floatValue = value }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.nestedScroll(connection)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val px = stretch.floatValue
                    if (px != 0f) {
                        val f = (abs(px) / maxStretch).coerceIn(0f, 1f)
                        translationY = px
                        // Titik tumpu di ujung yang ditarik agar terlihat meregang.
                        transformOrigin = TransformOrigin(0.5f, if (px > 0f) 0f else 1f)
                        scaleY = 1f + 0.05f * f
                        scaleX = 1f - 0.02f * f
                    }
                },
        ) {
            content()
        }
    }
}
