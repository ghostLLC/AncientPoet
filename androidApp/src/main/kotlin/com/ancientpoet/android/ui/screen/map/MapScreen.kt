package com.ancientpoet.android.ui.screen.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dynasties.find { it.first == state.selectedDynasty }?.let { "${it.second}朝疆域图" } ?: "疆域图",
                            fontFamily = SerifFont, color = InkBlack,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box {
                            TextButton(onClick = { showDynastyMenu = true }) {
                                Text("切换 ▼", color = ImperialGold, style = MaterialTheme.typography.labelMedium)
                            }
                            DropdownMenu(expanded = showDynastyMenu, onDismissRequest = { showDynastyMenu = false }) {
                                dynasties.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text = { Text("${name}朝", fontFamily = SerifFont, color = if (id == state.selectedDynasty) VermilionRed else InkBlack) },
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
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(RicePaper.copy(alpha = 0.5f))) {
            // Map canvas
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f).background(RicePaper.copy(alpha = 0.3f))) {
                val w = size.width; val h = size.height
                state.cities.forEach { city ->
                    val cx = (city.mapX / 2000f) * w; val cy = (city.mapY / 1500f) * h
                    val isCapital = city.isCapital
                    // Glow behind capital cities
                    if (isCapital) {
                        drawCircle(ImperialGold.copy(alpha = 0.3f), 14f, Offset(cx, cy))
                        drawCircle(ImperialGold.copy(alpha = 0.15f), 22f, Offset(cx, cy))
                    }
                    drawCircle(if (isCapital) ImperialGold else WarmGray, if (isCapital) 6f else 4f, Offset(cx, cy))
                }
                // User position with pulse
                state.userLocation?.let { user ->
                    val ux = (user.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val uy = ((40.0 - user.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    drawCircle(VermilionRed, 9f, Offset(ux, uy))
                    drawCircle(VermilionRed.copy(alpha = 0.15f), 16f, Offset(ux, uy), style = Stroke(2f))
                }
                // Poet position with golden shadow
                state.poetLocation?.let { poet ->
                    val px = (poet.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val py = ((40.0 - poet.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    drawCircle(ImperialGold.copy(alpha = 0.3f), 14f, Offset(px, py))
                    drawCircle(ImperialGold, 9f, Offset(px, py))
                }
                // Connection dash line
                if (state.userLocation != null && state.poetLocation != null) {
                    val uLoc = state.userLocation!!; val pLoc = state.poetLocation!!
                    val ux = (uLoc.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val uy = ((40.0 - uLoc.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    val px = (pLoc.lng / 120.0 * w * 0.7f + w * 0.15f).coerceIn(0f, w)
                    val py = ((40.0 - pLoc.lat) / 20.0 * h * 0.7f + h * 0.15f).coerceIn(0f, h)
                    drawLine(WarmGray.copy(alpha = 0.4f), Offset(ux, uy), Offset(px, py), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                }
            }

            // City list
            LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 16.dp)) {
                items(state.cities) { city ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.selectCity(city) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(city.name, fontFamily = SerifFont, fontWeight = FontWeight.Medium, color = InkBlack)
                                if (city.isCapital) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(2.dp)).background(ImperialGold.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                        Text("都", fontSize = 10.sp, color = ImperialGold)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = WarmGray.copy(alpha = 0.1f))
                }
            }

            // Info panel — glass-morphism style
            AnimatedVisibility(visible = state.selectedCity != null) {
                state.selectedCity?.let { city ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = RicePaper.copy(alpha = 0.95f),
                        shadowElevation = 4.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(city.name, fontFamily = SerifFont, fontWeight = FontWeight.Bold, color = VermilionRed, fontSize = 18.sp)
                                if (state.distanceKm > 0) Text("${state.distanceKm}km", fontFamily = SerifFont, color = ImperialGold)
                            }
                            if (state.distanceKm > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("驿路传书约需 ${state.estimatedDelay}", style = MaterialTheme.typography.labelMedium, color = WarmGray)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.moveTo(state.selectedDynasty, city) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = VermilionRed),
                                shape = RoundedCornerShape(4.dp),
                            ) { Text("前往此地", color = OnPrimary) }
                        }
                    }
                }
            }
        }
    }
}
