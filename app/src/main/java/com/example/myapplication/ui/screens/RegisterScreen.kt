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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.RegisterRequest
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.ui.ScreenBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun RegisterScreen(
    onRegisterSuccess: (token: String, userId: String, displayName: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
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
            text = "创建账号",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "注册暖阳守护账号",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; errorMessage = null },
            label = { Text("您的姓名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; errorMessage = null },
            label = { Text("手机号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text("确认密码") },
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
                when {
                    displayName.isBlank() -> errorMessage = "请填写您的姓名"
                    phone.isBlank() -> errorMessage = "请填写手机号"
                    password.length < 6 -> errorMessage = "密码至少6位"
                    password != confirmPassword -> errorMessage = "两次密码不一致"
                    else -> {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.authApi.register(
                                        RegisterRequest(
                                            phone = phone.trim(),
                                            password = password,
                                            displayName = displayName.trim(),
                                        )
                                    )
                                }
                                val body = response.body()
                                val serverMsg = body?.message
                                    ?: runCatching {
                                        response.errorBody()?.string()
                                            ?.substringAfter("\"message\":\"")
                                            ?.substringBefore("\"")
                                            ?.takeIf { it.isNotEmpty() }
                                    }.getOrNull()
                                if (response.isSuccessful && body?.success == true && body.token != null) {
                                    onRegisterSuccess(body.token, body.userId ?: "", body.displayName ?: displayName)
                                } else {
                                    errorMessage = serverMsg ?: "注册失败（HTTP ${response.code()}）"
                                }
                            } catch (e: Exception) {
                                errorMessage = "网络错误，请稍后重试"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (isLoading) "注册中…" else "注册")
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToLogin) { Text("已有账号？去登录") }
    }
}
