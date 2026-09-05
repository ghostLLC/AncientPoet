package com.ancientpoet.android.ui.screen.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.component.ApiErrorBanner
import com.ancientpoet.android.ui.screen.conversation.*
import com.ancientpoet.shared.contract.CityResponse
import com.ancientpoet.shared.util.*
import kotlin.math.hypot
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    initialDynasty: String = "tang",
    conversationId: Long? = null,
    showBack: Boolean = true,
    onLogin: () -> Unit = {},
    viewModel: MapViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var conversationMenu by remember { mutableStateOf(false) }
    LaunchedEffect(initialDynasty, conversationId) { viewModel.loadMapData(initialDynasty, conversationId) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(
            title = { Text("驿路") },
            navigationIcon = { if (showBack) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "更新旅途状态") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("此心安处，便是落脚地。", style = MaterialTheme.typography.headlineSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("tang" to "唐", "song" to "宋", "han" to "汉", "jin" to "晋", "ming" to "明").forEach { (id, name) ->
                        FilterChip(
                            selected = state.selectedDynasty == id,
                            onClick = { viewModel.loadMapData(id) },
                            label = { Text(name) },
                            enabled = !state.isMoving,
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                ApiErrorBanner(state.errorMessage, state.canRetry, viewModel::refresh)
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column {
                        Text(
                            "地理示意 · 非历史疆域图",
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RouteCanvas(state, viewModel::selectCity)
                        Text(
                            "红色圆点：我    方形：诗人    灰点：城市\n城市标签可点按，也可从下方列表选择。",
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                val movement = state.movement
                Text(
                    if (state.guest) {
                        "登录后，选择自己的落脚地"
                    } else if (movement?.status == "moving") {
                        "行途中 · " + movement.currentName + " → " + movement.movingToName
                    } else {
                        if (movement?.status == "settled") "我在" + movement.currentName else "还没有落脚地"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                if (movement?.status == "moving") {
                    Text(
                        "距抵达" + remainingTime(movement.remainingSeconds) + "。抵达后刷新即可安顿。",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.guest) Button(onClick = onLogin, modifier = Modifier.padding(top = 12.dp)) { Text("登录并选择落脚地") }
            }
            if (!state.guest) {
                item {
                    Box {
                        OutlinedButton(onClick = { conversationMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(state.conversation?.let { "与" + it.poet.name + "通信 · " + it.currentYear + " 年" } ?: "选择通信，查看收信人的位置")
                        }
                        DropdownMenu(expanded = conversationMenu, onDismissRequest = { conversationMenu = false }) {
                            DropdownMenuItem(text = { Text("只看城市") }, onClick = {
                                conversationMenu = false
                                viewModel.loadMapData(state.selectedDynasty)
                            })
                            state.conversations.forEach { conversation ->
                                DropdownMenuItem(
                                    text = { Text(conversation.poet.name + " · " + conversation.currentYear + " 年") },
                                    onClick = {
                                        conversationMenu = false
                                        viewModel.loadMapData(state.selectedDynasty, conversation.id)
                                    }
                                )
                            }
                            if (state.conversations.isEmpty()) DropdownMenuItem(text = { Text("这个朝代还没有通信") }, onClick = { conversationMenu = false }, enabled = false)
                        }
                    }
                    state.conversation?.poetLocation?.let { location ->
                        Text("收信人在" + location.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    state.delivery?.let { delivery ->
                        Text(
                            "相距约 " + delivery.distanceKm.toInt() + " 公里 · 现在寄信，预计 " + formatLetterTime(delivery.deliverAt) + " 收到回信",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            item { Text("选择城市", style = MaterialTheme.typography.titleLarge) }
            items(state.cities, key = { it.name }) { city ->
                ListItem(
                    headlineContent = { Text(city.name) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                city.modernName?.let { "今 " + it },
                                city.province
                            ).joinToString(" · ")
                        )
                    },
                    trailingContent = {
                        if (city.isCapital) {
                            Text(
                                "都城",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (state.selectedCity == city) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { viewModel.selectCity(city) }
                )
            }
        }
    }
    state.selectedCity?.let { city ->
        ModalBottomSheet(
            onDismissRequest = { if (!state.isMoving) viewModel.clearCity() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(city.name, style = MaterialTheme.typography.headlineMedium)
                Text(city.modernName?.let { "今 " + it } ?: "古地名资料", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val preview = state.preview
                if (state.guest) {
                    Button(onClick = {
                        viewModel.clearCity()
                        onLogin()
                    }, modifier = Modifier.fillMaxWidth()) { Text("登录并选择落脚地") }
                } else if (state.movement?.status == "moving") {
                    Text("当前旅途结束后，才能前往下一站。")
                } else {
                    if (preview == null && state.errorMessage == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    preview?.let {
                        Text(
                            if (it.firstLocation) {
                                "可立即在此落脚。回信会送到这个朝代的落脚地，其他朝代可以分别选择。"
                            } else if (it.travelSeconds == 0L) {
                                "你已经在这座城市附近。"
                            } else {
                                "相距约 " + it.distanceKm.toInt() + " 公里 · 旅途需" + remainingTime(it.travelSeconds) + "，途中不能改道。"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!it.firstLocation) {
                            Text(
                                "行旅速度按每天 50 公里估算，单次最多等待 7 天。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ApiErrorBanner(state.errorMessage, state.canRetry, { viewModel.selectCity(city) })
                    Button(
                        onClick = viewModel::moveTo,
                        enabled = preview != null && !state.isMoving &&
                            (preview.firstLocation || preview.travelSeconds > 0),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        Text(
                            if (state.isMoving) {
                                "正在安排行程…"
                            } else if (preview?.firstLocation == true) {
                                "确认在此落脚"
                            } else {
                                "确认启程"
                            }
                        )
                    }
                }
                TextButton(onClick = viewModel::clearCity, enabled = !state.isMoving, modifier = Modifier.fillMaxWidth()) { Text("返回驿路") }
            }
        }
    }
}

@Composable
private fun RouteCanvas(state: MapState, onCity: (CityResponse) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val movement = state.movement?.takeIf { it.status != "unknown" }
    val poet = state.conversation?.poetLocation
    val projection = remember(state.cities, movement, poet) {
        MapProjection(
            state.cities.map { GeographicPoint(it.lat, it.lng) } +
                listOfNotNull(movement?.let { GeographicPoint(it.currentLat, it.currentLng) }, poet?.let { GeographicPoint(it.lat, it.lng) })
        )
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurface)
    Canvas(
        Modifier.fillMaxWidth().height(280.dp).semantics {
            contentDescription = "城市位置示意。" + (movement?.let { "我在" + it.currentName + "。" } ?: "") + (poet?.let { "诗人在" + it.name + "。" } ?: "")
        }.pointerInput(state.cities, projection) {
            detectTapGestures { tap ->
                state.cities.minByOrNull { city ->
                    val p = projection.project(city.lat, city.lng, size.width.toDouble(), size.height.toDouble(), 36.dp.toPx().toDouble())
                    hypot(tap.x - p.x, tap.y - p.y)
                }?.let { city ->
                    val p = projection.project(city.lat, city.lng, size.width.toDouble(), size.height.toDouble(), 36.dp.toPx().toDouble())
                    if (hypot(tap.x - p.x, tap.y - p.y) <= 24.dp.toPx()) onCity(city)
                }
            }
        }
    ) {
        fun point(lat: Double, lng: Double) = projection.project(lat, lng, size.width.toDouble(), size.height.toDouble(), 36.dp.toPx().toDouble())
            .let { Offset(it.x.toFloat(), it.y.toFloat()) }
        repeat(5) { i ->
            val y = size.height * (i + 1) / 6
            drawLine(
                colors.outlineVariant.copy(alpha = 0.6f),
                Offset(12.dp.toPx(), y),
                Offset(size.width - 12.dp.toPx(), y),
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))
            )
        }
        if (movement != null && poet != null) {
            drawLine(
                colors.primary,
                point(movement.currentLat, movement.currentLng),
                point(poet.lat, poet.lng),
                1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
            )
        }
        state.cities.forEach { city ->
            val p = point(city.lat, city.lng)
            if (city == state.selectedCity) drawCircle(colors.primary, 12.dp.toPx(), p, style = Stroke(2.dp.toPx()))
            drawCircle(if (city.isCapital) colors.secondary else colors.onSurfaceVariant, 4.dp.toPx(), p)
        }
        movement?.let { drawCircle(colors.primary, 8.dp.toPx(), point(it.currentLat, it.currentLng)) }
        poet?.let {
            val p = point(it.lat, it.lng)
            drawRect(colors.tertiary, Offset(p.x - 5.dp.toPx(), p.y - 5.dp.toPx()), Size(10.dp.toPx(), 10.dp.toPx()))
        }
        val occupied = mutableListOf<Rect>()
        state.cities.sortedByDescending { it == state.selectedCity || it.isCapital }.forEach { city ->
            val p = point(city.lat, city.lng)
            val layout = textMeasurer.measure(city.name, style = labelStyle)
            val pos = Offset(
                (p.x + 8.dp.toPx()).coerceIn(4.dp.toPx(), (size.width - layout.size.width - 4.dp.toPx()).coerceAtLeast(4.dp.toPx())),
                (p.y - layout.size.height / 2).coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f))
            )
            val rect = Rect(pos, Size(layout.size.width.toFloat(), layout.size.height.toFloat()))
            if (occupied.none { it.overlaps(rect) }) {
                drawRect(colors.surfaceContainer.copy(alpha = 0.92f), pos, rect.size)
                drawText(layout, topLeft = pos)
                occupied += rect.inflate(2.dp.toPx())
            }
        }
    }
}
