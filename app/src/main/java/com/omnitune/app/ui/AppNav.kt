package com.omnitune.app.ui

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omnitune.app.ui.components.MiniPlayer
import com.omnitune.app.ui.home.HomeScreen
import com.omnitune.app.ui.library.LibraryScreen
import com.omnitune.app.ui.library.PlaylistScreen
import com.omnitune.app.ui.library.TrackListScreen
import com.omnitune.app.ui.login.LoginScreen
import com.omnitune.app.ui.player.NowPlayingScreen
import com.omnitune.app.ui.search.SearchScreen
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.Background
import com.omnitune.app.ui.theme.Titanium

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val FAVORITES = "favorites"
    const val PLAYLIST = "playlist/{id}"
    const val NOW = "now"
    fun playlist(id: String) = "playlist/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Inicio", Icons.Rounded.Home),
    Tab(Routes.SEARCH, "Buscar", Icons.Rounded.Search),
    Tab(Routes.LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
)

@Composable
fun AppRoot(main: MainViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val loggedIn by main.loggedIn.collectAsStateWithLifecycle()
    val player by main.playerState.collectAsStateWithLifecycle()
    val start = remember { if (main.loggedIn.value) Routes.HOME else Routes.LOGIN }
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route

    // Si la sesión muere (refresh revocado, logout) volvemos al login y limpiamos la pila.
    LaunchedEffect(loggedIn) {
        if (!loggedIn && nav.currentDestination?.route != Routes.LOGIN) {
            nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { main.messages.collect { snackbar.showSnackbar(it) } }

    val showBars = route == Routes.HOME || route == Routes.SEARCH || route == Routes.LIBRARY ||
        route == Routes.FAVORITES || route == Routes.PLAYLIST

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBars) {
                Column {
                    MiniPlayer(
                        state = player,
                        onOpen = { nav.navigate(Routes.NOW) { launchSingleTop = true } },
                        onToggle = main::togglePlay,
                        onNext = main::next,
                    )
                    NavigationBar(containerColor = Background) {
                        tabs.forEach { tab ->
                            val selected = route == tab.route ||
                                (tab.route == Routes.LIBRARY && (route == Routes.FAVORITES || route == Routes.PLAYLIST))
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Accent,
                                    selectedTextColor = Accent,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Titanium,
                                    unselectedTextColor = Titanium,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = start, modifier = Modifier.fillMaxSize()) {
            composable(Routes.LOGIN) {
                LoginScreen(padding = padding, onLoggedIn = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                })
            }
            composable(Routes.HOME) { HomeScreen(main, padding) }
            composable(Routes.SEARCH) { SearchScreen(main, padding) }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    main = main,
                    padding = padding,
                    onOpenFavorites = { nav.navigate(Routes.FAVORITES) },
                    onOpenPlaylist = { nav.navigate(Routes.playlist(it)) },
                    onLogout = main::logout,
                )
            }
            composable(Routes.FAVORITES) {
                val favorites by main.favorites.collectAsStateWithLifecycle()
                TrackListScreen(
                    title = "Favoritos",
                    subtitle = "${favorites.size} canciones",
                    coverUrl = null,
                    isFavoritesList = true,
                    tracks = favorites,
                    main = main,
                    padding = padding,
                    onBack = { nav.popBackStack() },
                    emptyText = "Toca el corazón en cualquier canción para guardarla aquí.",
                )
            }
            composable(
                Routes.PLAYLIST,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) {
                PlaylistScreen(main, padding, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.NOW,
                enterTransition = { slideInVertically(initialOffsetY = { it }) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) },
                popEnterTransition = { slideInVertically(initialOffsetY = { it }) },
                popExitTransition = { slideOutVertically(targetOffsetY = { it }) },
            ) {
                NowPlayingScreen(main, onClose = { nav.popBackStack() })
            }
        }
    }
}
