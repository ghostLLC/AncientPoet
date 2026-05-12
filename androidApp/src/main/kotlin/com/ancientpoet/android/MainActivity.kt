package com.ancientpoet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ancientpoet.android.ui.navigation.NavGraph
import com.ancientpoet.android.ui.theme.AncientPoetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AncientPoetTheme {
                NavGraph()
            }
        }
    }
}
