package com.ancientpoet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.navigation.NavGraph
import com.ancientpoet.android.ui.session.SessionStatus
import com.ancientpoet.android.ui.session.SessionViewModel
import com.ancientpoet.android.ui.theme.AncientPoetTheme
import com.ancientpoet.android.ui.theme.RicePaper
import com.ancientpoet.android.ui.theme.VermilionRed
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AncientPoetTheme {
                val sessionViewModel: SessionViewModel = koinViewModel()
                val status by sessionViewModel.status.collectAsStateWithLifecycle()
                when (status) {
                    SessionStatus.Checking -> Box(
                        modifier = Modifier.fillMaxSize().background(RicePaper),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = VermilionRed) }
                    SessionStatus.Authenticated -> NavGraph(
                        startDestination = "home",
                        sessionViewModel = sessionViewModel,
                    )
                    SessionStatus.Anonymous -> NavGraph(
                        startDestination = "login",
                        sessionViewModel = sessionViewModel,
                    )
                }
            }
        }
    }
}
