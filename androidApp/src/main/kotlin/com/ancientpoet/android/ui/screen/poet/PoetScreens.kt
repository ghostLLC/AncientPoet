package com.ancientpoet.android.ui.screen.poet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.component.ApiErrorBanner
import org.koin.androidx.compose.koinViewModel

private val introductions = mapOf(
    "李白" to "山水、远行，与一场尽兴",
    "杜甫" to "人间烟火，家国与牵挂",
    "苏轼" to "在起落之间，安顿日常",
    "李清照" to "细看风物，也细说心事",
    "王维" to "把山中明月，写进信里"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoetListScreen(
    onPoetClick: (Long) -> Unit,
    onBack: () -> Unit,
    onPoetry: () -> Unit = {},
    showBack: Boolean = true,
    viewModel: PoetViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var dynasty by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.loadPoets() }
    val visible = state.poets.filter {
        (query.isBlank() || it.name.contains(query) || it.courtesyName.orEmpty().contains(query)) &&
            (dynasty.isEmpty() || it.dynastyId == dynasty)
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(
            title = { Text("诗人") },
            navigationIcon = { if (showBack) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = onPoetry) { Icon(Icons.AutoMirrored.Filled.MenuBook, "打开诗词阁") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("隔着千年，也能相知。", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "从诗作与生平，认识收信的人。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item { OutlinedTextField(query, { query = it.take(50) }, label = { Text("搜索姓名或字") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("" to "全部") + state.poets.map { it.dynastyId to it.dynastyName.orEmpty() }.distinct()).forEach { (id, name) ->
                        FilterChip(selected = dynasty == id, onClick = { dynasty = id }, label = { Text(name) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                ApiErrorBanner(state.errorMessage, state.canRetry, viewModel::loadPoets)
                if (state.isLoading && state.poets.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            items(visible, key = { it.id }) { poet ->
                Column(
                    Modifier.fillMaxWidth().clickable(onClickLabel = "了解" + poet.name) { onPoetClick(poet.id) }.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    poet.name.takeLast(1),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(poet.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                poet.dynastyName.orEmpty() + " · " + poet.birthYear + "—" + poet.deathYear,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        introductions[poet.name] ?: "走近" + poet.name + "的诗作与人生",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (visible.isEmpty() && !state.isLoading) item { Text("没有找到这位诗人，试试其他姓名或朝代。") }
            item { OutlinedButton(onClick = onPoetry, modifier = Modifier.fillMaxWidth()) { Text("去诗词阁，读一首原作") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PoetDetailScreen(
    poetId: Long,
    onBack: () -> Unit,
    onStartConversation: (Long, Int?) -> Unit,
    onPoetry: (Long) -> Unit = {},
    viewModel: PoetViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var year by rememberSaveable(poetId) { mutableStateOf<Int?>(null) }
    var showLife by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(poetId) { viewModel.loadPoetDetailInitial(poetId) }
    val poet = state.selectedPoet
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(
            title = { Text(poet?.name ?: "诗人小传") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }, bottomBar = {
        if (poet != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Button(onClick = { onStartConversation(poetId, year) }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).heightIn(min = 48.dp)) {
                    Text("写信给" + poet.name + (year?.let { " · " + it + " 年" } ?: ""))
                }
            }
        }
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ApiErrorBanner(state.errorMessage, state.canRetry, { viewModel.retryPoetDetail(poetId) })
                if (state.isLoading && poet == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (poet != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(poet.name, style = MaterialTheme.typography.displaySmall)
                        Text(
                            listOfNotNull(poet.dynastyName, poet.courtesyName?.let { "字" + it }, poet.artName?.let { "号" + it }).joinToString(" · "),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "公元 " + poet.birthYear + "—" + poet.deathYear + " 年",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(poet.biographySummary ?: "生平资料仍在整理中。", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("通信气质", style = MaterialTheme.typography.titleLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                poet.personalityProfile?.traits.orEmpty().forEach { trait ->
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                        Text(
                                            trait,
                                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Text(
                                "这些是书信角色的文学设定。AI 回信与下方可查阅的诗词原作分开呈现。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = { onPoetry(poetId) }, modifier = Modifier.fillMaxWidth()) { Text("读" + poet.name + "的诗词原作") }
                        }
                    }
                }
                item {
                    Text("从哪一年，开始相识？", style = MaterialTheme.typography.titleLarge)
                    state.location?.let { location ->
                        Text(
                            "默认从 " + location.year + " 年的" + location.locationName + " 开始。",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    TextButton(onClick = { showLife = !showLife }) { Text(if (showLife) "收起生平" else "翻阅生平，选择年代") }
                }
                if (showLife) {
                    item { OutlinedButton(onClick = { year = null }) { Text("使用默认年代") } }
                    items(state.lifeEvents.filter { it.year in poet.birthYear..poet.deathYear }, key = { it.id }) { event ->
                        Surface(
                            color = if (year == event.year) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().clickable { year = event.year }
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(event.year.toString() + " 年 · " + event.title, style = MaterialTheme.typography.titleMedium)
                                Text(event.description, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (year == event.year) "已选为通信年代" else "点按选择这一年",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
