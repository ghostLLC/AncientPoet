package com.ancientpoet.android.ui.screen.poet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.component.StorylineTimeline
import com.ancientpoet.android.ui.component.TimelineEvent
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetListScreen(onPoetClick: (Long) -> Unit, onBack: () -> Unit, viewModel: PoetViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPoets() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("诗人", color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.poets) { poet ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = RicePaper),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    ListItem(
                        headlineContent = { Text(poet.name, fontWeight = FontWeight.Bold, color = InkBlack) },
                        supportingContent = { Text("${poet.dynastyName} · ${poet.birthYear}-${poet.deathYear}", color = WarmGray) },
                        modifier = Modifier.clickable { onPoetClick(poet.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetDetailScreen(poetId: Long, onBack: () -> Unit, onStartConversation: (Long, Int?) -> Unit, viewModel: PoetViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var showStorylineMode by remember { mutableStateOf(false) }

    LaunchedEffect(poetId) { viewModel.loadPoetDetail(poetId); viewModel.loadLifeEvents(poetId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedPoet?.name ?: "", color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                // Storyline mode button
                if (showStorylineMode && selectedYear != null) {
                    FloatingActionButton(
                        onClick = { onStartConversation(poetId, selectedYear) },
                        containerColor = VermilionRed,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Text("从${selectedYear}年开始", color = RicePaper, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
                // Open chat button
                FloatingActionButton(
                    onClick = { onStartConversation(poetId, null) },
                    containerColor = VermilionRed,
                ) {
                    Text("书信", color = RicePaper)
                }
            }
        }
    ) { padding ->
        state.selectedPoet?.let { poet ->
            Column(
                modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Poet info card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = RicePaper),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${poet.dynastyName} · ${poet.courtesyName?.let { "字$it" } ?: ""}${poet.artName?.let { " 号$it" } ?: ""}", color = WarmGray, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(poet.biographySummary ?: "", color = InkBlack, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TextButton(onClick = { showStorylineMode = !showStorylineMode }) {
                                Text(if (showStorylineMode) "隐藏故事线" else "查看故事线", color = VermilionRed)
                            }
                        }
                    }
                }

                // Storyline timeline
                if (showStorylineMode && state.lifeEvents.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = RicePaper),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        StorylineTimeline(
                            birthYear = poet.birthYear,
                            deathYear = poet.deathYear,
                            currentYear = selectedYear ?: (poet.birthYear + 42),
                            lifeEvents = state.lifeEvents.map { TimelineEvent(it.year, it.title, it.description, it.eventType) },
                            onYearSelected = { year -> selectedYear = year },
                        )
                    }
                }

                // Writing style
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = RicePaper),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("文风", fontWeight = FontWeight.Bold, color = VermilionRed)
                        Spacer(Modifier.height(4.dp))
                        Text(poet.writingStyle, color = InkBlack)
                    }
                }
            }
        }
    }
}
