package com.ancientpoet.android.ui.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onPostClick: (Long) -> Unit,
    onBack: () -> Unit,
    onProfileClick: (Long) -> Unit,
    viewModel: CommunityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPosts() }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("文苑", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
    ) { padding ->
        if (state.posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("文苑暂无文章", fontFamily = SerifFont, fontSize = 18.sp, color = WarmGray)
                    Spacer(Modifier.height(4.dp))
                    Text("分享你的书信对话，与同好共赏", style = MaterialTheme.typography.labelMedium, color = WarmGray.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(state.posts) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onPostClick(post.id) },
                        colors = CardDefaults.cardColors(containerColor = RicePaper),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(1.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // User header
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onProfileClick(post.userId) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = ImperialGold,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text((post.userNickname ?: "?").take(1), fontFamily = SerifFont, fontWeight = FontWeight.Bold, color = RicePaper, fontSize = 16.sp)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(post.userNickname ?: "匿名", fontFamily = SerifFont, fontWeight = FontWeight.Medium, color = InkBlack)
                            }
                            Spacer(Modifier.height(12.dp))
                            // Content
                            if (post.contentText != null) {
                                Text(post.contentText, fontFamily = SerifFont, fontSize = 16.sp, lineHeight = 28.sp, color = InkBlack)
                            }
                            // Shared messages preview
                            if (post.sharedMessageIds != null && post.sharedMessageIds.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = RicePaper, border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(ImperialGold.copy(alpha = 0.3f)))) {
                                    Text("📜 分享了 ${post.sharedMessageIds.size} 段对话", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = ImperialGold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Like + comment bar
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleLike(post.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        "赞", tint = if (post.isLiked) VermilionRed else WarmGray,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text("${post.likeCount}", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                                Text("${post.commentCount} 评论", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    viewModel: CommunityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(postId) { viewModel.loadComments(postId) }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("帖子详情", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText, onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("写下评论...", color = WarmGray) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ImperialGold, unfocusedBorderColor = WarmGray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(4.dp),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { viewModel.addComment(postId, commentText); commentText = "" }) {
                    Text("发送", color = VermilionRed)
                }
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(state.comments) { comment ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = RicePaper), shape = RoundedCornerShape(4.dp)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Surface(modifier = Modifier.size(24.dp), shape = RoundedCornerShape(4.dp), color = ImperialGold.copy(alpha = 0.5f)) {
                            Box(contentAlignment = Alignment.Center) { Text((comment.userNickname ?: "?").take(1), fontSize = 12.sp, color = RicePaper) }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(comment.userNickname ?: "匿名", style = MaterialTheme.typography.labelSmall, color = ImperialGold)
                            Text(comment.content, fontFamily = SerifFont, color = InkBlack)
                        }
                    }
                }
            }
        }
    }
}
