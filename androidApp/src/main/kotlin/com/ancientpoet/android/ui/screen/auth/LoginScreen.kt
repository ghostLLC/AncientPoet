package com.ancientpoet.android.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancientpoet.android.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, viewModel: LoginViewModel = koinViewModel()) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.verified) { if (state.verified) onLoginSuccess() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RicePaper)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Brand — 48sp serif, vermilion, matching DESIGN/_6
        Text(
            "鸿雁",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = VermilionRed,
            fontFamily = SerifFont,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "在快时代慢下来，像当时一样交流",
            fontSize = 14.sp,
            color = WarmGray,
            fontFamily = SansFont,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(64.dp))

        if (!codeSent) {
            // Phone input — underline style
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("手机号", color = WarmGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VermilionRed,
                    unfocusedBorderColor = WarmGray.copy(alpha = 0.3f),
                    focusedTextColor = InkBlack,
                    cursorColor = VermilionRed,
                ),
                shape = RoundedCornerShape(4.dp),
            )
            Spacer(Modifier.height(20.dp))
            // Seal-style button — square-ish, red border, no fill
            OutlinedButton(
                onClick = { viewModel.sendSms(phone); codeSent = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(2.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(VermilionRed)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VermilionRed),
            ) {
                Text("发送验证码", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = VermilionRed)
            }
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("验证码", color = WarmGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VermilionRed,
                    unfocusedBorderColor = WarmGray.copy(alpha = 0.3f),
                    focusedTextColor = InkBlack,
                    cursorColor = VermilionRed,
                ),
                shape = RoundedCornerShape(4.dp),
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { viewModel.verifySms(phone, code) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(2.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(VermilionRed)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VermilionRed),
            ) {
                Text("登录", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = VermilionRed)
            }
            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = ErrorColor, fontSize = 13.sp)
            }
        }
    }
}
