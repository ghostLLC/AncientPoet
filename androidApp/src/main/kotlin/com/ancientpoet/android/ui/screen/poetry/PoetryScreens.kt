package com.ancientpoet.android.ui.screen.poetry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.theme.*
import com.ancientpoet.android.ui.component.ApiErrorBanner
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
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(poetId, searchQuery) {
        if (poetId != null) viewModel.loadPoetPoems(poetId)
        else if (searchQuery.isNotBlank()) viewModel.searchPoems(searchQuery)
        else viewModel.loadAllPoems()
    }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text(if (poetId != null) "诗词" else "诗词阁", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ApiErrorBanner(
                message = state.errorMessage,
                canRetry = state.canRetry,
                onRetry = { if (poetId != null) viewModel.loadPoetPoems(poetId) else viewModel.searchPoems(searchQuery) },
            )
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it; viewModel.searchPoems(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索诗词...", color = WarmGray, fontFamily = SerifFont) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ImperialGold, unfocusedBorderColor = WarmGray.copy(alpha = 0.3f), cursorColor = VermilionRed),
                shape = RoundedCornerShape(4.dp),
            )

            if (state.poems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无诗词", fontFamily = SerifFont, color = WarmGray) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.poems) { poem ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onPoemClick(poem.id) },
                            colors = CardDefaults.cardColors(containerColor = RicePaper),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(1.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(poem.title, fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkBlack)
                                Spacer(Modifier.height(2.dp))
                                Text("${poem.poetName} · ${poem.dynasty}", style = MaterialTheme.typography.labelMedium, color = ImperialGold)
                                if (poem.preview.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(poem.preview, fontFamily = SerifFont, fontSize = 15.sp, color = InkBlack.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
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
fun PoetryDetailScreen(poemId: Long, onBack: () -> Unit, viewModel: PoetryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTranslation by remember { mutableStateOf(false) }

    LaunchedEffect(poemId) { viewModel.loadPoemDetail(poemId) }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text(state.selectedPoem?.title ?: "", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
    ) { padding ->
        ApiErrorBanner(
            message = state.errorMessage,
            canRetry = state.canRetry,
            onRetry = { viewModel.loadPoemDetail(poemId) },
            modifier = Modifier.padding(padding),
        )
        state.selectedPoem?.let { poem ->
            Column(
                modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(poem.title, fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = InkBlack)
                        Spacer(Modifier.height(8.dp))
                        Text("${poem.poetName} · ${poem.dynasty}${poem.yearWritten?.let { " · ${it}年" } ?: ""}", style = MaterialTheme.typography.labelMedium, color = ImperialGold)
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = WarmGray.copy(alpha = 0.2f))
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (showTranslation && poem.translation != null) poem.translation else poem.content,
                            fontFamily = SerifFont,
                            fontSize = 18.sp,
                            lineHeight = 36.sp,
                            letterSpacing = 0.5.sp,
                            color = InkBlack,
                        )
                    }
                }

                if (poem.translation != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { showTranslation = !showTranslation }) {
                            Text(if (showTranslation) "查看原文" else "查看白话翻译", color = VermilionRed, fontFamily = SerifFont)
                        }
                    }
                }

                if (poem.context != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("创作背景", fontFamily = SerifFont, fontWeight = FontWeight.Bold, color = VermilionRed)
                            Spacer(Modifier.height(8.dp))
                            Text(poem.context, fontFamily = SerifFont, fontSize = 16.sp, color = InkBlack.copy(alpha = 0.8f))
                        }
                    }
                }

                if (poem.appreciation != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("赏析", fontFamily = SerifFont, fontWeight = FontWeight.Bold, color = ImperialGold)
                            Spacer(Modifier.height(8.dp))
                            Text(poem.appreciation, fontFamily = SerifFont, fontSize = 16.sp, color = InkBlack.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("加载中...", fontFamily = SerifFont, color = WarmGray)
        }
    }
}
