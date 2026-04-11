package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.EmergencyContactConfig
import com.example.myapplication.VisionDataHub
import com.example.myapplication.ui.DangerRed
import com.example.myapplication.ui.ScreenBackground
import com.example.myapplication.ui.components.AppWordmark
import com.example.myapplication.ui.components.PillActionButton
import com.example.myapplication.ui.components.PrimaryListAction
import com.example.myapplication.ui.components.ProfileHeroCard
import com.example.myapplication.ui.components.SettingsGroupCard
import com.example.myapplication.util.ContactPreference

@Composable
internal fun ProfileScreen(
    onOpenHistory: () -> Unit,
    onVoiceSettings: () -> Unit = {},
    onObstacleSensitivity: () -> Unit = {},
    onModelConfig: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val emergencyContact by VisionDataHub.emergencyContact.collectAsStateWithLifecycle()
    var showEditContactDialog by remember { mutableStateOf(false) }

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
                            ContactPreference.save(context, trimmed)
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
            ProfileHeroCard()
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
                title = "紧急联系人：${emergencyContact.emergencyNumber}",
                icon = Icons.Filled.Phone,
                onClick = { showEditContactDialog = true },
            )
        }
        item {
            SettingsGroupCard(
                onVoiceSettings = onVoiceSettings,
                onObstacleSensitivity = onObstacleSensitivity,
                onModelConfig = onModelConfig,
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
