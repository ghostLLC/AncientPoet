package com.ancientpoet.android.ui.screen.poetry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.component.ApiErrorBanner
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetryListScreen(poetId: Long? = null, onPoemClick: (Long) -> Unit, onBack: () -> Unit, viewModel: PoetryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable(poetId) { mutableStateOf("") }
    LaunchedEffect(query, poetId) { viewModel.search(query, poetId) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(
            title = { Text("诗词阁") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("读诗，也读一段心境。", style = MaterialTheme.typography.headlineSmall) }
            item {
                OutlinedTextField(
                    query,
                    { query = it.take(100) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (poetId == null) "搜索题目、诗句或作者" else "在这位诗人的作品中搜索") }
                )
                ApiErrorBanner(state.errorMessage, state.canRetry, { viewModel.search(query, poetId) })
                if (state.showingCache) Text("正在阅读本机缓存", style = MaterialTheme.typography.bodySmall)
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            if (state.poems.isEmpty() && !state.isLoading) item { Text("未找到匹配作品，试试其他诗句。") }
            items(state.poems, key = { it.id }) { poem ->
                Column(
                    Modifier.fillMaxWidth().clickable(onClickLabel = "阅读全文") { onPoemClick(poem.id) }.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(poem.title, style = MaterialTheme.typography.titleLarge)
                    Text(poem.poetName + " · " + poem.dynasty, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        poem.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (state.hasMore) {
                item {
                    TextButton(
                        onClick = { viewModel.search(query, poetId, true) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("读更多诗词") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetryDetailScreen(poemId: Long, onBack: () -> Unit, viewModel: PoetryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var showTranslation by rememberSaveable(poemId) { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(poemId) { viewModel.loadPoemDetail(poemId) }
    val poem = state.selectedPoem
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(
            title = { Text("诗词原作") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                ApiErrorBanner(state.errorMessage, state.canRetry, { viewModel.loadPoemDetail(poemId) })
                if (state.isLoading && poem == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (poem != null) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(poem.title, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                poem.poetName + " · " + poem.dynasty + (poem.yearWritten?.let { " · 公元 " + it + " 年" } ?: ""),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SelectionContainer { Text(poem.content, style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }
                if (!poem.translation.isNullOrBlank()) {
                    item {
                        TextButton(onClick = { showTranslation = !showTranslation }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showTranslation) "收起白话导读" else "读白话导读")
                        }
                        if (showTranslation) SelectionContainer { Text(poem.translation.orEmpty(), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                poem.context?.let { context ->
                    item {
                        Text("版本与背景", style = MaterialTheme.typography.titleLarge)
                        Text(context, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                poem.appreciation?.let { appreciation ->
                    item {
                        Text("慢读一会儿", style = MaterialTheme.typography.titleLarge)
                        Text(appreciation, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                    }
                }
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (poem.sourceUrl?.startsWith("https://") == true) {
                        TextButton(onClick = { runCatching { uriHandler.openUri(poem.sourceUrl.orEmpty()) }.onFailure { linkError = "无法打开浏览器，请稍后重试" } }) {
                            Text("查阅原文来源 · 维基文库")
                        }
                        Text(
                            "原作属于公有领域。白话导读与赏读为项目编写，不替代古籍校勘。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "这条旧版资料的来源仍待补充校订。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    linkError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
