package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ancientpoet.android.ui.component.TranslationSeal
import com.ancientpoet.android.ui.theme.*

@Composable
fun MessageBubble(text: String, translation: String?, imageUrl: String?, isUser: Boolean) {
    var showTranslation by remember { mutableStateOf(false) }

    if (isUser) {
        // User message — right aligned, seal-style
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl, contentDescription = "附画",
                    modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 200.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(8.dp))
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    fontFamily = SerifFont,
                    fontSize = 18.sp,
                    lineHeight = 36.sp,
                    letterSpacing = 0.5.sp,
                    color = InkBlack.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 48.dp),
                )
            }
        }
    } else {
        // Poet message — left aligned with left border accent, translation seal
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            // Left accent line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(VermilionRed.copy(alpha = 0.12f))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl, contentDescription = "附画",
                        modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 200.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // Classical text
                if (text.isNotBlank()) {
                    Text(
                        text = if (showTranslation && translation != null) translation else text,
                        fontFamily = SerifFont,
                        fontSize = 18.sp,
                        lineHeight = 36.sp,
                        letterSpacing = 0.5.sp,
                        color = InkBlack.copy(alpha = 0.9f),
                    )
                }
                // Translation with seal button
                if (translation != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TranslationSeal(isActive = showTranslation, onToggle = { showTranslation = !showTranslation })
                        Text(
                            if (showTranslation) "白话译文" else "点击查看白话",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGray,
                        )
                    }
                }
            }
        }
    }
}
