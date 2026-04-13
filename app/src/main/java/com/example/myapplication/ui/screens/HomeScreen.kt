package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ConnectionState
import com.example.myapplication.FallAlertState
import com.example.myapplication.LocalVisionState
import com.example.myapplication.ui.PrimaryText
import com.example.myapplication.ui.ScreenBackground
import com.example.myapplication.ui.SecondaryText
import com.example.myapplication.ui.SuccessGreen
import com.example.myapplication.ui.SuccessText
import com.example.myapplication.ui.SurfaceSoft
import com.example.myapplication.ui.components.AppWordmark
import com.example.myapplication.ui.components.GiantActionCard
import com.example.myapplication.ui.components.StatusBanner
import com.example.myapplication.util.homeConnectionBannerSubtitle
import com.example.myapplication.util.homeConnectionBannerTitle
import com.example.myapplication.util.obstacleActionSubtitle
import com.example.myapplication.util.recognitionActionSubtitle

@Composable
internal fun HomeScreen(
    connectionState: ConnectionState,
    localVisionState: LocalVisionState,
    fallAlertState: FallAlertState,
    onOpenObstacle: () -> Unit,
    onOpenRecognition: () -> Unit,
    onEmergencyHelp: () -> Unit = {},
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
                title = "智视胸牌",
                subtitle = "暖阳语音助手已就绪",
            )
        }
        item {
            Text(
                text = "你好，有什么可以帮到您？请对我发出指令~",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = PrimaryText,
            )
        }
        item {
            GiantActionCard(
                title = "实时避障",
                subtitle = obstacleActionSubtitle(connectionState, fallAlertState),
                icon = Icons.Filled.Visibility,
                onClick = onOpenObstacle,
            )
        }
        item {
            GiantActionCard(
                title = "药品识别",
                subtitle = recognitionActionSubtitle(localVisionState),
                icon = Icons.Filled.Search,
                onClick = onOpenRecognition,
            )
        }
        item {
            val isConnected = connectionState == ConnectionState.CONNECTED
            StatusBanner(
                title = homeConnectionBannerTitle(connectionState),
                subtitle = homeConnectionBannerSubtitle(connectionState),
                icon = if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.Search,
                background = if (isConnected) SuccessGreen else SurfaceSoft,
                foreground = if (isConnected) SuccessText else SecondaryText,
            )
        }
    }
}
