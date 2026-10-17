package com.ats.galery.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.ats.galery.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** Sisi terpanjang gambar kerja (bitmap hasil edit). */
private const val MAX_EDIT_DIM = 2048

/** Ukuran minimum area potong (px layar). */
private const val MIN_CROP_PX = 72f

/**
 * Editor foto sederhana: putar, cermin, dan potong.
 * Hasil disimpan sebagai berkas baru di galeri, berkas asli tidak diubah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onSave: (Bitmap, (Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(item.id) { mutableStateOf(true) }
    var saving by remember(item.id) { mutableStateOf(false) }
    var cropMode by remember(item.id) { mutableStateOf(false) }
    var crop by remember(item.id) { mutableStateOf<Rect?>(null) }
    var container by remember { mutableStateOf(IntSize.Zero) }

    suspend fun reload() {
        loading = true
        working = withContext(Dispatchers.IO) { loadEditableBitmap(context, Uri.parse(item.uri)) }
        loading = false
        crop = null
        cropMode = false
    }

    LaunchedEffect(item.id) { reload() }

    fun transform(matrix: Matrix) {
        val src = working ?: return
        working = runCatching {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }.getOrNull() ?: src
        crop = null
    }

    val bitmap = working
    val imageRect = remember(container, bitmap) { bitmap?.let { fitRect(container, it.width, it.height) } }

    fun applyCrop() {
        val src = bitmap ?: return
        val rect = crop ?: return
        val area = imageRect ?: return
        val scale = (area.width / src.width).takeIf { it > 0f } ?: return
        val x = ((rect.left - area.left) / scale).roundToInt().coerceIn(0, src.width - 1)
        val y = ((rect.top - area.top) / scale).roundToInt().coerceIn(0, src.height - 1)
        val w = (rect.width / scale).roundToInt().coerceIn(1, src.width - x)
        val h = (rect.height / scale).roundToInt().coerceIn(1, src.height - y)
        working = runCatching { Bitmap.createBitmap(src, x, y, w, h) }.getOrNull() ?: src
        cropMode = false
        crop = null
    }

    fun save() {
        val src = working ?: return
        saving = true
        onSave(src) { ok ->
            saving = false
            Toast.makeText(
                context,
                if (ok) "Foto tersimpan ke galeri" else "Gagal menyimpan foto",
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Edit foto", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(onClick = { save() }, enabled = bitmap != null && !saving) {
                        Text(if (saving) "Menyimpan…" else "Simpan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { container = it },
            ) {
                if (bitmap != null) {
                    val image = remember(bitmap) { bitmap.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )

                    val area = imageRect
                    if (cropMode && area != null) {
                        val current = crop ?: area
                        CropOverlay(
                            bounds = area,
                            rect = current,
                            onRectChange = { crop = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Surface(color = MaterialTheme.colorScheme.surface) {
                if (cropMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { cropMode = false; crop = null }) { Text("Batal") }
                        Text(
                            text = "Tarik sudut untuk memotong",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { applyCrop() }) { Text("Terapkan") }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditAction("Putar kiri", { Icon(Icons.Rounded.RotateLeft, null) }) {
                            transform(Matrix().apply { postRotate(-90f) })
                        }
                        EditAction("Putar kanan", { Icon(Icons.Rounded.RotateRight, null) }) {
                            transform(Matrix().apply { postRotate(90f) })
                        }
                        EditAction("Cermin mendatar", { Icon(Icons.Rounded.Flip, null) }) {
                            transform(Matrix().apply { postScale(-1f, 1f) })
                        }
                        EditAction("Cermin tegak", { Icon(Icons.Rounded.SwapVert, null) }) {
                            transform(Matrix().apply { postScale(1f, -1f) })
                        }
                        EditAction("Potong", { Icon(Icons.Rounded.Crop, null) }) {
                            crop = imageRect
                            cropMode = true
                        }
                        EditAction("Muat ulang", { Icon(Icons.Rounded.Refresh, null) }) {
                            scope.launch { reload() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { icon() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- Area potong ------------------------------------------------------------

private enum class CropHandle { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** Overlay potong: tarik sudut untuk mengubah ukuran, tarik bagian dalam untuk menggeser. */
@Composable
private fun CropOverlay(
    bounds: Rect,
    rect: Rect,
    onRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    var handle by remember { mutableStateOf(CropHandle.NONE) }

    Canvas(
        modifier = modifier.pointerInput(bounds) {
            val touch = 48.dp.toPx()
            detectDragGestures(
                onDragStart = { position -> handle = pickHandle(position, rect, touch) },
                onDragEnd = { handle = CropHandle.NONE },
                onDragCancel = { handle = CropHandle.NONE },
                onDrag = { change, drag ->
                    change.consume()
                    onRectChange(moveRect(rect, handle, drag, bounds, MIN_CROP_PX))
                },
            )
        },
    ) {
        val scrim = Color.Black.copy(alpha = 0.55f)
        // Bagian luar area potong digelapkan.
        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, rect.top))
        drawRect(
            scrim,
            topLeft = Offset(0f, rect.bottom),
            size = Size(size.width, (size.height - rect.bottom).coerceAtLeast(0f)),
        )
        drawRect(
            scrim,
            topLeft = Offset(0f, rect.top),
            size = Size(rect.left, rect.height),
        )
        drawRect(
            scrim,
            topLeft = Offset(rect.right, rect.top),
            size = Size((size.width - rect.right).coerceAtLeast(0f), rect.height),
        )

        // Garis bantu tiga bagian.
        val grid = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            val x = rect.left + rect.width * i / 3f
            val y = rect.top + rect.height * i / 3f
            drawLine(grid, Offset(x, rect.top), Offset(x, rect.bottom), strokeWidth = 1f)
            drawLine(grid, Offset(rect.left, y), Offset(rect.right, y), strokeWidth = 1f)
        }

        drawRect(
            color = Color.White,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Pegangan sudut.
        val h = 16.dp.toPx() / 2f
        val bar = 3.dp.toPx()
        listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { corner ->
            drawLine(
                Color.White,
                Offset(corner.x - h, corner.y),
                Offset(corner.x + h, corner.y),
                strokeWidth = bar,
            )
            drawLine(
                Color.White,
                Offset(corner.x, corner.y - h),
                Offset(corner.x, corner.y + h),
                strokeWidth = bar,
            )
        }
    }
}

