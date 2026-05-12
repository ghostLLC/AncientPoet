package com.ancientpoet.android.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.*

@Composable
fun LetterCard(poetName: String, lastMessage: String, dynasty: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RicePaper)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PoetAvatar(name = poetName)
            Column(modifier = Modifier.weight(1f)) {
                Text(poetName, fontWeight = FontWeight.Bold, color = InkBlack)
                Text(dynasty, style = MaterialTheme.typography.labelMedium, color = WarmGray)
                if (lastMessage.isNotBlank()) Text(lastMessage, maxLines = 1, color = InkBlack.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun PoetAvatar(name: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = ImperialGold) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(name.take(1), fontWeight = FontWeight.Bold, color = RicePaper)
        }
    }
}

@Composable
fun TranslationToggle(isClassical: Boolean, onToggle: (Boolean) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("文言", style = MaterialTheme.typography.labelMedium, color = if (isClassical) VermilionRed else WarmGray)
        Switch(checked = !isClassical, onCheckedChange = { onToggle(!it) })
        Text("白话", style = MaterialTheme.typography.labelMedium, color = if (!isClassical) VermilionRed else WarmGray)
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        CircularProgressIndicator(color = VermilionRed)
    }
}
