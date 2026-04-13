package com.example.myapplication.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.EmergencyContactConfig
import com.example.myapplication.UserProfile
import com.example.myapplication.VisionDataHub
import com.example.myapplication.ui.DangerRed
import com.example.myapplication.ui.ScreenBackground
import com.example.myapplication.ui.components.AppWordmark
import com.example.myapplication.ui.components.PillActionButton
import com.example.myapplication.ui.components.PrimaryListAction
import com.example.myapplication.ui.components.ProfileHeroCard
import com.example.myapplication.ui.components.SettingsGroupCard
import com.example.myapplication.util.ContactPreference
import com.example.myapplication.util.AiServicePreference
import com.example.myapplication.util.UserPreference
import com.example.myapplication.AiServiceConfig
import com.example.myapplication.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileScreen(
    onOpenHistory: () -> Unit = {},
    onObstacleSensitivity: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val emergencyContact by VisionDataHub.emergencyContact.collectAsStateWithLifecycle()
    val userProfile by VisionDataHub.userProfile.collectAsStateWithLifecycle()
    val aiServiceConfig by VisionDataHub.aiServiceConfig.collectAsStateWithLifecycle()

    var showEditContactDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }

    // 紧急联系人编辑对话框
    if (showEditContactDialog) {
        var phoneNumber by rememberSaveable { mutableStateOf(emergencyContact.emergencyNumber) }
        AlertDialog(
            onDismissRequest = { showEditContactDialog = false },
            title = { Text("编辑紧急联系人") },
            text = {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("紧急联系电话") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = phoneNumber.trim()
                        if (trimmed.isNotBlank()) {
                            val newConfig = EmergencyContactConfig(emergencyNumber = trimmed)
                            VisionDataHub.updateEmergencyContact(newConfig)
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    ContactPreference.save(context, trimmed)
                                }
                            }
                        }
                        showEditContactDialog = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditContactDialog = false }) { Text("取消") }
            },
        )
    }

    // 用户档案编辑对话框
    if (showEditProfileDialog) {
        var displayName by rememberSaveable { mutableStateOf(userProfile.displayName) }
        var avatarInitial by rememberSaveable { mutableStateOf(userProfile.avatarInitial) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
            title = { Text("编辑个人资料") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("显示名称") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = avatarInitial,
                        onValueChange = {
                            if (it.length <= 1) avatarInitial = it
                        },
                        label = { Text("头像文字（单个字符）") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = displayName.trim()
                        if (trimmedName.isBlank()) return@TextButton

                        isSaving = true
                        scope.launch {
                            val newProfile = UserProfile(
                                userId = userProfile.userId,
                                displayName = trimmedName,
                                avatarInitial = avatarInitial.take(1),
                            )
                            VisionDataHub.updateUserProfile(newProfile)

                            // 本地保存到 SharedPreferences（IO 线程）
                            withContext(Dispatchers.IO) {
                                UserPreference.save(context, newProfile)
                            }

                            // 同步到后端
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.userProfileApi.updateUserProfile(
                                        userId = userProfile.userId,
                                        request = com.example.myapplication.UpdateProfileRequest(
                                            displayName = trimmedName,
                                            avatarInitial = avatarInitial.take(1),
                                        )
                                    )
                                }
                                if (response.isSuccessful && response.body()?.success == true) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "服务器同步失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("ProfileScreen", "Failed to sync profile to server", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "网络错误，已本地保存", Toast.LENGTH_SHORT).show()
                                }
                            }

                            isSaving = false
                            showEditProfileDialog = false
                        }
                    },
                    enabled = !isSaving && displayName.isNotBlank()
                ) { Text(if (isSaving) "保存中..." else "保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isSaving) showEditProfileDialog = false }
                ) { Text("取消") }
            },
        )
    }

    // AI 服务配置对话框
    if (showAiConfigDialog) {
        AiConfigDialog(
            initialConfig = aiServiceConfig,
            onDismiss = { showAiConfigDialog = false },
            onSave = { config ->
                VisionDataHub.updateAiServiceConfig(config)
                scope.launch {
                    withContext(Dispatchers.IO) {
                        AiServicePreference.save(context, config)
                    }
                }
                showAiConfigDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppWordmark(
                title = "语音助手已就绪",
                subtitle = "我的",
            )
        }
        item {
            ProfileHeroCard(
                displayName = userProfile.displayName.ifBlank { "点击设置" },
                userId = userProfile.userId.ifBlank { "未登录" },
                avatarInitial = userProfile.avatarInitial.ifBlank { "我" },
                onClick = { showEditProfileDialog = true },
            )
        }
        item {
            PrimaryListAction(
                title = "查看历史记录",
                icon = Icons.Filled.History,
                onClick = onOpenHistory,
            )
        }
        item {
            PrimaryListAction(
                title = "编辑个人资料",
                icon = Icons.Filled.Person,
                onClick = { showEditProfileDialog = true },
            )
        }
        item {
            PrimaryListAction(
                title = "紧急联系人：${emergencyContact.emergencyNumber}",
                icon = Icons.Filled.Phone,
                onClick = { showEditContactDialog = true },
            )
        }
        item {
            SettingsGroupCard(
                aiServiceStatus = aiServiceConfig.getDisplayStatus(),
                onAiSettings = { showAiConfigDialog = true },
                onObstacleSensitivity = onObstacleSensitivity,
            )
        }
        item {
            PillActionButton(
                label = "退出登录",
                icon = Icons.AutoMirrored.Filled.Logout,
                containerColor = DangerRed,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
                height = 74.dp,
                textStyle = MaterialTheme.typography.titleLarge,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun AiConfigDialog(
    initialConfig: AiServiceConfig,
    onDismiss: () -> Unit,
    onSave: (AiServiceConfig) -> Unit,
) {
    var selectedPreset by remember { mutableStateOf("custom") }
    var baseUrl by rememberSaveable { mutableStateOf(initialConfig.baseUrl) }
    var apiKey by rememberSaveable { mutableStateOf(initialConfig.apiKey) }
    var chatModel by rememberSaveable { mutableStateOf(initialConfig.chatModel) }
    var ttsModel by rememberSaveable { mutableStateOf(initialConfig.ttsModel) }
    var ttsVoice by rememberSaveable { mutableStateOf(initialConfig.ttsVoice) }
    var sttModel by rememberSaveable { mutableStateOf(initialConfig.sttModel) }
    var showAdvanced by remember { mutableStateOf(false) }

    // 当选择预设时更新配置
    fun applyPreset(preset: String) {
        selectedPreset = preset
        val config = when (preset) {
            "openai" -> AiServiceConfig.OPENAI
            "azure" -> AiServiceConfig.AZURE
            "dashscope" -> AiServiceConfig.DASHSCOPE
            "deepseek" -> AiServiceConfig.DEEPSEEK
            else -> return
        }
        baseUrl = config.baseUrl
        chatModel = config.chatModel
        ttsModel = config.ttsModel
        ttsVoice = config.ttsVoice
        sttModel = config.sttModel
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 服务配置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 预设选择（简化下拉菜单，使用文本按钮）
                Text(
                    text = "选择服务商预设",
                    style = MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.contentColorFor(MaterialTheme.colorScheme.surface)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("openai" to "OpenAI", "azure" to "Azure", "dashscope" to "阿里云", "deepseek" to "DeepSeek", "custom" to "自定义")
                        .forEach { (key, label) ->
                            TextButton(
                                onClick = { applyPreset(key) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedPreset == key) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                            }
                        }
                }

                // Base URL
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 高级配置展开/收起
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                ) {
                    Text(if (showAdvanced) "收起高级配置" else "展开高级配置")
                }

                // 高级配置
                if (showAdvanced) {
                    OutlinedTextField(
                        value = chatModel,
                        onValueChange = { chatModel = it },
                        label = { Text("对话模型") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = ttsModel,
                        onValueChange = { ttsModel = it },
                        label = { Text("语音合成模型") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = ttsVoice,
                        onValueChange = { ttsVoice = it },
                        label = { Text("语音音色") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = sttModel,
                        onValueChange = { sttModel = it },
                        label = { Text("语音识别模型") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AiServiceConfig(
                            baseUrl = baseUrl.trim(),
                            apiKey = apiKey.trim(),
                            chatModel = chatModel.trim(),
                            ttsModel = ttsModel.trim(),
                            ttsVoice = ttsVoice.trim(),
                            sttModel = sttModel.trim(),
                        )
                    )
                },
                enabled = baseUrl.isNotBlank() && apiKey.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
