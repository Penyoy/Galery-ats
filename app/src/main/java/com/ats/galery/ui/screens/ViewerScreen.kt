package com.ats.galery.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.ats.galery.MainActivity
import com.ats.galery.R
import com.ats.galery.data.MediaItem
import com.ats.galery.media.MediaUiState
import com.ats.galery.media.MediaViewModel
import com.ats.galery.pip.LocalIsInPip
import com.ats.galery.pip.Pip
import com.ats.galery.pip.PipActionSpec
import com.ats.galery.pip.PipActions
import com.ats.galery.pip.PipDelegate
import com.ats.galery.ui.components.ConfirmDeleteDialog
import com.ats.galery.ui.theme.ViewerBackdrop
import com.ats.galery.util.findActivity
import com.ats.galery.util.formatDuration
import com.ats.galery.util.formatFullDate
import com.ats.galery.util.formatSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Viewer layar penuh: geser antar item, cubit untuk zoom, putar video,
 * bagikan, hapus, dan masuk Picture-in-Picture.
 */
@Composable
fun ViewerScreen(
    spec: String,
    startIndex: Int,
    ui: MediaUiState,
    vm: MediaViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? MainActivity }
    val inPip = LocalIsInPip.current
    val scope = rememberCoroutineScope()

    val items = remember(spec, ui.media, ui.favorites, ui.settings.sort) { ui.bySpec(spec) }

    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )
    val currentItem = items.getOrNull(pagerState.currentPage)

    val zoomedPages = remember { mutableStateMapOf<Int, Boolean>() }
    val playingPages = remember { mutableStateMapOf<Int, Boolean>() }
    val players = remember { mutableStateMapOf<Int, ExoPlayer>() }

    val currentPlaying = playingPages[pagerState.currentPage] ?: false
    val currentPlayer = players[pagerState.currentPage]

    var chromeVisible by remember { mutableStateOf(true) }
    LaunchedEffect(pagerState.currentPage) { chromeVisible = true }
    LaunchedEffect(chromeVisible, currentPlaying, currentItem?.id) {
        if (chromeVisible && currentItem?.isVideo == true && currentPlaying) {
            delay(3000)
            chromeVisible = false
        }
    }
    val showChrome = chromeVisible && !inPip

    val animationMs = ui.settings.duration(200)

    // Sembunyikan system bars saat layar penuh (bukan saat PIP).
    DisposableEffect(inPip) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (!inPip && controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // ---- Hapus ---------------------------------------------------------------
    var pendingDelete by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.refresh()
    }

    fun performDelete(list: List<MediaItem>) {
        if (list.isEmpty()) return
        val sender = vm.buildDeleteRequest(list)
        if (sender != null) {
            deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
        } else {
            vm.deleteDirect(list) { }
        }
    }

    fun requestDelete(item: MediaItem) {
        if (ui.settings.confirmDelete) {
            pendingDelete = listOf(item)
            showDeleteDialog = true
        } else {
            performDelete(listOf(item))
        }
    }

    fun share(item: MediaItem) {
        val uri = Uri.parse(item.uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan")) }
    }

    // ---- PIP -----------------------------------------------------------------
    val itemsRef = rememberUpdatedState(items)
    val currentItemRef = rememberUpdatedState(currentItem)

    val pipDelegate = remember {
        object : PipDelegate {
            override fun onPipAction(id: String) {
                when (id) {
                    PipActions.TOGGLE -> players[pagerState.currentPage]?.let { player ->
                        if (player.isPlaying) player.pause() else player.play()
                    }
                    PipActions.PREV -> scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                    PipActions.NEXT -> scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(itemsRef.value.lastIndex)
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Pip.delegate = pipDelegate
        onDispose { if (Pip.delegate === pipDelegate) Pip.delegate = null }
    }

    val remoteActions = buildList {
        val item = currentItem ?: return@buildList
        if (item.isVideo) {
            add(
                PipActionSpec(
                    id = PipActions.TOGGLE,
                    iconRes = if (currentPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play,
                )
            )
        } else {
            if (pagerState.currentPage > 0) add(PipActionSpec(PipActions.PREV, R.drawable.ic_pip_prev))
            if (pagerState.currentPage < items.lastIndex) add(PipActionSpec(PipActions.NEXT, R.drawable.ic_pip_next))
        }
    }
    LaunchedEffect(remoteActions) { Pip.host?.setPipActions(remoteActions) }

    // Auto-PIP saat menekan Home.
    DisposableEffect(currentItem?.id, currentPlaying, ui.settings.autoPip, activity) {
        val autoPipItem = currentItemRef.value
            ?.takeIf { it.isVideo && ui.settings.autoPip && currentPlaying }
        val handler: (() -> Unit)? = autoPipItem?.let { media ->
            { Pip.host?.enterPip(aspectRatioOf(media)) }
        }
        activity?.onLeaveHint = handler
        onDispose { if (activity?.onLeaveHint === handler) activity?.onLeaveHint = null }
    }

    // ---- Posisi playback -----------------------------------------------------
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(currentPlayer, currentItem?.id) {
        val player = currentPlayer ?: return@LaunchedEffect
        while (isActive) {
            positionMs = player.currentPosition
            durationMs = if (player.duration > 0L) player.duration else currentItemRef.value?.durationMs ?: 0L
            delay(300)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBackdrop),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !(zoomedPages[pagerState.currentPage] ?: false),
            key = { page -> items.getOrNull(page)?.id ?: page },
        ) { page ->
            val item = items[page]
            val isCurrent = page == pagerState.currentPage
            if (item.isVideo) {
                VideoPage(
                    item = item,
                    isCurrent = isCurrent,
                    onPlayerReady = { player -> players[page] = player },
                    onPlayingChanged = { playing -> playingPages[page] = playing },
                    onPlayerReleased = { players.remove(page) },
                    onToggleChrome = { chromeVisible = !chromeVisible },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ZoomableImage(
                    item = item,
                    onZoomChanged = { zoomed -> zoomedPages[page] = zoomed },
                    onTap = { chromeVisible = !chromeVisible },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ---- Bar atas --------------------------------------------------------
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(animationMs.coerceAtLeast(1))),
            exit = fadeOut(tween(animationMs.coerceAtLeast(1))),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            val item = currentItem ?: return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatFullDate(item.dateMillis),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(onClick = { vm.toggleFavorite(item.id) }) {
                    Icon(
                        imageVector = if (ui.isFavorite(item.id)) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorit",
                        tint = if (ui.isFavorite(item.id)) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
                IconButton(onClick = { share(item) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Bagikan", tint = Color.White)
                }
                IconButton(onClick = { requestDelete(item) }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = Color.White)
                }
                IconButton(onClick = { Pip.host?.enterPip(aspectRatioOf(item)) }) {
                    Icon(
                        Icons.Rounded.PictureInPictureAlt,
                        contentDescription = "Mode PIP",
                        tint = Color.White,
                    )
                }
            }
        }

        // ---- Bar bawah -------------------------------------------------------
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(animationMs.coerceAtLeast(1))),
            exit = fadeOut(tween(animationMs.coerceAtLeast(1))),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val item = currentItem ?: return@AnimatedVisibility
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (item.isVideo && currentPlayer != null) {
                    var dragValue by remember { mutableFloatStateOf(-1f) }
                    val max = durationMs.coerceAtLeast(1L).toFloat()
                    val shown = (if (dragValue >= 0f) dragValue else positionMs.toFloat()).coerceIn(0f, max)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconButton(onClick = {
                            if (currentPlayer.isPlaying) currentPlayer.pause() else currentPlayer.play()
                        }) {
                            Icon(
                                imageVector = if (currentPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (currentPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = formatDuration(positionMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Slider(
                            value = shown,
                            onValueChange = { dragValue = it },
                            onValueChangeFinished = {
                                currentPlayer.seekTo(dragValue.toLong())
                                dragValue = -1f
                            },
                            valueRange = 0f..max,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatDuration(durationMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                } else {
                    Text(
                        text = "${formatFullDate(item.dateMillis)}  •  ${formatSize(item.sizeBytes)}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (item.width > 0 && item.height > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${item.width} × ${item.height}",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            count = pendingDelete.size,
            onConfirm = {
                showDeleteDialog = false
                performDelete(pendingDelete)
                pendingDelete = emptyList()
            },
            onDismiss = {
                showDeleteDialog = false
                pendingDelete = emptyList()
            },
        )
    }
}

/** Halaman video dengan ExoPlayer. */
@Composable
private fun VideoPage(
    item: MediaItem,
    isCurrent: Boolean,
    onPlayerReady: (ExoPlayer) -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    onPlayerReleased: () -> Unit,
    onToggleChrome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(item.id) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .build()
            .apply {
                setMediaItem(ExoMediaItem.fromUri(item.uri))
                prepare()
            }
    }

    LaunchedEffect(player) { onPlayerReady(player) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged(isPlaying)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            onPlayingChanged(false)
            onPlayerReleased()
        }
    }

    LaunchedEffect(isCurrent) {
        if (isCurrent) player.play() else player.pause()
    }

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onToggleChrome,
            ),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Gambar dengan cubit-zoom dan ketuk dua kali. */
@Composable
private fun ZoomableImage(
    item: MediaItem,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxScale = 6f
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(newOffset: Offset, newScale: Float): Offset {
        val maxX = (containerSize.width * (newScale - 1f)) / 2f
        val maxY = (containerSize.height * (newScale - 1f)) / 2f
        return Offset(
            newOffset.x.coerceIn(-maxX, maxX),
            newOffset.y.coerceIn(-maxY, maxY),
        )
    }

    LaunchedEffect(scale) { onZoomChanged(scale > 1.01f) }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(item.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, maxScale)
                    scale = newScale
                    offset = if (newScale <= 1f) Offset.Zero else clamp(offset + pan, newScale)
                }
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

private fun aspectRatioOf(item: MediaItem): Float = when {
    item.width > 0 && item.height > 0 -> item.aspectRatio
    item.isVideo -> 16f / 9f
    else -> 1f
}
