package com.ancientpoet.android.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ancientpoet.android.ui.screen.auth.LoginScreen
import com.ancientpoet.android.ui.screen.conversation.*
import com.ancientpoet.android.ui.screen.home.HomeScreen
import com.ancientpoet.android.ui.screen.map.MapScreen
import com.ancientpoet.android.ui.screen.poet.*
import com.ancientpoet.android.ui.screen.poetry.*
import com.ancientpoet.android.ui.screen.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun NavGraph(authenticated: Boolean, pendingRoute: String?, onPendingRoute: (String?) -> Unit) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val tabs = listOf("home" to "书信", "poets" to "诗人", "map" to "驿路", "settings" to "我的")
    fun open(route: String) {
        nav.navigate(route) { launchSingleTop = true }
    }
    fun login(destination: String? = null) {
        onPendingRoute(destination)
        open("login")
    }
    LaunchedEffect(authenticated) {
        if (authenticated && pendingRoute != null) {
            open(pendingRoute)
            onPendingRoute(null)
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (current?.destination?.route in tabs.map { it.first }) {
            NavigationBar {
                tabs.forEachIndexed { index, (route, label) ->
                    NavigationBarItem(
                        selected = current?.destination?.route == route,
                        onClick = {
                            nav.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(listOf(Icons.Default.MailOutline, Icons.Default.People, Icons.Default.Place, Icons.Default.Person)[index], null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("home") {
                HomeScreen(
                    onConversationClick = { open("conversation/" + it) },
                    onPoetsClick = { open("poets") },
                    onMapClick = { open("map") },
                    onSettingsClick = { open("settings") },
                    onLogin = { login() }
                )
            }
            composable("poets") {
                PoetListScreen(
                    onPoetClick = { open("poet/" + it) },
                    onBack = { nav.popBackStack() },
                    onPoetry = { open("poetry") },
                    showBack = false
                )
            }
            composable("poet/{poetId}", arguments = listOf(navArgument("poetId") { type = NavType.LongType })) { entry ->
                PoetDetailScreen(
                    poetId = entry.arguments!!.getLong("poetId"),
                    onBack = { nav.popBackStack() },
                    onPoetry = { open("poetry/" + it) },
                    onStartConversation = { id, year ->
                        val route = "new/" + id + "?year=" + (year ?: 0)
                        if (authenticated) open(route) else login(route)
                    }
                )
            }
            composable("map") { MapScreen(onBack = { nav.popBackStack() }, showBack = false, onLogin = { login() }) }
            composable("route/{dynasty}/{conversationId}", arguments = listOf(navArgument("conversationId") { type = NavType.LongType })) { entry ->
                MapScreen(
                    onBack = { nav.popBackStack() },
                    initialDynasty = entry.arguments!!.getString("dynasty") ?: "tang",
                    conversationId = entry.arguments!!.getLong("conversationId"),
                    onLogin = { login() }
                )
            }
            composable("settings") {
                SettingsScreen(authenticated = authenticated, onLogin = { login() }, onSignedOut = { onPendingRoute(null) })
            }
            composable("login") {
                LoginScreen(onLoginSuccess = {}, onBack = {
                    onPendingRoute(null)
                    nav.popBackStack()
                })
            }
            composable("conversation/{convId}", arguments = listOf(navArgument("convId") { type = NavType.LongType })) { entry ->
                ConversationScreen(
                    entry.arguments!!.getLong("convId"),
                    onBack = { nav.popBackStack() },
                    onMap = { dynasty, id -> open("route/" + dynasty + "/" + id) }
                )
            }
            composable(
                "new/{poetId}?year={year}",
                arguments = listOf(
                    navArgument("poetId") { type = NavType.LongType },
                    navArgument("year") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { entry ->
                NewConversationScreen(
                    entry.arguments!!.getLong("poetId"),
                    entry.arguments!!.getInt("year"),
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.navigate("conversation/" + id) { popUpTo(entry.destination.route!!) { inclusive = true } }
                    }
                )
            }
            composable("poetry") { PoetryListScreen(onPoemClick = { open("poem/" + it) }, onBack = { nav.popBackStack() }) }
            composable("poetry/{poetId}", arguments = listOf(navArgument("poetId") { type = NavType.LongType })) { entry ->
                PoetryListScreen(poetId = entry.arguments!!.getLong("poetId"), onPoemClick = { open("poem/" + it) }, onBack = { nav.popBackStack() })
            }
            composable("poem/{poemId}", arguments = listOf(navArgument("poemId") { type = NavType.LongType })) { entry ->
                PoetryDetailScreen(entry.arguments!!.getLong("poemId"), onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun NewConversationScreen(
    poetId: Long,
    year: Int,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: ConversationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(poetId, year) { viewModel.createConversation(poetId, year, onCreated) }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Text("正在展开信笺…", Modifier.padding(16.dp))
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.canRetry) Button(onClick = { viewModel.createConversation(poetId, year, onCreated) }) { Text("重试") }
            TextButton(onClick = onBack) { Text("返回诗人") }
        }
    }
}
