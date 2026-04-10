package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.DangerRed
import com.example.myapplication.ui.ScreenBackground
import com.example.myapplication.ui.components.AppWordmark
import com.example.myapplication.ui.components.PillActionButton
import com.example.myapplication.ui.components.PrimaryListAction
import com.example.myapplication.ui.components.ProfileHeroCard
import com.example.myapplication.ui.components.SettingsGroupCard

@Composable
internal fun ProfileScreen(
    onOpenHistory: () -> Unit,
    onVoiceSettings: () -> Unit = {},
    onObstacleSensitivity: () -> Unit = {},
    onModelConfig: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
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
