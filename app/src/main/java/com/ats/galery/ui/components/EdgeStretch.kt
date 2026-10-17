package com.ats.galery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.abs

/** Seberapa besar gerakan jari diteruskan menjadi regangan. */
private const val PULL_FACTOR = 0.45f

/** Batas maksimum regangan (px dihitung dari dp berikut). */
private const val MAX_STRETCH_DP = 64f

/**
 * Efek "melar" ala pegas: ketika gulir sudah mentok di ujung atas/bawah, sisa
 * tarikan diserap menjadi regangan konten, lalu kembali dengan animasi pegas.
 * Tidak memakai subcomposition dan hanya menulis state di fase gambar, jadi
 * tidak menambah beban komposisi.
 */
@Composable
fun EdgeStretch(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val stretch = remember { Animatable(0f) }
    val maxStretch = with(LocalDensity.current) { MAX_STRETCH_DP.dp.toPx() }

    val connection = remember(enabled, maxStretch) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero
                val current = stretch.value
                // Saat pengguna menggulir balik, kembalikan regangan lebih dulu.
                if (current == 0f || available.y * current >= 0f) return Offset.Zero
                val target = current + available.y * PULL_FACTOR
                val clamped = if (current > 0f) target.coerceAtLeast(0f) else target.coerceAtMost(0f)
                stretch.value = clamped
                return Offset(0f, (clamped - current) / PULL_FACTOR)
            }

            override suspend fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!enabled || source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y == 0f) return Offset.Zero
                val next = (stretch.value + available.y * PULL_FACTOR)
                    .coerceIn(-maxStretch, maxStretch)
                stretch.snapTo(next)
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (stretch.value != 0f) {
                    stretch.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
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
                    val px = stretch.value
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
