package com.ancientpoet.android.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.component.LetterCard
import com.ancientpoet.android.ui.theme.ImperialGold
import com.ancientpoet.android.ui.theme.InkBlack
import com.ancientpoet.android.ui.theme.VermilionRed
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onConversationClick: (Long) -> Unit,
    onPoetsClick: () -> Unit,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("鸿雁", fontWeight = FontWeight.Bold, color = VermilionRed) },
                actions = {
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "设置") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPoetsClick, containerColor = ImperialGold) {
                Icon(Icons.Default.Edit, "新书信", tint = InkBlack)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Bottom nav shortcuts
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                NavigationItem("诗人", Icons.Default.Person, onPoetsClick)
                NavigationItem("地图", Icons.Default.Place, onMapClick)
            }

            if (state.conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("暂无书信往来\n点击右下角开始第一封信", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.conversations) { conv ->
                        LetterCard(poetName = conv.poetName, lastMessage = conv.lastMessage, dynasty = conv.dynasty, modifier = Modifier.clickable { onConversationClick(conv.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(12.dp)) {
        Icon(icon, label, tint = ImperialGold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
