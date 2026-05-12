package com.ancientpoet.android.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*

@Composable
fun DrawingCanvasDialog(
    onConfirm: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var paths by remember { mutableStateOf(listOf<DrawPath>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var brushColor by remember { mutableStateOf(InkBlack) }
    var brushSize by remember { mutableStateOf(3f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("绘画", color = InkBlack) },
        text = {
            Column {
                // Brush controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("颜色:", style = MaterialTheme.typography.labelMedium, color = WarmGray)
                    listOf(InkBlack, VermilionRed, ImperialGold, JadeGreen, SkyBlue).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(color, RoundedCornerShape(12.dp))
                                .then(
                                    if (brushColor == color) Modifier.padding(2.dp).background(Color.Transparent, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .clickable { brushColor = color }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("粗细:", style = MaterialTheme.typography.labelMedium, color = WarmGray)
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 1f..10f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = VermilionRed, activeTrackColor = VermilionRed),
                    )
                }

                // Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            RicePaper,
                            RoundedCornerShape(8.dp)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val path = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = path
                                    currentPoints = listOf(offset)
                                    paths = paths + DrawPath(path, brushColor, brushSize, false)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val offset = change.position
                                    currentPath?.lineTo(offset.x, offset.y)
                                    currentPoints = currentPoints + offset
                                },
                                onDragEnd = {
                                    currentPath = null
                                },
                            )
                        }
                ) {
                    paths.forEach { drawPath ->
                        drawPath(
                            path = drawPath.path,
                            color = drawPath.color,
                            style = Stroke(
                                width = drawPath.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Convert canvas to bitmap bytes (stub — real impl needs Android Canvas bitmap)
                onConfirm(ByteArray(0))
                onDismiss()
            }) {
                Text("确认", color = VermilionRed)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { paths = paths.dropLast(1).let { if (it.size < paths.size) it else paths } }) {
                    Text("撤销", color = WarmGray)
                }
                TextButton(onClick = { paths = emptyList() }) {
                    Text("清除", color = WarmGray)
                }
            }
        },
        containerColor = RicePaper,
    )
}

private data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isComplete: Boolean,
)
