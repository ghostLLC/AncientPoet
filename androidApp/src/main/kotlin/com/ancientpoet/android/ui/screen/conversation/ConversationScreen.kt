package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.component.ApiErrorBanner
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: Long,
    onBack: () -> Unit,
    initialYear: Int? = null,
    onMap: (String, Long) -> Unit = { _, _ -> },
    viewModel: ConversationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var chooseYear by rememberSaveable { mutableStateOf(false) }
    var allPending by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    var scrolledSequence by remember { mutableIntStateOf(0) }
    LaunchedEffect(conversationId) { viewModel.loadInitial(conversationId, initialYear) }
    LifecycleStartEffect(conversationId) {
        viewModel.startPolling(conversationId)
        onStopOrDispose { viewModel.stopPolling() }
    }
    LaunchedEffect(state.messages.lastOrNull()?.id, state.sentSequence) {
        if (list.firstVisibleItemIndex < 2 || state.sentSequence > scrolledSequence) list.animateScrollToItem(0)
        scrolledSequence = state.sentSequence
    }
    val detail = state.detail
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(state.poetName.ifBlank { "书信" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                    actions = {
                        IconButton(onClick = { onMap(detail?.dynastyId ?: "tang", conversationId) }) { Icon(Icons.Default.Place, "查看通信驿路") }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "通信管理") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(if (detail?.archived == true) "恢复通信" else "归档通信") }, onClick = {
                                    menu = false
                                    viewModel.archive(conversationId, detail?.archived != true, onBack)
                                })
                                DropdownMenuItem(text = { Text("删除通信", color = MaterialTheme.colorScheme.error) }, onClick = {
                                    menu = false
                                    deleting = true
                                })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                if (detail?.currentYear != null) {
                    TextButton(onClick = { chooseYear = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(
                            "公元 " + detail.currentYear + " 年 · " + detail.poetLocation?.name.orEmpty() + "  /  选择年代",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (detail?.archived == true) {
                        TextButton(
                            onClick = { viewModel.archive(conversationId, false) { viewModel.loadConversation(conversationId) } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("恢复这段通信，继续写信") }
                    } else {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.draftText, onValueChange = { viewModel.updateDraft(conversationId, it) },
                                label = { Text("写一封信") }, placeholder = { Text("说说今日所见，或心中的牵挂") },
                                enabled = !state.isSending, modifier = Modifier.weight(1f), minLines = 1, maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium, shape = MaterialTheme.shapes.medium
                            )
                            FilledIconButton(
                                onClick = { viewModel.prepareSend(conversationId) },
                                enabled = state.draftText.isNotBlank() && !state.isSending && !state.previewLoading,
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (state.isSending || state.previewLoading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, "确认寄信时间")
                                }
                            }
                        }
                        Text(
                            if (state.isSending) {
                                "正在寄信，请稍候…"
                            } else if (state.draftText.isBlank()) {
                                "文字书信 · 回信需要时间"
                            } else {
                                (if (state.draftSaved) "信稿已保存在本机" else "正在保存信稿…") + " · " + state.draftText.length + " / 12000"
                            },
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = list,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item("status") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiErrorBanner(state.errorMessage, state.canRetry, { viewModel.retry(conversationId) }, Modifier)
                    if (state.needsLocation || (detail != null && detail.userLocation == null)) {
                        OutlinedButton(onClick = { onMap(detail?.dynastyId ?: "tang", conversationId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("先选择落脚地，让回信有处可达")
                        }
                    }
                    if (state.showingCache) {
                        Text(
                            "当前显示本机保存的书信，联网后会继续更新",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.pending.isNotEmpty()) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "等候回信 · " + state.pending.size + " 封",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                (if (allPending) state.pending else state.pending.takeLast(3)).forEach { pending ->
                                    val label = when (pending.status) {
                                        "queued" -> "来信已收，等待提笔"

                                        "generating" -> "正在写回信"

                                        "retrying" -> "回信暂时受阻，系统会再次尝试"

                                        "failed" -> if (pending.canRetry) "回信生成失败，可以重试" else "多次生成失败，请稍后新写一封信"

                                        else -> if ((pending.estimatedSecondsRemaining ?: 0) > 0) {
                                            "回信在途 · " + remainingTime(pending.estimatedSecondsRemaining ?: 0)
                                        } else {
                                            "已到预计时间，正在等候投递"
                                        }
                                    }
                                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    if (pending.canRetry) TextButton(onClick = { viewModel.retryReply(conversationId, pending.id) }) { Text("重试这封回信") }
                                }
                                if (state.pending.size > 3) TextButton(onClick = { allPending = !allPending }) { Text(if (allPending) "收起进度" else "查看全部进度") }
                            }
                        }
                    }
                    if (state.isLoading && state.messages.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            items(state.messages.asReversed(), key = { it.id }) { message ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MessageBubble(message.contentText, message.translation, message.imageUrl, message.senderType == "user")
                    Text(
                        formatLetterTime(message.deliveredAt ?: message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            if (state.hasOlder && state.messages.isNotEmpty()) {
                item("older") {
                    TextButton(onClick = { viewModel.loadOlder(conversationId) }, enabled = !state.loadingOlder, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.loadingOlder) "正在翻阅…" else "翻阅更早的书信")
                    }
                }
            }
            item("intro") {
                Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (state.messages.isEmpty()) "从一封信开始" else "纸短情长，见字如面", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "回信由 AI 依据人物资料进行文学演绎。诗人生平与诗词原作可在「诗人」中查阅。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.messages.isEmpty() && state.draftText.isBlank()) {
                        OutlinedButton(onClick = {
                            viewModel.updateDraft(conversationId, "近来常在忙碌中忘了看天。你今日所见的山水，可有什么值得记下的景色？")
                        }) { Text("以今日所见为题") }
                    }
                }
            }
        }
    }
    state.quote?.let { quote ->
        AlertDialog(
            onDismissRequest = viewModel::dismissQuote,
            title = { Text("把这封信交给鸿雁") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(quote.fromLocation + " → " + quote.toLocation)
                    Text("预计回信：" + formatLetterTime(quote.deliverAt))
                    Text(
                        if (quote.factors["demo"] == "true") {
                            "当前为加速演示环境。正式通信按距离、行旅状态与人物经历计算时间。"
                        } else {
                            "相距约 " + quote.distanceKm.toInt() + " 公里。生成排队或服务故障可能延后，以寄信回执与实际送达为准。"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.confirmSend(conversationId) }) { Text("寄出书信") } },
            dismissButton = { TextButton(onClick = viewModel::dismissQuote) { Text("继续写") } }
        )
    }
    if (chooseYear) {
        AlertDialog(
            onDismissRequest = { chooseYear = false },
            title = { Text("选择通信年代") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "新寄出的信会采用所选年代的背景；已寄出的信沿用寄出时的时空。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    state.yearOptions.forEach { year ->
                        TextButton(onClick = {
                            chooseYear = false
                            viewModel.jumpToYear(conversationId, year.year)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(year.year.toString() + " 年 · " + year.title)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { chooseYear = false }) { Text("返回") } }
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("删除这段通信？") },
            text = { Text("这段通信中的书信与待投递回信会被永久删除。也可以先归档，日后继续。") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    viewModel.delete(conversationId, onBack)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("保留") } }
        )
    }
}
fun formatLetterTime(value: String?): String = value?.let {
    runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M 月 d 日 HH:mm")) }.getOrDefault("")
}.orEmpty()
fun remainingTime(seconds: Long): String = when {
    seconds >= 86400 -> "约 " + seconds / 86400 + " 天 " + seconds % 86400 / 3600 + " 小时"
    seconds >= 3600 -> "约 " + seconds / 3600 + " 小时 " + seconds % 3600 / 60 + " 分钟"
    seconds >= 60 -> "约 " + seconds / 60 + " 分钟"
    else -> "不到一分钟"
}
