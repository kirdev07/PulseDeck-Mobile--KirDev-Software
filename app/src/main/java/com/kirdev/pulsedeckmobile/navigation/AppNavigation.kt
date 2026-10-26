package com.kirdev.pulsedeckmobile.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.LocalMessages
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.remote.PairingClient
import com.kirdev.pulsedeckmobile.feature.applications.ProcessesScreen
import com.kirdev.pulsedeckmobile.feature.connection.ConnectionScreen
import com.kirdev.pulsedeckmobile.feature.connection.ConnectionViewModel
import com.kirdev.pulsedeckmobile.feature.dashboard.HomeScreen
import com.kirdev.pulsedeckmobile.feature.devices.DevicesScreen
import com.kirdev.pulsedeckmobile.feature.files.FilesScreen
import com.kirdev.pulsedeckmobile.feature.media.MiniPlayer
import com.kirdev.pulsedeckmobile.feature.media.MusicScreen
import com.kirdev.pulsedeckmobile.feature.menu.MenuScreen
import com.kirdev.pulsedeckmobile.feature.monitoring.MonitoringScreen
import com.kirdev.pulsedeckmobile.feature.notifications.EventsScreen
import com.kirdev.pulsedeckmobile.feature.remote.ControlScreen
import com.kirdev.pulsedeckmobile.feature.settings.AppearanceScreen
import com.kirdev.pulsedeckmobile.feature.settings.SettingsScreen
import com.kirdev.pulsedeckmobile.media.hasTrack
import com.kirdev.pulsedeckmobile.media.playerState

object Routes {
    const val HOME = "home"
    const val MONITORING = "monitoring"
    const val CONTROL = "control"
    const val FILES = "files"
    const val MENU = "menu"
    const val MUSIC = "music"
    const val PROCESSES = "processes"
    const val DEVICES = "devices"
    const val EVENTS = "events"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val PAIR = "pair"
}

private data class Tab(val route: String, val title: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(Routes.HOME, "Главная", AppIcons.Home),
    Tab(Routes.MONITORING, "Мониторинг", AppIcons.Pulse),
    Tab(Routes.CONTROL, "Управление", AppIcons.Control),
    Tab(Routes.FILES, "Файлы", AppIcons.Folder),
    Tab(Routes.MENU, "Меню", AppIcons.Menu),
)

@Composable
fun AppNavigation(app: PulseDeskApp) {
    val state by app.connection.state.collectAsStateWithLifecycle()
    val messages = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalMessages provides messages) {
        // Not paired (first start, revoked, forgotten): only the pairing screen makes sense.
        if (state is ConnectionState.NotPaired || state is ConnectionState.Revoked) {
            Pairing(app, onBack = null, onPaired = {})
        } else {
            Main(app, messages)
        }
    }
}

@Composable
private fun Pairing(app: PulseDeskApp, onBack: (() -> Unit)?, onPaired: () -> Unit) {
    val vm: ConnectionViewModel = viewModel(factory = viewModelFactory {
        initializer { ConnectionViewModel(app.pairing, PairingClient(), app.connection) }
    })
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.search() }
    ConnectionScreen(vm, onBack, onPaired)
}

@Composable
private fun Main(app: PulseDeskApp, messages: SnackbarHostState) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val state by app.connection.state.collectAsStateWithLifecycle()
    val media by app.connection.media.collectAsStateWithLifecycle()
    val showPlayer = playerState(state, media).hasTrack && route != Routes.MUSIC && route != Routes.PAIR
    val showTabs = route in Tabs.map { it.route }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        containerColor = Palette.Black,
        snackbarHost = {
            SnackbarHost(messages) { Snackbar(it, containerColor = Palette.Surface2, contentColor = Palette.Text) }
        },
        bottomBar = {
            if (!landscape) Column(Modifier.windowInsetsPadding(sideInsets)) {
                // Mini player only while there is a real track on the PC, right above the tabs.
                if (showPlayer) MiniPlayer(app, onOpen = { nav.navigate(Routes.MUSIC) })
                if (showTabs) BottomTabs(nav, route)
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        // Landscape (phone on a stand): tabs move to a rail on the left so screens keep their height.
        Row(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(sideInsets)) {
            if (landscape && showTabs) SideTabs(nav, route)
            Column(Modifier.weight(1f)) {
                Box(Modifier.weight(1f)) { Screens(app, nav) }
                if (landscape && showPlayer) MiniPlayer(app, onOpen = { nav.navigate(Routes.MUSIC) })
            }
        }
    }
}

@Composable
private fun Screens(app: PulseDeskApp, nav: NavHostController) {
    NavHost(
        nav, startDestination = Routes.HOME,
        modifier = Modifier.fillMaxSize().background(Palette.Black),
        enterTransition = { if (animations(app)) fadeIn() else EnterTransition.None },
        exitTransition = { if (animations(app)) fadeOut() else ExitTransition.None },
    ) {
        val back: () -> Unit = { nav.popBackStack() }
        val go: (String) -> Unit = { nav.navigate(it) }
        composable(Routes.HOME) { HomeScreen(app, go) }
        composable(Routes.MONITORING) { MonitoringScreen(app) }
        composable(Routes.CONTROL) { ControlScreen(app, go) }
        composable(Routes.FILES) { FilesScreen(app) }
        composable(Routes.MENU) { MenuScreen(app, go) }
        composable(Routes.MUSIC) { MusicScreen(app, back) }
        composable(Routes.PROCESSES) { ProcessesScreen(app, back) }
        composable(Routes.DEVICES) { DevicesScreen(app, back, go) }
        composable(Routes.EVENTS) { EventsScreen(app, back) }
        composable(Routes.SETTINGS) { SettingsScreen(app, back, go) }
        composable(Routes.APPEARANCE) { AppearanceScreen(app, back) }
        composable(Routes.PAIR) { Pairing(app, onBack = back, onPaired = { nav.popBackStack(Routes.HOME, false) }) }
    }
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun SideTabs(nav: NavHostController, route: String?) {
    NavigationRail(containerColor = Palette.Black, modifier = Modifier.statusBarsPadding()) {
        Spacer(Modifier.weight(1f))
        for (tab in Tabs) {
            NavigationRailItem(
                selected = route == tab.route,
                onClick = { if (route != tab.route) nav.openTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(22.dp)) },
                label = { Text(tab.title, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Palette.Black, selectedTextColor = Palette.Text,
                    indicatorColor = Palette.accent, unselectedIconColor = Palette.Text3, unselectedTextColor = Palette.Text3),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Landscape: the system buttons sit on a side; keep content and the mini player clear of them. */
private val sideInsets @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)

private fun animations(app: PulseDeskApp) = app.settings.values.value.animations

@Composable
private fun BottomTabs(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = Palette.Black, tonalElevation = 0.dp) {
        for (tab in Tabs) {
            NavigationBarItem(
                selected = route == tab.route,
                onClick = { if (route != tab.route) nav.openTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                label = { Text(tab.title, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Palette.Black, selectedTextColor = Palette.Text,
                    indicatorColor = Palette.accent, unselectedIconColor = Palette.Text3, unselectedTextColor = Palette.Text3),
            )
        }
    }
}
