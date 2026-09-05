package com.ancientpoet.android.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.component.ApiErrorBanner
import com.ancientpoet.android.ui.screen.conversation.formatLetterTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConversationClick: (Long) -> Unit,
    onPoetsClick: () -> Unit,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogin: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(Unit) {
        viewModel.start()
        onStopOrDispose { viewModel.stop() }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("鸿雁", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                actions = { IconButton(onClick = viewModel::loadConversations) { Icon(Icons.Default.Refresh, "刷新收信匣") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (state.conversations.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onPoetsClick,
                    icon = { Icon(Icons.Default.Edit, null) },
                    text = { Text("写新信") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(Modifier.padding(top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("与古人，慢慢说。", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "让一封信，留住片刻心事。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.info?.development == true) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                if (state.info?.demoAi == true) {
                                    "本地演示 · 固定回信文本"
                                } else if (state.info?.acceleratedDelivery == true) {
                                    "开发环境 · 加速投递"
                                } else {
                                    "开发环境 · 测试登录"
                                },
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            if (!state.guest) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = !state.showArchived,
                            onClick = { viewModel.showArchived(false) },
                            label = { Text("收信匣" + state.conversations.sumOf { it.unreadCount }.takeIf { it > 0 && !state.showArchived }?.let { " · " + it + " 封未读" }.orEmpty()) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        FilterChip(
                            selected = state.showArchived,
                            onClick = { viewModel.showArchived(true) },
                            label = { Text("已归档") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
            item {
                ApiErrorBanner(state.errorMessage, state.canRetry, viewModel::loadConversations, Modifier)
                if (state.showingCache) {
                    Text(
                        "本机保存的通信 · 正在等候更新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.isLoading && state.conversations.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state.conversations.isEmpty() && !state.isLoading) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                if (state.showArchived) "这里收好暂别的通信" else "你的第一封信，想写给谁？",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                if (state.showArchived) {
                                    "归档会保留书信，你可以随时恢复。"
                                } else {
                                    "先认识一位诗人，选择落脚地，再把日常见闻写进信里。回信会沿着驿路，慢慢来到。"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!state.showArchived) {
                                Button(onClick = onPoetsClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("去认识一位诗人") }
                                if (state.guest) TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("已有通信？登录收信") }
                            }
                        }
                    }
                }
            }
            items(state.conversations, key = { it.id }) { conversation ->
                Column(
                    Modifier.fillMaxWidth().clickable(onClickLabel = "打开与" + conversation.poet.name + "的通信") {
                        onConversationClick(conversation.id)
                    }.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    conversation.poet.name.takeLast(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(conversation.poet.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                conversation.poet.dynasty + " · 公元 " + conversation.currentYear + " 年",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (conversation.unreadCount > 0) Badge { Text(conversation.unreadCount.toString() + " 封未读") }
                    }
                    Text(
                        conversation.lastMessage.ifBlank { "这段通信，等你落笔。" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (conversation.pendingCount > 0) {
                                conversation.pendingCount.toString() + " 封回信待抵达"
                            } else {
                                if (conversation.archived) "已归档" else "可继续写信"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            formatLetterTime(conversation.lastActivityAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
