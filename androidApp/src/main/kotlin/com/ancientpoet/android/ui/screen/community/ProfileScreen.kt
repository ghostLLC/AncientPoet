package com.ancientpoet.android.ui.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Long,
    onBack: () -> Unit,
    viewModel: CommunityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.loadProfile(userId) }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text(state.profileName ?: "个人主页", fontFamily = SerifFont, color = InkBlack) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = InkBlack) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Avatar
            Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(12.dp), color = ImperialGold) {
                Box(contentAlignment = Alignment.Center) {
                    Text((state.profileName ?: "?").take(1), fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = RicePaper)
                }
            }
            // Name
            Text(state.profileName ?: "匿名", fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = InkBlack)
            state.profileBio?.let { bio ->
                Text(bio, fontFamily = SerifFont, fontSize = 15.sp, color = WarmGray)
            }
            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.postCount}", fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = VermilionRed)
                    Text("帖子", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("—", fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ImperialGold)
                    Text("收藏", style = MaterialTheme.typography.labelSmall, color = WarmGray)
                }
            }

            HorizontalDivider(color = WarmGray.copy(alpha = 0.2f))

            // Posts list (placeholder)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("暂无更多内容", fontFamily = SerifFont, color = WarmGray)
            }
        }
    }
}
