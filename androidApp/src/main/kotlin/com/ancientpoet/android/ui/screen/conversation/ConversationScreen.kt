package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        containerColor = RicePaper,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(state.poetName, fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = InkBlack)
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
                )
                // Storyline banner
                AnimatedVisibility(visible = state.storyline != null) {
                    state.storyline?.let { sl ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { showYearPicker = true },
                            shape = RoundedCornerShape(6.dp),
                            color = ImperialGold.copy(alpha = 0.12f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${sl.currentYear}年 · ${sl.poetAge}岁 · ${sl.locationName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                                        sl.eventDescription?.let { Text(it, fontSize = 12.sp, color = if (sl.eventType == "war" || sl.eventType == "exile") VermilionRed else WarmGray) }
                                    }
                                    Text("↕", fontSize = 16.sp, color = ImperialGold)
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Borderless input area — matches DESIGN/_4 letter-lines style
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RicePaper.copy(alpha = 0.95f),
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    IconButton(onClick = { showDrawing = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Brush, "绘画", tint = ImperialGold, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Image, "图片", tint = ImperialGold, modifier = Modifier.size(22.dp))
                    }

                    // Letter-lines input area — borderless with faint rule lines
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RicePaper)
                    ) {
                        // Subtle letter lines (simulated via background)
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            var y = 40f
                            while (y < size.height) {
                                drawLine(
                                    color = WarmGray.copy(alpha = 0.12f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 1f,
                                )
                                y += 40f
                            }
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 40.sp,
                                letterSpacing = 0.5.sp,
                            ),
                            placeholder = {
                                Text(
                                    "研墨铺纸，落笔生花...",
                                    color = WarmGray.copy(alpha = 0.5f),
                                    fontFamily = SerifFont,
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = InkBlack,
                                cursorColor = VermilionRed,
                            ),
                            shape = RoundedCornerShape(0.dp),
                        )
                    }

                    // Send — mail (envelope) icon
                    IconButton(
                        onClick = { viewModel.sendMessage(conversationId, messageText); messageText = "" },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Default.Mail, "发送",
                            tint = if (messageText.isNotBlank()) VermilionRed else WarmGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().background(RicePaper),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        ) {
            // Poet header — centered name with dynasty/year context
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.poetName,
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = InkBlack.copy(alpha = 0.9f),
                    )
                    state.storyline?.let { sl ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.width(24.dp).height(1.dp).background(OutlineVariantColor))
                            Text(
                                "${sl.currentYear}年 · ${sl.locationName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmGray,
                            )
                            Box(Modifier.width(24.dp).height(1.dp).background(OutlineVariantColor))
                        }
                    }
                }
            }

            items(state.messages) { msg ->
                MessageBubble(
                    text = msg.contentText,
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
                        trackColor = ImperialGold.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }

    if (showDrawing) {
        DrawingCanvasDialog(onConfirm = { showDrawing = false }, onDismiss = { showDrawing = false })
    }

    if (showYearPicker && state.storyline != null) {
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("跳转到哪一年？", fontFamily = SerifFont, color = InkBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("当前: ${state.storyline!!.currentYear}年", color = VermilionRed, fontFamily = SerifFont)
                    Spacer(Modifier.height(8.dp))
                    val decades = ((state.storyline!!.currentYear - 30)..(state.storyline!!.currentYear + 30) step 10).toList()
                    decades.forEach { decade ->
                        TextButton(onClick = {
                            viewModel.jumpToYear(conversationId, decade)
                            showYearPicker = false
                        }) {
                            Text("${decade}年", fontFamily = SerifFont, color = InkBlack)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showYearPicker = false }) { Text("取消", color = WarmGray) } },
            containerColor = RicePaper,
        )
    }
}
