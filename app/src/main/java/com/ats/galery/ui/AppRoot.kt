package com.ats.galery.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ats.galery.data.MediaItem
import com.ats.galery.data.ViewerSpec
import com.ats.galery.media.MediaUiState
import com.ats.galery.media.MediaViewModel
import com.ats.galery.ui.components.EdgeStretch
import com.ats.galery.ui.screens.AlbumScreen
import com.ats.galery.ui.screens.AlbumsTab
import com.ats.galery.ui.screens.FavoritesTab
import com.ats.galery.ui.screens.GalleryTab
import com.ats.galery.ui.screens.PermissionScreen
import com.ats.galery.ui.screens.PhotoEditorScreen
import com.ats.galery.ui.screens.SearchTab
import com.ats.galery.ui.screens.SettingsScreen
import com.ats.galery.ui.screens.ViewerScreen
import com.ats.galery.ui.theme.GaleryTheme
import com.ats.galery.util.MediaPermissions

private data class TabItem(val label: String, val icon: ImageVector)

/** Akar aplikasi: izin akses → tema → navigasi. */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val vm: MediaViewModel = viewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()

    var hasAccess by remember { mutableStateOf(MediaPermissions.hasAccess(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAccess = MediaPermissions.hasAccess(context)
        if (hasAccess) vm.refresh()
    }

    LifecycleResumeEffect(Unit) {
        val access = MediaPermissions.hasAccess(context)
        hasAccess = access
        if (access) vm.refresh()
        onPauseOrDispose { }
    }

    GaleryTheme(settings = ui.settings) {
        if (!hasAccess) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
            ) {
                PermissionScreen(
                    onRequest = { permissionLauncher.launch(MediaPermissions.requested()) },
                    onOpenAppSettings = { openAppSettings(context) },
                )
            }
        } else {
            AppNav(ui = ui, vm = vm)
        }
    }
}

@Composable
private fun AppNav(ui: MediaUiState, vm: MediaViewModel) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "main",
        enterTransition = { fadeIn(tween(ui.settings.duration(220).coerceAtLeast(1))) },
        exitTransition = { fadeOut(tween(ui.settings.duration(150).coerceAtLeast(1))) },
        popEnterTransition = { fadeIn(tween(ui.settings.duration(150).coerceAtLeast(1))) },
        popExitTransition = { fadeOut(tween(ui.settings.duration(150).coerceAtLeast(1))) },
    ) {
        composable("main") {
            MainScaffold(
                ui = ui,
                vm = vm,
                onOpenViewer = { spec, _, index -> nav.navigate("viewer/$spec/$index") },
                onOpenAlbum = { albumId -> nav.navigate("album/$albumId") },
            )
        }
        composable(
            route = "album/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val albumId = entry.arguments?.getLong("id") ?: -1L
            AlbumScreen(
                albumId = albumId,
                ui = ui,
                onBack = { nav.popBackStack() },
                onOpen = { spec, _, index -> nav.navigate("viewer/$spec/$index") },
                onToggleFavorite = { vm.toggleFavorite(it.id) },
            )
        }
        composable(
            route = "viewer/{spec}/{index}",
            arguments = listOf(
                navArgument("spec") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType },
            ),
        ) { entry ->
            ViewerScreen(
                spec = entry.arguments?.getString("spec") ?: ViewerSpec.ALL,
                startIndex = entry.arguments?.getInt("index") ?: 0,
                ui = ui,
                vm = vm,
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("edit/$id") },
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: -1L
            val item = ui.media.firstOrNull { it.id == id }
            if (item == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                PhotoEditorScreen(
                    item = item,
                    onBack = { nav.popBackStack() },
                    onSave = { bitmap, onResult -> vm.saveEditedImage(item.name, bitmap, onResult) },
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(
    ui: MediaUiState,
    vm: MediaViewModel,
    onOpenViewer: (spec: String, order: List<MediaItem>, index: Int) -> Unit,
    onOpenAlbum: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            TabItem("Foto", Icons.Rounded.PhotoLibrary),
            TabItem("Album", Icons.Rounded.PhotoAlbum),
            TabItem("Favorit", Icons.Rounded.Star),
            TabItem("Cari", Icons.Rounded.Search),
            TabItem("Setelan", Icons.Rounded.SettingsIcon),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = index == tab,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        val contentPadding = PaddingValues(
            start = 6.dp,
            end = 6.dp,
            top = inner.calculateTopPadding() + 2.dp,
            bottom = inner.calculateBottomPadding() + 18.dp,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // Efek "melar" saat gulir mentok di ujung.
            EdgeStretch(
                enabled = ui.settings.animationsEnabled,
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val duration = ui.settings.duration(240).coerceAtLeast(1)
                        val exitDuration = ui.settings.duration(170).coerceAtLeast(1)
                        val sign = if (forward) 1 else -1
                        // Geser sepertiga lebar layar: jelas terlihat "sliding".
                        (
                            fadeIn(tween(duration)) +
                                slideInHorizontally(tween(duration)) { it / 3 * sign }
                            ) togetherWith (
                            fadeOut(tween(exitDuration)) +
                                slideOutHorizontally(tween(exitDuration)) { -it / 3 * sign }
                            )
                    },
                    label = "tabContent",
                ) { current ->
                    when (current) {
                        0 -> GalleryTab(
                            ui = ui,
                            contentPadding = contentPadding,
                            onOpen = onOpenViewer,
                            onToggleFavorite = { vm.toggleFavorite(it.id) },
                        )
                        1 -> AlbumsTab(
                            ui = ui,
                            contentPadding = contentPadding,
                            onOpenAlbum = onOpenAlbum,
                        )
                        2 -> FavoritesTab(
                            ui = ui,
                            contentPadding = contentPadding,
                            onOpen = onOpenViewer,
                            onToggleFavorite = { vm.toggleFavorite(it.id) },
                        )
                        3 -> SearchTab(
                            ui = ui,
                            contentPadding = contentPadding,
                            onQueryChange = vm::setSearchQuery,
                            onOpen = onOpenViewer,
                            onToggleFavorite = { vm.toggleFavorite(it.id) },
                        )
                        else -> SettingsScreen(
                            ui = ui,
                            vm = vm,
                            contentPadding = contentPadding,
                        )
                    }
                }
            }

            if (ui.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    runCatching { context.startActivity(intent) }
}
