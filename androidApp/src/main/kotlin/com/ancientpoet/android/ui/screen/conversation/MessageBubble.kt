package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ancientpoet.android.ui.theme.*

@Composable
fun MessageBubble(text: String, translation: String?, imageUrl: String?, isUser: Boolean) {
    var showTranslation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(12.dp).let {
                        if (isUser) it.copy(bottomEnd = RoundedCornerShape(0.dp))
                        else it.copy(bottomStart = RoundedCornerShape(0.dp))
                    }
                )
                .background(if (isUser) VermilionRed.copy(alpha = 0.08f) else RicePaper)
                .padding(12.dp)
        ) {
            Column {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "附画",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (text.isNotBlank()) {
                    Text(
                        text = if (showTranslation && translation != null) translation else text,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.8),
                        color = InkBlack,
                    )
                }
            }
        }

        if (!isUser && translation != null) {
            Text(
                text = if (showTranslation) "原文" else "白话",
                modifier = Modifier.clickable { showTranslation = !showTranslation }.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = VermilionRed.copy(alpha = 0.7f),
            )
        }
    }
}
