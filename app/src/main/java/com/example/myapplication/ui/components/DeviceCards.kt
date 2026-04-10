package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ConnectionState
import com.example.myapplication.FallAlertState
import com.example.myapplication.ui.CardBackground
import com.example.myapplication.ui.PrimaryText
import com.example.myapplication.ui.SecondaryText
import com.example.myapplication.ui.StatMetric
import com.example.myapplication.ui.SuccessGreen
import com.example.myapplication.ui.SuccessText
import com.example.myapplication.ui.SurfaceSoft
import com.example.myapplication.ui.WarmYellow
import com.example.myapplication.ui.WarmYellowDark
import com.example.myapplication.util.finderSupportingText
import com.example.myapplication.util.guardianLocationText
import com.example.myapplication.util.guardianLocationUpdateText

@Composable
internal fun DeviceCoreCard(
    connectionState: ConnectionState,
    metrics: List<StatMetric>,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "主控制器",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = WarmYellowDark,
                    )
                    Text(
                        text = "ESP32-S3 核心",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = PrimaryText,
                    )
                }
                Surface(
                    color = SuccessGreen,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (connectionState == ConnectionState.CONNECTED)
                                        Color(0xFF54B35D) else WarmYellowDark
                                ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (connectionState == ConnectionState.CONNECTED) "已连接" else "待同步",
                            color = SuccessText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                metrics.forEach { metric ->
                    MetricCard(metric = metric, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun GuardianLocationCard(connectionState: ConnectionState) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WarmYellow),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = PrimaryText,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "监护人定位",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = PrimaryText,
                )
            }
            Card(
                modifier = Modifier.padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = guardianLocationText(connectionState),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = PrimaryText,
                    )
                    Text(
                        text = guardianLocationUpdateText(connectionState),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText.copy(alpha = 0.72f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryWideButton(
                    title = "地图查看",
                    icon = Icons.Filled.Map,
                    modifier = Modifier.weight(1f),
                    containerColor = PrimaryText,
                    contentColor = Color.White,
                )
                SecondaryWideButton(
                    title = "发送位置",
                    icon = Icons.Filled.LocationOn,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.White,
                    contentColor = PrimaryText,
                )
            }
        }
    }
}

@Composable
internal fun FinderCard(
    fallAlertState: FallAlertState,
    onBuzzer: () -> Unit = {},
    onFlashlight: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSoft),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "一键找寻",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = PrimaryText,
            )
            Text(
                text = finderSupportingText(fallAlertState),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FinderOptionCard(
                    title = "蜂鸣器",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    onClick = onBuzzer,
                    modifier = Modifier.weight(1f),
                )
                FinderOptionCard(
                    title = "灯光闪烁",
                    icon = Icons.Filled.FlashlightOn,
                    onClick = onFlashlight,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FinderOptionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WarmYellowDark,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = PrimaryText,
            )
        }
    }
}
