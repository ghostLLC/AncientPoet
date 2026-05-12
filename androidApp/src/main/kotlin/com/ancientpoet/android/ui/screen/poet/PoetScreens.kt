package com.ancientpoet.android.ui.screen.poet

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("诗人", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.poets) { poet ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onPoetClick(poet.id) },
                    colors = CardDefaults.cardColors(containerColor = RicePaper),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    ListItem(
                        headlineContent = { Text(poet.name, fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = InkBlack) },
                        supportingContent = { Text("${poet.dynastyName} · ${poet.birthYear}-${poet.deathYear}", style = MaterialTheme.typography.labelMedium, color = WarmGray) },
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
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text(state.selectedPoet?.name ?: "", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showStorylineMode && selectedYear != null) {
                    FloatingActionButton(onClick = { onStartConversation(poetId, selectedYear) }, containerColor = VermilionRed, shape = RoundedCornerShape(4.dp)) {
                        Text("从${selectedYear}年开始", color = OnPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
                FloatingActionButton(onClick = { onStartConversation(poetId, null) }, containerColor = VermilionRed, shape = RoundedCornerShape(4.dp)) {
                    Text("研墨修书", color = OnPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        },
    ) { padding ->
        state.selectedPoet?.let { poet ->
            Column(
                modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${poet.dynastyName} · ${poet.courtesyName?.let { "字$it" } ?: ""}${poet.artName?.let { " 号$it" } ?: ""}", color = WarmGray, fontFamily = SerifFont, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(poet.biographySummary ?: "", color = InkBlack, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showStorylineMode = !showStorylineMode }) { Text(if (showStorylineMode) "隐藏故事线" else "查看故事线", color = VermilionRed) }
                    }
                }
                if (showStorylineMode && state.lifeEvents.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(8.dp)) {
                        StorylineTimeline(
                            birthYear = poet.birthYear, deathYear = poet.deathYear,
                            currentYear = selectedYear ?: (poet.birthYear + 42),
                            lifeEvents = state.lifeEvents.map { TimelineEvent(it.year, it.title, it.description, it.eventType) },
                            onYearSelected = { selectedYear = it },
                        )
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("文风", fontFamily = SerifFont, fontWeight = FontWeight.Bold, color = VermilionRed)
                        Spacer(Modifier.height(4.dp))
                        Text(poet.writingStyle, color = InkBlack, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
