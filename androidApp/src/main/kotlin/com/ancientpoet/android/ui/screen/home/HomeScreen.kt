package com.ancientpoet.android.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.component.LetterCard
import com.ancientpoet.android.ui.component.ApiErrorBanner
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConversationClick: (Long) -> Unit,
    onPoetsClick: () -> Unit,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = {
                    Text("鸿雁", fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = VermilionRed)
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.HistoryEdu, "诗词阁", tint = WarmGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPoetsClick,
                containerColor = ImperialGold,
                contentColor = RicePaper,
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Mail, "新书信")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(RicePaper)) {
            ApiErrorBanner(
                message = state.errorMessage,
                canRetry = state.canRetry,
                onRetry = viewModel::loadConversations,
            )
            if (state.isLoading && state.conversations.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VermilionRed)
                }
            }
            // Quick nav row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TextButton(onClick = onPoetsClick) {
                    Icon(Icons.Default.Person, "诗人", tint = ImperialGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("诗人", color = WarmGray, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onMapClick) {
                    Icon(Icons.Default.Place, "地图", tint = ImperialGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("地图", color = WarmGray, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (state.conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无书信往来", fontFamily = SerifFont, fontSize = 18.sp, color = WarmGray)
                        Spacer(Modifier.height(4.dp))
                        Text("点击右下角研墨修书", style = MaterialTheme.typography.labelMedium, color = WarmGray.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.conversations) { conv ->
                        LetterCard(
                            poetName = conv.poetName,
                            dynasty = conv.dynasty,
                            lastMessage = conv.lastMessage,
                            modifier = Modifier.clickable { onConversationClick(conv.id) },
                        )
                    }
                }
            }
        }
    }
}
