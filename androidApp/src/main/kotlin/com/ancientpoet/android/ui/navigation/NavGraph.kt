package com.ancientpoet.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ancientpoet.android.ui.screen.auth.LoginScreen
import com.ancientpoet.android.ui.screen.community.CommunityScreen
import com.ancientpoet.android.ui.screen.community.PostDetailScreen
import com.ancientpoet.android.ui.screen.community.ProfileScreen
import com.ancientpoet.android.ui.screen.conversation.ConversationScreen
import com.ancientpoet.android.ui.screen.conversation.ConversationViewModel
import com.ancientpoet.android.ui.screen.home.HomeScreen
import com.ancientpoet.android.ui.screen.map.MapScreen
import com.ancientpoet.android.ui.screen.poet.PoetDetailScreen
import com.ancientpoet.android.ui.screen.poet.PoetListScreen
import com.ancientpoet.android.ui.screen.poetry.PoetryDetailScreen
import com.ancientpoet.android.ui.screen.poetry.PoetryListScreen
import com.ancientpoet.android.ui.screen.settings.SettingsScreen
import com.ancientpoet.android.ui.session.SessionViewModel
import com.ancientpoet.android.ui.theme.RicePaper
import com.ancientpoet.android.ui.theme.VermilionRed
import org.koin.androidx.compose.koinViewModel

@Composable
fun NavGraph(
    startDestination: String = "login",
    sessionViewModel: SessionViewModel? = null,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }
        composable("home") {
            HomeScreen(
                onConversationClick = { convId -> navController.navigate("conversation/$convId?year=0") },
                onPoetsClick = { navController.navigate("poets") },
                onMapClick = { navController.navigate("map") },
                onSettingsClick = { navController.navigate("settings") },
            )
        }
        composable(
            route = "conversation/{convId}?year={year}",
            arguments = listOf(
                navArgument("convId") { type = NavType.LongType },
                navArgument("year") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getLong("convId") ?: return@composable
            val year = backStackEntry.arguments?.getInt("year")?.takeIf { it != 0 }
            ConversationScreen(
                conversationId = convId,
                initialYear = year,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "conversation/new/{poetId}?year={year}",
            arguments = listOf(
                navArgument("poetId") { type = NavType.LongType },
                navArgument("year") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { backStackEntry ->
            val poetId = backStackEntry.arguments?.getLong("poetId") ?: return@composable
            val year = backStackEntry.arguments?.getInt("year")?.takeIf { it != 0 }
            NewConversationScreen(
                poetId = poetId,
                year = year,
                onCreated = { conversationId ->
                    val creationRoute = navController.currentDestination?.route
                    navController.navigate("conversation/$conversationId?year=${year ?: 0}") {
                        creationRoute?.let { popUpTo(it) { inclusive = true } }
                    }
                },
            )
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
                onStartConversation = { id, year -> navController.navigate("conversation/new/$id?year=${year ?: 0}") },
            )
        }
        composable("map") { MapScreen(onBack = { navController.popBackStack() }) }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    sessionViewModel?.logout()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
            )
        }
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
        composable("community") {
            CommunityScreen(
                onPostClick = { postId -> navController.navigate("post/$postId") },
                onBack = { navController.popBackStack() },
                onProfileClick = { userId -> navController.navigate("profile/$userId") },
            )
        }
        composable("post/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")?.toLongOrNull() ?: return@composable
            PostDetailScreen(postId = postId, onBack = { navController.popBackStack() })
        }
        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: return@composable
            ProfileScreen(userId = userId, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun NewConversationScreen(
    poetId: Long,
    year: Int?,
    onCreated: (Long) -> Unit,
    viewModel: ConversationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(poetId, year) {
        viewModel.createConversation(poetId, year ?: 0, onCreated)
    }
    Box(modifier = Modifier.fillMaxSize().background(RicePaper), contentAlignment = Alignment.Center) {
        if (state.errorMessage == null) {
            CircularProgressIndicator(color = VermilionRed)
        } else {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.errorMessage!!, color = VermilionRed)
                if (state.canRetry) {
                    androidx.compose.material3.TextButton(onClick = { viewModel.createConversation(poetId, year ?: 0, onCreated) }) { Text("重试") }
                }
            }
        }
    }
}
