package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.component.DrawingCanvasDialog
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(conversationId: Long, onBack: () -> Unit, viewModel: ConversationViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showDrawing by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(conversationId) { viewModel.loadConversation(conversationId); viewModel.loadMessages(conversationId) }
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(state.poetName, color = InkBlack, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
                )
                // Storyline banner
                AnimatedVisibility(visible = state.storyline != null) {
                    state.storyline?.let { sl ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { showYearPicker = true },
                            colors = CardDefaults.cardColors(containerColor = ImperialGold.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sl.currentYear}年 · ${sl.poetAge}岁 · ${sl.locationName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                                    sl.eventDescription?.let {
                                        Text(it, fontSize = 12.sp, color = if (sl.eventType == "war" || sl.eventType == "exile") VermilionRed else WarmGray)
                                    }
                                }
                                Text("↕", fontSize = 16.sp, color = ImperialGold)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Drawing / image buttons
                IconButton(onClick = { showDrawing = true }) { Icon(Icons.Default.Brush, "绘画", tint = ImperialGold) }
                IconButton(onClick = { /* TODO: gallery picker */ }) { Icon(Icons.Default.Image, "图片", tint = ImperialGold) }

                OutlinedTextField(
                    value = messageText, onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("书信内容...", color = WarmGray) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = WarmGray.copy(alpha = 0.3f),
                        focusedTextColor = InkBlack,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { viewModel.sendMessage(conversationId, messageText); messageText = "" },
                    enabled = messageText.isNotBlank(),
                ) {
                    Icon(Icons.Default.Send, "发送", tint = if (messageText.isNotBlank()) VermilionRed else WarmGray)
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), state = listState) {
            items(state.messages) { msg ->
                MessageBubble(
                    text = msg.contentText ?: "",
                    translation = msg.translation,
                    imageUrl = msg.imageUrl,
                    isUser = msg.senderType == "user",
                )
            }
            if (state.isSending) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = ImperialGold,
                        trackColor = ImperialGold.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }

    // Drawing dialog
    if (showDrawing) {
        DrawingCanvasDialog(
            onConfirm = { /* upload and attach */ showDrawing = false },
            onDismiss = { showDrawing = false },
        )
    }

    // Year picker dialog
    if (showYearPicker && state.storyline != null) {
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("跳转到哪一年？", color = InkBlack) },
            text = {
                // Simple year list
                val poet = state.storyline!!
                // Show decade markers
                Column {
                    Text("当前: ${poet.currentYear}年", color = VermilionRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val decades = (poet.birthDecade..poet.deathDecade step 10).toList()
                    decades.forEach { decade ->
                        TextButton(onClick = {
                            viewModel.jumpToYear(conversationId, decade)
                            showYearPicker = false
                        }) {
                            Text("${decade}年代", color = InkBlack)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showYearPicker = false }) { Text("取消", color = WarmGray) } },
            containerColor = RicePaper,
        )
    }
}
