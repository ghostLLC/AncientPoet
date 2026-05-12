package com.ancientpoet.android.ui.screen.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onBack: () -> Unit, viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMapData() }

    Scaffold(topBar = { TopAppBar(title = { Text("唐朝疆域图") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Draw simplified map — cities as dots, capitals as larger dots
                state.cities.forEach { city ->
                    val cx = (city.mapX / 2000f) * size.width
                    val cy = (city.mapY / 1500f) * size.height
                    val radius = if (city.isCapital) 8f else 5f
                    drawCircle(Color.Red, radius, Offset(cx, cy))
                }
                // Draw user-poet connection if available
                if (state.distanceKm > 0) {
                    drawLine(Color(0xFFC8A45C), Offset(size.width * 0.5f, size.height * 0.5f), Offset(size.width * 0.6f, size.height * 0.45f))
                }
            }
            if (state.distanceKm > 0) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = RicePaper)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("预估距离: ${state.distanceKm}km", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = VermilionRed)
                        Text("预估延迟: ${state.estimatedDelay}")
                    }
                }
            }
        }
    }
}
