package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.LoginRequest
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.ui.ScreenBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LoginScreen(
    onLoginSuccess: (token: String, userId: String, displayName: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "暖阳守护",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "老人健康安全守护平台",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 40.dp),
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; errorMessage = null },
            label = { Text("手机号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (phone.isBlank() || password.isBlank()) {
                    errorMessage = "请填写手机号和密码"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.authApi.login(LoginRequest(phone.trim(), password))
                        }
                        val body = response.body()
                        if (response.isSuccessful && body?.success == true && body.token != null) {
                            onLoginSuccess(body.token, body.userId ?: "", body.displayName ?: "")
                        } else {
                            errorMessage = body?.message ?: "登录失败，请检查手机号和密码"
                        }
                    } catch (e: Exception) {
                        errorMessage = "网络错误，请稍后重试"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (isLoading) "登录中…" else "登录")
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToRegister) { Text("还没有账号？立即注册") }
        TextButton(onClick = onNavigateToReset) { Text("忘记密码") }
    }
}
