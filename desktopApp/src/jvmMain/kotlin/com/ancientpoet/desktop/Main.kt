package com.ancientpoet.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "鸿雁 AncientPoet",
        state = rememberWindowState(width = 420.dp, height = 800.dp)
    ) {
        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("鸿雁", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("AncientPoet", fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Text("Desktop 客户端 — Phase 4 待开发", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
