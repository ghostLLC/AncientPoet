package com.ancientpoet.android.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.VermilionRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VermilionRed)) {
                Text("退出登录", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
