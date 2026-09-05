package com.ancientpoet.android.ui.screen.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.BuildConfig
import com.ancientpoet.android.data.*
import com.ancientpoet.shared.data.api.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    authenticated: Boolean,
    onLogin: () -> Unit,
    onSignedOut: () -> Unit,
    repository: AppRepository = koinInject(),
    preferences: ReadingPreferences = koinInject()
) {
    val options by preferences.options.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var deleteWord by remember { mutableStateOf("") }
    var logoutConfirmation by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(ArrivalNotifications.permitted(context)) }
    LifecycleResumeEffect(Unit) {
        allowed = ArrivalNotifications.permitted(context)
        onPauseOrDispose {}
    }
    fun toggleNotifications(value: Boolean) {
        preferences.notifications(value)
        ArrivalNotifications.configure(context, value)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted && ArrivalNotifications.permitted(context)
        toggleNotifications(allowed)
        if (!allowed) feedback = "通知未开启，你仍可以在收信匣查看回信。"
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                feedback = null
                try {
                    when (val result = repository.exportAccount()) {
                        is ApiResult.Success -> {
                            withContext(Dispatchers.IO) {
                                val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Unavailable document")
                                stream.bufferedWriter(Charsets.UTF_8).use { it.write(result.value.toString()) }
                            }
                            feedback = "书信与账号数据已导出。文件含私人内容，请妥善保管。"
                        }

                        is ApiResult.Failure -> feedback = result.message
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    feedback = "文件未能保存，请重新选择位置。"
                } finally {
                    busy = false
                }
            }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("我的") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("把书房，调成喜欢的样子。", style = MaterialTheme.typography.headlineSmall)
                Text("鸿雁 " + BuildConfig.VERSION_NAME, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("阅读外观", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "跟随系统", "light" to "日间", "dark" to "夜间").forEach { (value, label) ->
                        FilterChip(
                            selected = options.theme == value,
                            onClick = { preferences.theme(value) },
                            label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                Text("正文字号 · " + (options.textScale * 100).toInt() + "%")
                Slider(modifier = Modifier.semantics { contentDescription = "正文字号" }, value = options.textScale, onValueChange = preferences::textScale, valueRange = 0.9f..1.3f, steps = 3)
                Text("山中何所有，岭上多白云。", style = MaterialTheme.typography.bodyLarge)
                Text("字号会叠加系统字体设置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { HorizontalDivider() }
            item {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("回信提醒", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "约每 15 分钟检查，时间受系统后台调度影响；通知不展示正文。",
                            Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(modifier = Modifier.semantics { contentDescription = "回信提醒" }, checked = options.notifications && allowed, enabled = authenticated, onCheckedChange = { enabled ->
                        if (!enabled) {
                            toggleNotifications(false)
                        } else if (ArrivalNotifications.permitted(context)) {
                            allowed = true
                            toggleNotifications(true)
                        } else if (Build.VERSION.SDK_INT >= 33) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    })
                }
                if (!authenticated) Text("登录后可开启。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) { Text("系统通知设置") }
            }
            item { HorizontalDivider() }
            item {
                Text("账号与数据", style = MaterialTheme.typography.titleMedium)
                if (authenticated) {
                    OutlinedButton(onClick = { export.launch("鸿雁-个人数据.json") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("导出我的书信与账号数据") }
                    OutlinedButton(onClick = { logoutConfirmation = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
                    TextButton(onClick = { deleteConfirmation = true }, enabled = !busy) { Text("删除账号与服务器上的全部书信", color = MaterialTheme.colorScheme.error) }
                } else {
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("登录鸿雁") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                feedback?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            item { HorizontalDivider() }
            item {
                Text("数据说明", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                DataNotice()
            }
        }
    }
    if (logoutConfirmation) {
        AlertDialog(
            onDismissRequest = { logoutConfirmation = false },
            title = { Text("退出登录？") },
            text = { Text("本机草稿与离线书信按账号保存，重新登录后可继续阅读。离线退出只会清除本机登录信息。") },
            confirmButton = {
                TextButton(onClick = {
                    logoutConfirmation = false
                    busy = true
                    scope.launch {
                        repository.logout()
                        ArrivalNotifications.configure(context, false)
                        busy = false
                        onSignedOut()
                    }
                }) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { logoutConfirmation = false }) { Text("取消") } }
        )
    }
    if (deleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                deleteConfirmation = false
                deleteWord = ""
            },
            title = { Text("永久删除账号") },
            text = {
                Column {
                    Text("这会删除服务器上的账号、书信、旅途与本机该账号的离线副本，无法撤销。可先导出数据。输入“删除”以继续。")
                    OutlinedTextField(value = deleteWord, onValueChange = { deleteWord = it }, label = { Text("输入 删除") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = deleteWord == "删除" && !busy, onClick = {
                    busy = true
                    scope.launch {
                        when (val result = repository.deleteAccount()) {
                            is ApiResult.Success -> {
                                deleteConfirmation = false
                                ArrivalNotifications.configure(context, false)
                                onSignedOut()
                            }

                            is ApiResult.Failure -> {
                                feedback = result.message
                                deleteConfirmation = false
                            }
                        }
                        deleteWord = ""
                        busy = false
                    }
                }) { Text("永久删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = {
                    deleteConfirmation = false
                    deleteWord = ""
                }) { Text("取消") }
            }
        )
    }
}

@Composable
fun DataNotice() {
    Text(
        "手机号用于登录。书信、对话摘要和所选城市存储在当前服务端；城市是你在应用内选择的落脚地，应用不读取设备定位。\n\n" +
            "生成回信时，服务端会把当前书信、必要的历史上下文与角色背景发送给配置的 AI 服务。请避免填写身份证、密码或他人隐私。回信与白话译文为 AI 创作，不能视为史料或诗人本人的真实观点。\n\n" +
            "登录凭据使用 Android Keystore 加密保存。草稿与已读内容保存在应用私有目录，并按账号隔离；系统备份和设备迁移不包含这些数据。卸载会清除本机草稿。\n\n" +
            "你可以导出个人数据或删除账号。当前版本没有公开社区和附件分享。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
