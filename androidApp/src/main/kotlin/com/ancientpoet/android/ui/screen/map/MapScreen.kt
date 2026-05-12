package com.ancientpoet.android.ui.screen.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onBack: () -> Unit, viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var showDynastyMenu by remember { mutableStateOf(false) }
    val dynasties = listOf("tang" to "唐", "song" to "宋", "han" to "汉", "jin" to "晋", "ming" to "明")

    LaunchedEffect(Unit) { viewModel.loadMapData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${dynasties.find { it.first == state.selectedDynasty }?.second ?: "唐"}朝疆域图",
                            color = InkBlack,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box {
                            TextButton(onClick = { showDynastyMenu = true }) {
                                Text("切换 ▼", color = ImperialGold, style = MaterialTheme.typography.labelMedium)
                            }
                            DropdownMenu(expanded = showDynastyMenu, onDismissRequest = { showDynastyMenu = false }) {
                                dynasties.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text = { Text("${name}朝", color = if (id == state.selectedDynasty) VermilionRed else InkBlack) },
                                        onClick = { viewModel.loadMapData(id); showDynastyMenu = false },
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Map canvas with interactive cities
            Canvas(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                val w = size.width
                val h = size.height

                // Draw city dots
                state.cities.forEach { city ->
                    val cx = (city.mapX / 2000f) * w
                    val cy = (city.mapY / 1500f) * h
                    val isCapital = city.isCapital

                    // City marker
                    drawCircle(
                        color = if (isCapital) ImperialGold else Color(0xFF8B8178),
                        radius = if (isCapital) 8f else 5f,
                        center = Offset(cx, cy),
                    )
                    if (isCapital) {
                        drawCircle(
                            color = ImperialGold.copy(alpha = 0.3f),
                            radius = 14f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f),
                        )
                    }
                }

                // User position
                state.userLocation?.let { user ->
                    val ux = (user.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val uy = ((40.0 - user.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    drawCircle(VermilionRed, 10f, Offset(ux, uy))
                    drawCircle(VermilionRed.copy(alpha = 0.2f), 16f, Offset(ux, uy), style = Stroke(2f))
                }

                // Poet position
                state.poetLocation?.let { poet ->
                    val px = (poet.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val py = ((40.0 - poet.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    drawCircle(ImperialGold, 10f, Offset(px, py))
                }

                // Connection line
                if (state.userLocation != null && state.poetLocation != null) {
                    val uLoc = state.userLocation!!
                    val pLoc = state.poetLocation!!
                    val ux = (uLoc.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val uy = ((40.0 - uLoc.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    val px = (pLoc.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val py = ((40.0 - pLoc.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    drawLine(
                        color = ImperialGold.copy(alpha = 0.5f),
                        start = Offset(ux, uy),
                        end = Offset(px, py),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                    )
                }
            }

            // City list (tappable)
            LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp)) {
                items(state.cities) { city ->
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(city.name, fontWeight = FontWeight.Medium, color = InkBlack)
                                if (city.isCapital) {
                                    Text(" 都", color = ImperialGold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                        supportingContent = { Text(city.modernName ?: "", color = WarmGray) },
                        modifier = Modifier.clickable { viewModel.selectCity(city) },
                    )
                    HorizontalDivider(color = WarmGray.copy(alpha = 0.2f))
                }
            }

            // Info panel
            AnimatedVisibility(visible = state.selectedCity != null) {
                state.selectedCity?.let { city ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RicePaper),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(city.name, fontWeight = FontWeight.Bold, color = VermilionRed)
                                if (state.distanceKm > 0) {
                                    Text("${state.distanceKm}km", color = ImperialGold)
                                }
                            }
                            if (state.distanceKm > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("预估延迟: ${state.estimatedDelay}", color = WarmGray)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.moveTo(state.selectedDynasty, city) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ImperialGold),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("前往此地", color = RicePaper)
                            }
                        }
                    }
                }
            }
        }
    }
}
