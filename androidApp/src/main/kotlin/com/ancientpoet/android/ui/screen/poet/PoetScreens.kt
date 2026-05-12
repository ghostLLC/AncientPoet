package com.ancientpoet.android.ui.screen.poet

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
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetListScreen(onPoetClick: (Long) -> Unit, onBack: () -> Unit, viewModel: PoetViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPoets() }

    Scaffold(topBar = { TopAppBar(title = { Text("诗人") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.poets) { poet ->
                ListItem(
                    headlineContent = { Text(poet.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${poet.dynastyName} · ${poet.birthYear}-${poet.deathYear}") },
                    modifier = Modifier.clickable { onPoetClick(poet.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetDetailScreen(poetId: Long, onBack: () -> Unit, onStartConversation: () -> Unit, viewModel: PoetViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(poetId) { viewModel.loadPoetDetail(poetId) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(state.selectedPoet?.name ?: "") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) },
        floatingActionButton = { FloatingActionButton(onClick = onStartConversation, containerColor = VermilionRed) { Text("书信", color = RicePaper) } }
    ) { padding ->
        state.selectedPoet?.let { poet ->
            Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${poet.dynastyName} · ${poet.courtesyName?.let { "字$it" } ?: ""}${poet.artName?.let { " 号$it" } ?: ""}", style = MaterialTheme.typography.titleMedium)
                Text(poet.biographySummary ?: "", style = MaterialTheme.typography.bodyLarge)
                Text("文风", fontWeight = FontWeight.Bold, color = VermilionRed)
                Text(poet.writingStyle)
            }
        }
    }
}
