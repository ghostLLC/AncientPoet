package com.ancientpoet.android.ui.screen.poetry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetryListScreen(
    poetId: Long? = null,
    onPoemClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: PoetryViewModel = koinViewModel(),
) {
    var searchQuery by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(poetId, searchQuery) {
        if (poetId != null) viewModel.loadPoetPoems(poetId)
        else if (searchQuery.isNotBlank()) viewModel.searchPoems(searchQuery)
        else viewModel.loadAllPoems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (poetId != null) "诗词" else "诗词库", color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.searchPoems(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索诗词...", color = WarmGray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ImperialGold,
                    unfocusedBorderColor = WarmGray.copy(alpha = 0.3f),
                ),
            )

            if (state.poems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("暂无诗词", color = WarmGray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.poems) { poem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onPoemClick(poem.id) },
                            colors = CardDefaults.cardColors(containerColor = RicePaper),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = poem.title,
                                    fontWeight = FontWeight.Bold,
                                    color = InkBlack,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${poem.poetName} · ${poem.dynasty}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ImperialGold,
                                )
                                if (poem.preview.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = poem.preview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkBlack.copy(alpha = 0.7f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetryDetailScreen(
    poemId: Long,
    onBack: () -> Unit,
    viewModel: PoetryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showTranslation by remember { mutableStateOf(false) }

    LaunchedEffect(poemId) { viewModel.loadPoemDetail(poemId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedPoem?.title ?: "", color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        }
    ) { padding ->
        state.selectedPoem?.let { poem ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RicePaper),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = poem.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${poem.poetName} · ${poem.dynasty}${poem.yearWritten?.let { " · ${it}年" } ?: ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ImperialGold,
                        )
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = WarmGray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = if (showTranslation && poem.translation != null) poem.translation else poem.content,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 2.0),
                            color = InkBlack,
                        )
                    }
                }

                // Translation toggle
                if (poem.translation != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(onClick = { showTranslation = !showTranslation }) {
                            Text(
                                text = if (showTranslation) "查看原文" else "查看白话翻译",
                                color = VermilionRed,
                            )
                        }
                    }
                }

                // Context
                if (poem.context != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RicePaper),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("创作背景", fontWeight = FontWeight.Bold, color = VermilionRed, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(poem.context, style = MaterialTheme.typography.bodyMedium, color = InkBlack.copy(alpha = 0.8f))
                        }
                    }
                }

                // Appreciation
                if (poem.appreciation != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RicePaper),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("赏析", fontWeight = FontWeight.Bold, color = ImperialGold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(poem.appreciation, style = MaterialTheme.typography.bodyMedium, color = InkBlack.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("加载中...", color = WarmGray)
        }
    }
}
