package com.ancientpoet.android.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.theme.*

@Composable
fun StorylineTimeline(
    birthYear: Int,
    deathYear: Int,
    currentYear: Int,
    lifeEvents: List<TimelineEvent>,
    modifier: Modifier = Modifier,
    onYearSelected: ((Int) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "一生轨迹",
            style = MaterialTheme.typography.titleMedium,
            color = VermilionRed,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        val totalYears = (deathYear - birthYear).coerceAtLeast(1)
        val sorted = lifeEvents.sortedBy { it.year }

        sorted.forEachIndexed { index, event ->
            val isActive = event.year == currentYear
            val isHardship = event.eventType == "hardship" || event.eventType == "exile" || event.eventType == "war"
            val isAchievement = event.eventType == "achievement"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onYearSelected != null) Modifier.clickable { onYearSelected(event.year) }
                        else Modifier
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Year marker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text(
                        text = "${event.year}",
                        fontSize = 12.sp,
                        color = if (isActive) VermilionRed else WarmGray,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                    // Timeline dot and line
                    Box(
                        modifier = Modifier
                            .size(
                                if (isActive) 14.dp else 10.dp,
                                if (isActive) 14.dp else 10.dp
                            )
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val cx = size.width / 2
                            val cy = size.height / 2
                            val r = size.width / 2
                            drawCircle(
                                color = when {
                                    isActive -> VermilionRed
                                    isHardship -> VermilionRed.copy(alpha = 0.6f)
                                    isAchievement -> ImperialGold
                                    else -> WarmGray
                                },
                                radius = r,
                                center = Offset(cx, cy),
                            )
                            if (isActive) {
                                drawCircle(
                                    color = VermilionRed.copy(alpha = 0.3f),
                                    radius = r + 4f,
                                    center = Offset(cx, cy),
                                )
                            }
                        }
                    }
                    // Vertical connector line (except last)
                    if (index < sorted.size - 1) {
                        Canvas(modifier = Modifier.width(2.dp).height(24.dp)) {
                            drawLine(
                                color = WarmGray.copy(alpha = 0.4f),
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = 2f,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Event content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive -> VermilionRed
                            isHardship -> InkBlack.copy(alpha = 0.85f)
                            else -> InkBlack
                        },
                    )
                    if (isActive) {
                        Text(
                            text = "当前",
                            fontSize = 11.sp,
                            color = VermilionRed.copy(alpha = 0.7f),
                        )
                    }
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGray,
                        maxLines = if (isActive) Int.MAX_VALUE else 2,
                    )
                }
            }
        }
    }
}

data class TimelineEvent(
    val year: Int,
    val title: String,
    val description: String,
    val eventType: String,
)
