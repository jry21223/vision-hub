package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            requestCallPermissionIfNeeded()
        }

    private val requestCallPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startVisionHubService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startVisionHubServiceWithPermissionChecks()
        setContent {
            MyApplicationTheme {
                val fallAlertState by VisionDataHub.fallAlertState.collectAsState()
                val connectionState by VisionDataHub.connectionState.collectAsState()
                val localVisionState by VisionDataHub.localVisionState.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VisionHubScreen(
                        fallAlertState = fallAlertState,
                        connectionState = connectionState,
                        localVisionState = localVisionState,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun startVisionHubServiceWithPermissionChecks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (notificationPermissionState != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        requestCallPermissionIfNeeded()
    }

    private fun requestCallPermissionIfNeeded() {
        val callPermissionState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE,
        )
        if (callPermissionState == PackageManager.PERMISSION_GRANTED) {
            startVisionHubService()
            return
        }

        requestCallPermission.launch(Manifest.permission.CALL_PHONE)
    }

    private fun startVisionHubService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, VisionHubService::class.java),
        )
    }
}

@Composable
private fun VisionHubScreen(
    fallAlertState: FallAlertState,
    connectionState: ConnectionState,
    localVisionState: LocalVisionState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "视觉中枢",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = connectionStatusText(connectionState),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = localVisionStatusText(localVisionState),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = fallAlertTitle(fallAlertState),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = fallAlertDescription(fallAlertState),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center,
        )
    }
}

private fun connectionStatusText(state: ConnectionState): String {
    val label = when (state) {
        ConnectionState.STOPPED -> "已停止"
        ConnectionState.STARTING -> "启动中"
        ConnectionState.LISTENING -> "监听中"
        ConnectionState.CONNECTED -> "已连接"
        ConnectionState.ERROR -> "连接异常"
    }
    return "连接状态：$label"
}

private fun localVisionStatusText(state: LocalVisionState): String {
    return "本地视觉：${state.summary}"
}

private fun fallAlertTitle(state: FallAlertState): String {
    return when (state) {
        FallAlertState.IDLE -> "监测中"
        FallAlertState.DETECTING -> "疑似跌倒"
        FallAlertState.FALL_CONFIRMED -> "已确认跌倒"
        FallAlertState.EMERGENCY_CALLING -> "正在呼叫紧急联系人"
    }
}

private fun fallAlertDescription(state: FallAlertState): String {
    return when (state) {
        FallAlertState.IDLE -> "系统正在持续监听跌倒信号。"
        FallAlertState.DETECTING -> "检测到异常运动，正在确认是否发生跌倒。"
        FallAlertState.FALL_CONFIRMED -> "已检测到跌倒，请立即查看佩戴者状态。"
        FallAlertState.EMERGENCY_CALLING -> "系统正在尝试发起紧急呼叫。"
    }
}

@Preview(showBackground = true)
@Composable
fun VisionHubScreenPreview() {
    MyApplicationTheme {
        VisionHubScreen(
            fallAlertState = FallAlertState.IDLE,
            connectionState = ConnectionState.LISTENING,
            localVisionState = LocalVisionState.IDLE,
        )
    }
}
