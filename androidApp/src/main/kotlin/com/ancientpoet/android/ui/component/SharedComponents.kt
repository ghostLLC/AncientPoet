package com.ancientpoet.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*

@Composable
fun LetterCard(
    poetName: String,
    dynasty: String,
    lastMessage: String,
    modifier: Modifier = Modifier,
    isUnread: Boolean = false,
    isWaiting: Boolean = false,
    estimatedArrival: String? = null,
) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RicePaper),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PoetAvatar(name = poetName)
            Column(modifier = Modifier.weight(1f)) {
                Text(poetName, fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkBlack)
                Text(dynasty, style = MaterialTheme.typography.labelMedium, color = WarmGray)
                if (lastMessage.isNotBlank()) {
                    Text(lastMessage, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = InkBlack.copy(alpha = 0.6f))
                }
                if (isWaiting && estimatedArrival != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("驿路传书中 · $estimatedArrival", style = MaterialTheme.typography.labelSmall, color = ImperialGold)
                }
            }
            // Status indicators
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(VermilionRed)
                        .align(Alignment.Top),
                )
            }
            if (isWaiting) {
                Text("✉️", fontSize = 16.sp, modifier = Modifier.align(Alignment.Top))
            }
        }
    }
}

@Composable
fun PoetAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ImperialGold),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1), fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = RicePaper)
    }
}

@Composable
fun TranslationSeal(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(32.dp),
        shape = RoundedCornerShape(2.dp),
        color = if (isActive) VermilionRed.copy(alpha = 0.1f) else RicePaper,
        border = if (isActive)
            ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(VermilionRed))
        else
            ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(WarmGray.copy(alpha = 0.3f))),
        onClick = onToggle,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "译",
                fontFamily = SerifFont,
                fontSize = 16.sp,
                color = if (isActive) VermilionRed else WarmGray,
            )
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ImperialGold)
    }
}
