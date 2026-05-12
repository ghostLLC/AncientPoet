package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.ancientpoet.android.ui.theme.InkBlack
import com.ancientpoet.android.ui.theme.RicePaper
import com.ancientpoet.android.ui.theme.VermilionRed

@Composable
fun MessageBubble(text: String, translation: String?, isUser: Boolean) {
    var showTranslation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp).let { if (isUser) it.copy(bottomEnd = RoundedCornerShape(0.dp)) else it.copy(bottomStart = RoundedCornerShape(0.dp)) })
                .background(if (isUser) VermilionRed.copy(alpha = 0.1f) else RicePaper)
                .padding(12.dp)
        ) {
            Text(if (showTranslation && translation != null) translation else text, style = MaterialTheme.typography.bodyLarge, color = InkBlack)
        }

        if (!isUser && translation != null) {
            Text(
                text = if (showTranslation) "原文" else "白话",
                modifier = Modifier.clickable { showTranslation = !showTranslation }.padding(4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = VermilionRed.copy(alpha = 0.7f),
            )
        }
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    androidx.compose.foundation.clickable { onClick() }
)
