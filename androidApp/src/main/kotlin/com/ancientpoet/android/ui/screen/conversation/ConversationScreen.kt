package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.VermilionRed
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(conversationId: Long, onBack: () -> Unit, viewModel: ConversationViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationId) { viewModel.loadMessages(conversationId) }
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.poetName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = messageText, onValueChange = { messageText = it }, modifier = Modifier.weight(1f), label = { Text("书信内容...") }, maxLines = 4)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.sendMessage(conversationId, messageText); messageText = "" }, enabled = messageText.isNotBlank()) {
                    Icon(Icons.Default.Send, "发送", tint = VermilionRed)
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), state = listState) {
            items(state.messages) { msg ->
                MessageBubble(text = msg.contentText ?: "", translation = msg.translation, isUser = msg.senderType == "user")
            }
            if (state.isSending) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp)) }
            }
        }
    }
}
