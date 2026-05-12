package com.ancientpoet.android.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.theme.InkBlack
import com.ancientpoet.android.ui.theme.VermilionRed
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = koinViewModel()) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.verified) { if (state.verified) onLoginSuccess() }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("鸿雁", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = VermilionRed)
        Text("AncientPoet", fontSize = 16.sp, color = InkBlack)
        Spacer(Modifier.height(48.dp))

        if (!codeSent) {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.sendSms(phone); codeSent = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VermilionRed)) {
                Text("发送验证码", color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("验证码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.verifySms(phone, code) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VermilionRed)) {
                Text("登录", color = MaterialTheme.colorScheme.onPrimary)
            }
            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
