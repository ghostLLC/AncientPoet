package com.ancientpoet.android.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ancientpoet.android.ui.screen.settings.DataNotice
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onBack: () -> Unit = {}, viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var understood by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf(false) }
    LaunchedEffect(state.verified) { if (state.verified) onLoginSuccess() }
    Scaffold(modifier = Modifier.imePadding(), topBar = {
        TopAppBar(
            title = { Text("登录鸿雁") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回浏览") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("给远方的古人，\n留一个回信地址。", style = MaterialTheme.typography.headlineMedium)
            Text(
                "登录后保存书信、草稿与落脚地。诗人介绍和诗词可直接浏览。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.development) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        "当前为开发环境。点击获取后使用测试验证码 123456，不会发送短信。",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it.filter(Char::isDigit).take(11)
                    code = ""
                },
                label = { Text("手机号") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("6 位验证码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { viewModel.sendSms(phone) },
                    enabled = phone.matches(Regex("1[3-9][0-9]{9}")) &&
                        !state.isLoading && !(state.sentPhone == phone && state.cooldown > 0),
                    modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp)
                ) {
                    Text(if (state.sentPhone == phone && state.cooldown > 0) state.cooldown.toString() + " 秒" else "获取验证码")
                }
            }
            if (state.sentPhone == phone) {
                Text(
                    if (state.development) "测试验证码已准备好" else "验证码已发送，有效期 5 分钟",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.errorMessage != null) Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            Row {
                Checkbox(modifier = Modifier.semantics { contentDescription = "已了解数据使用说明" }, checked = understood, onCheckedChange = { understood = it })
                Column {
                    Text("我已了解账号与书信的数据使用方式", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { notice = true }) { Text("阅读数据说明") }
                }
            }
            Button(
                onClick = { viewModel.verifySms(phone, code) },
                enabled = understood && !state.isLoading && code.length == 6 && state.sentPhone == phone,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(if (state.isLoading) "正在处理…" else "登录，开始写信")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("继续浏览诗人与诗词") }
        }
    }
    if (notice) {
        AlertDialog(
            onDismissRequest = { notice = false },
            title = { Text("数据说明") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { DataNotice() } },
            confirmButton = { TextButton(onClick = { notice = false }) { Text("知道了") } }
        )
    }
}
