package com.example.myapplication.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Elderly
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class VisionHubDestination(
    val label: String,
    val icon: ImageVector,
    val showInBottomBar: Boolean = true,
) {
    HOME("首页", Icons.Outlined.Home),
    OBSTACLE("避障", Icons.Outlined.Warning),
    RECOGNITION("识别", Icons.Outlined.Visibility),
    DEVICE("设备", Icons.Outlined.Devices),
    PROFILE("我的", Icons.Outlined.AccountCircle),
    HISTORY("历史记录", Icons.Outlined.History, showInBottomBar = false),
    ELDERLY("老人信息", Icons.Outlined.Elderly, showInBottomBar = false),
}