private fun pickHandle(position: Offset, rect: Rect, touch: Float): CropHandle {
    val corners = listOf(
        CropHandle.TOP_LEFT to rect.topLeft,
        CropHandle.TOP_RIGHT to rect.topRight,
        CropHandle.BOTTOM_LEFT to rect.bottomLeft,
        CropHandle.BOTTOM_RIGHT to rect.bottomRight,
    )
    corners.forEach { (handle, corner) ->
        if ((position - corner).getDistance() <= touch) return handle
    }
    return if (rect.contains(position)) CropHandle.MOVE else CropHandle.NONE
}

private fun moveRect(rect: Rect, handle: CropHandle, drag: Offset, bounds: Rect, minSize: Float): Rect {
    if (handle == CropHandle.NONE) return rect
    if (handle == CropHandle.MOVE) {
        val dx = drag.x.coerceIn(bounds.left - rect.left, bounds.right - rect.right)
        val dy = drag.y.coerceIn(bounds.top - rect.top, bounds.bottom - rect.bottom)
        return Rect(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
    }

    var left = rect.left
    var top = rect.top
    var right = rect.right
    var bottom = rect.bottom
    when (handle) {
        CropHandle.TOP_LEFT -> { left += drag.x; top += drag.y }
        CropHandle.TOP_RIGHT -> { right += drag.x; top += drag.y }
        CropHandle.BOTTOM_LEFT -> { left += drag.x; bottom += drag.y }
        CropHandle.BOTTOM_RIGHT -> { right += drag.x; bottom += drag.y }
        else -> Unit
    }
    left = left.coerceIn(bounds.left, right - minSize)
    top = top.coerceIn(bounds.top, bottom - minSize)
    right = right.coerceIn(left + minSize, bounds.right)
    bottom = bottom.coerceIn(top + minSize, bounds.bottom)
    return Rect(left, top, right, bottom)
}

// ---- Bantuan gambar ---------------------------------------------------------

/** Persegi tempat gambar digambar dengan ContentScale.Fit di dalam [container]. */
private fun fitRect(container: IntSize, width: Int, height: Int): Rect {
    if (container.width <= 0 || container.height <= 0 || width <= 0 || height <= 0) return Rect.Zero
    val scale = min(container.width / width.toFloat(), container.height / height.toFloat())
    val w = width * scale
    val h = height * scale
    val left = (container.width - w) / 2f
    val top = (container.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

/** Memuat gambar untuk diedit: diperkecil secukupnya + orientasi EXIF diterapkan. */
private fun loadEditableBitmap(context: Context, uri: Uri, maxDim: Int = MAX_EDIT_DIM): Bitmap? =
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@runCatching null

        val orientation = resolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        applyOrientation(decoded, orientation)
    }.getOrNull()

private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }
    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrDefault(bitmap)
}
