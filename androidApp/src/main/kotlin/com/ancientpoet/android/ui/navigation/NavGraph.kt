package com.ancientpoet.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ancientpoet.android.ui.screen.auth.LoginScreen
import com.ancientpoet.android.ui.screen.conversation.ConversationScreen
import com.ancientpoet.android.ui.screen.home.HomeScreen
import com.ancientpoet.android.ui.screen.map.MapScreen
import com.ancientpoet.android.ui.screen.poet.PoetDetailScreen
import com.ancientpoet.android.ui.screen.poet.PoetListScreen
import com.ancientpoet.android.ui.screen.poetry.PoetryDetailScreen
import com.ancientpoet.android.ui.screen.poetry.PoetryListScreen
import com.ancientpoet.android.ui.screen.settings.SettingsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(onLoginSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } }) }
        composable("home") {
            HomeScreen(
                onConversationClick = { convId -> navController.navigate("conversation/$convId") },
                onPoetsClick = { navController.navigate("poets") },
                onMapClick = { navController.navigate("map") },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable("conversation/{convId}") { backStackEntry ->
            val convId = backStackEntry.arguments?.getString("convId")?.toLongOrNull() ?: return@composable
            ConversationScreen(conversationId = convId, onBack = { navController.popBackStack() })
        }
        composable("poets") {
            PoetListScreen(
                onPoetClick = { poetId -> navController.navigate("poet/$poetId") },
                onBack = { navController.popBackStack() },
            )
        }
        composable("poet/{poetId}") { backStackEntry ->
            val poetId = backStackEntry.arguments?.getString("poetId")?.toLongOrNull() ?: return@composable
            PoetDetailScreen(
                poetId = poetId,
                onBack = { navController.popBackStack() },
                onStartConversation = { id, year ->
                    // TODO: pass year to create conversation screen
                    navController.navigate("conversation/new/$id")
                },
            )
        }
        composable("map") { MapScreen(onBack = { navController.popBackStack() }) }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = { navController.navigate("login") { popUpTo(0) { inclusive = true } } },
            )
        }
        // Poetry routes
        composable("poetry") {
            PoetryListScreen(onPoemClick = { poemId -> navController.navigate("poem/$poemId") }, onBack = { navController.popBackStack() })
        }
        composable("poetry/{poetId}") { backStackEntry ->
            val poetId = backStackEntry.arguments?.getString("poetId")?.toLongOrNull()
            PoetryListScreen(poetId = poetId, onPoemClick = { poemId -> navController.navigate("poem/$poemId") }, onBack = { navController.popBackStack() })
        }
        composable("poem/{poemId}") { backStackEntry ->
            val poemId = backStackEntry.arguments?.getString("poemId")?.toLongOrNull() ?: return@composable
            PoetryDetailScreen(poemId = poemId, onBack = { navController.popBackStack() })
        }
    }
}
