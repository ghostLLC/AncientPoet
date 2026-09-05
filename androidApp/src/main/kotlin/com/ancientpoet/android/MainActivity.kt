package com.ancientpoet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.data.ArrivalNotifications
import com.ancientpoet.android.data.ReadingPreferences
import com.ancientpoet.android.ui.navigation.NavGraph
import com.ancientpoet.android.ui.session.*
import com.ancientpoet.android.ui.theme.AncientPoetTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences: ReadingPreferences = koinInject()
            val options by preferences.options.collectAsStateWithLifecycle()
            val session: SessionViewModel = koinViewModel()
            val status by session.status.collectAsStateWithLifecycle()
            var pendingRoute by rememberSaveable { mutableStateOf<String?>(null) }
            val owner = (status as? SessionStatus.Authenticated)?.userId ?: 0L
            LaunchedEffect(owner, options.notifications) {
                if (status != SessionStatus.Checking) ArrivalNotifications.configure(this@MainActivity, owner > 0 && options.notifications)
            }
            val dark = when (options.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            AncientPoetTheme(
                darkTheme = dark,
                textScale = options.textScale
            ) {
                if (status == SessionStatus.Checking) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                } else {
                    key(owner) {
                        // Account changes dispose the navigation stack and every private screen ViewModel.
                        NavGraph(authenticated = owner > 0, pendingRoute = pendingRoute, onPendingRoute = { pendingRoute = it })
                    }
                }
            }
        }
    }
}
