package com.ancientpoet.android.ui.screen.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MessageBubble(text: String, translation: String?, imageUrl: String?, isUser: Boolean) {
    var translated by rememberSaveable { mutableStateOf(false) }
    Surface(
        color = if (isUser) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (isUser) "我寄出的信" else "收到回信 · AI 文学演绎",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
            SelectionContainer {
                Text(
                    if (translated && translation != null) translation else text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!isUser) {
                if (translation != null) {
                    TextButton(onClick = { translated = !translated }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (translated) "返回文言原信" else "读白话译文")
                    }
                } else {
                    Text(
                        "白话译文处理中，稍后自动更新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!imageUrl.isNullOrBlank()) Text("这封旧信含有附件，当前版本暂不展示", style = MaterialTheme.typography.bodySmall)
        }
    }
}
