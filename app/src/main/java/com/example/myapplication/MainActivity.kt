package com.example.myapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.navigation.VisionHubDestination
import com.example.myapplication.ui.CardBackground
import com.example.myapplication.ui.HistoryRecord
import com.example.myapplication.ui.PrimaryText
import com.example.myapplication.ui.ScreenBackground
import com.example.myapplication.ui.SecondaryText
import com.example.myapplication.ui.SurfaceSoft
import com.example.myapplication.ui.WarmYellow
import com.example.myapplication.ui.components.PillActionButton
import com.example.myapplication.ui.screens.DeviceScreen
import com.example.myapplication.ui.screens.HistoryScreen
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.ObstacleScreen
import com.example.myapplication.ui.screens.ProfileScreen
import com.example.myapplication.ui.screens.RecognitionScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.SensitivityPreference
import com.example.myapplication.util.VolumePreference
import com.example.myapplication.util.connectionStatusText
import com.example.myapplication.util.fallAlertDescription
import com.example.myapplication.util.fallAlertTitle
import com.example.myapplication.util.filterHistoryRecords
import com.example.myapplication.util.guardianLocationText
import com.example.myapplication.util.historyDetail
import com.example.myapplication.util.historyTitle
import com.example.myapplication.util.obstacleGuidance
import android.util.Log

import com.example.myapplication.util.ContactPreference
import com.example.myapplication.util.UserPreference
import com.example.myapplication.util.AiServicePreference
import com.example.myapplication.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startVisionHubService()
        requestNotificationPermissionIfNeeded()
        setContent {
            MyApplicationTheme {
                val fallAlertState by VisionDataHub.fallAlertState.collectAsStateWithLifecycle()
                val connectionState by VisionDataHub.connectionState.collectAsStateWithLifecycle()
                val localVisionState by VisionDataHub.localVisionState.collectAsStateWithLifecycle()
                val obstacleEnabled by VisionDataHub.obstacleEnabled.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VisionHubScreen(
                        fallAlertState = fallAlertState,
                        connectionState = connectionState,
                        localVisionState = localVisionState,
                        obstacleEnabled = obstacleEnabled,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startVisionHubService() {
        ContextCompat.startForegroundService(this, Intent(this, VisionHubService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisionHubScreen(
    fallAlertState: FallAlertState,
    connectionState: ConnectionState,
    localVisionState: LocalVisionState,
    obstacleEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fallConfig by VisionDataHub.fallConfig.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // 加载紧急联系人（SharedPreferences 在 IO 线程读取）
        val savedContact = withContext(Dispatchers.IO) {
            ContactPreference.load(context)
        }
        VisionDataHub.updateEmergencyContact(savedContact)

        // 加载用户档案（SharedPreferences 在 IO 线程读取）
        val profile = withContext(Dispatchers.IO) {
            var p = UserPreference.load(context)
            if (p.userId.isBlank()) {
                p = p.copy(userId = UserPreference.generateUserId())
                UserPreference.save(context, p)
            }
            p
        }
        VisionDataHub.updateUserProfile(profile)

        // 加载 AI 服务配置
        val aiConfig = withContext(Dispatchers.IO) {
            AiServicePreference.load(context)
        }
        VisionDataHub.updateAiServiceConfig(aiConfig)

        // 尝试从后端同步用户档案（仅在需要时）
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.userProfileApi.getUserProfile(profile.userId)
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        val serverProfile = UserProfile(
                            userId = profile.userId,
                            displayName = body.data.displayName,
                            avatarInitial = body.data.avatarInitial,
                        )
                        VisionDataHub.updateUserProfile(serverProfile)
                        withContext(Dispatchers.IO) {
                            UserPreference.save(context, serverProfile)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to sync user profile from server", e)
            }
        }
    }

    var currentDestination by rememberSaveable { mutableStateOf(VisionHubDestination.HOME) }
    var volume by rememberSaveable { mutableFloatStateOf(VolumePreference.load(context)) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showSensitivitySheet by remember { mutableStateOf(false) }
    var historyQuery by rememberSaveable { mutableStateOf("") }
    var speechLaunchError by rememberSaveable { mutableStateOf<String?>(null) }

    var ttsEngine: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
            }
        }
        ttsEngine = tts
        onDispose { tts?.shutdown() }
    }

    var captureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            captureUri?.let { uri ->
                coroutineScope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes()
                        }
                    }
                    if (bytes != null) VisionDataHub.publishImageFrame(bytes)
                }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File.createTempFile("capture_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            captureUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) EmergencyCallHandler(config = VisionDataHub.emergencyContact.value).triggerEmergencyCall(context)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val query = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (query.isNotBlank()) {
            historyQuery = query
            currentDestination = VisionHubDestination.HISTORY
        }
    }

    val historyRecords = remember(fallAlertState, connectionState, localVisionState) {
        listOf(
            HistoryRecord(
                title = historyTitle(localVisionState, connectionState),
                detail = historyDetail(localVisionState),
                time = "今天 10:30",
                accent = androidx.compose.ui.graphics.Color(0xFFF6D68B),
            ),
            HistoryRecord(
                title = connectionStatusText(connectionState),
                detail = "视觉引导系统正在持续同步设备链路状态",
                time = "昨天 15:45",
                accent = androidx.compose.ui.graphics.Color(0xFFD2EBCF),
            ),
            HistoryRecord(
                title = fallAlertTitle(fallAlertState),
                detail = fallAlertDescription(fallAlertState),
                time = "10月24日 08:20",
                accent = androidx.compose.ui.graphics.Color(0xFFF2C4BE),
            ),
            HistoryRecord(
                title = "药品识别",
                detail = "识别摘要：${localVisionState.summary}",
                time = "10月23日 19:12",
                accent = androidx.compose.ui.graphics.Color(0xFFC9DBFF),
            ),
            HistoryRecord(
                title = "避障播报",
                detail = obstacleGuidance(connectionState, fallAlertState),
                time = "10月22日 09:00",
                accent = androidx.compose.ui.graphics.Color(0xFFD6F1EE),
            ),
            HistoryRecord(
                title = "家属守护",
                detail = guardianLocationText(connectionState),
                time = "10月21日 20:30",
                accent = androidx.compose.ui.graphics.Color(0xFFE4D6FF),
            ),
        )
    }
    val filteredHistoryRecords = remember(historyRecords, historyQuery) {
        filterHistoryRecords(historyRecords, historyQuery)
    }

    val onEmergencyHelp: () -> Unit = { showEmergencyDialog = true }

    val onVolumeChange: (Float) -> Unit = { newValue ->
        volume = newValue
        VolumePreference.save(context, newValue)
        val am = context.getSystemService(AudioManager::class.java)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (newValue * maxVol).roundToInt(), 0)
    }

    val onCaptureClick: () -> Unit = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            val file = File.createTempFile("capture_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            captureUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onSpeakResult: () -> Unit = {
        val result = localVisionState.result
        if (result != null && result.hasMedicineBox) {
            // 判断远近模式：面积比例 > 10% 认为是近景（细看模式）
            val isCloseUp = (result.medicineBoxAreaRatio ?: 0f) > 0.10f

            val ttsText = if (isCloseUp) {
                // 细看模式：播报药盒位置 + OCR 文字
                if (result.hasOcrContent) {
                    "药盒位于${result.medicineBoxLocation}，文字内容：${result.ocrText}"
                } else {
                    "药盒位于${result.medicineBoxLocation}，暂未识别到文字"
                }
            } else {
                // 找寻模式：只播报药盒位置
                "药盒位于${result.medicineBoxLocation}"
            }
            ttsEngine?.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            // 没有检测到药盒，播报默认摘要
            ttsEngine?.speak(localVisionState.summary, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val onVoiceInput: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出要查询的药品或日期")
        }
        try {
            speechLaunchError = null
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            speechLaunchError = "当前设备不支持语音识别"
        }
    }

    if (speechLaunchError != null) {
        AlertDialog(
            onDismissRequest = { speechLaunchError = null },
            title = { Text("无法启动语音输入") },
            text = { Text(speechLaunchError ?: "") },
            confirmButton = {
                TextButton(onClick = { speechLaunchError = null }) { Text("知道了") }
            },
        )
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("确认紧急求助") },
            text = { Text("即将拨打紧急联系人电话，确认继续？") },
            confirmButton = {
                TextButton(onClick = {
                    showEmergencyDialog = false
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CALL_PHONE,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        EmergencyCallHandler(config = VisionDataHub.emergencyContact.value).triggerEmergencyCall(context)
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }) { Text("确认拨打") }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) { Text("取消") }
            },
        )
    }

    if (showSensitivitySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSensitivitySheet = false },
            sheetState = sheetState,
        ) {
            SensitivitySheetContent(
                current = fallConfig,
                onSelect = { newConfig ->
                    VisionDataHub.updateFallConfig(newConfig)
                    SensitivityPreference.save(context, newConfig)
                    showSensitivitySheet = false
                },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScreenBackground,
        bottomBar = {
            NavigationBar(containerColor = CardBackground, tonalElevation = 0.dp) {
                VisionHubDestination.entries
                    .filter { it.showInBottomBar }
                    .forEach { destination ->
                        val selected = currentDestination == destination ||
                            (destination == VisionHubDestination.RECOGNITION &&
                                currentDestination == VisionHubDestination.HISTORY)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentDestination = destination },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryText,
                                selectedTextColor = PrimaryText,
                                indicatorColor = WarmYellow,
                                unselectedIconColor = SecondaryText,
                                unselectedTextColor = SecondaryText,
                            ),
                        )
                    }
            }
        },
    ) { innerPadding ->
        when (currentDestination) {
            VisionHubDestination.HOME -> HomeScreen(
                connectionState = connectionState,
                localVisionState = localVisionState,
                fallAlertState = fallAlertState,
                onOpenObstacle = { currentDestination = VisionHubDestination.OBSTACLE },
                onOpenRecognition = { currentDestination = VisionHubDestination.RECOGNITION },
                onEmergencyHelp = onEmergencyHelp,
                modifier = Modifier.padding(innerPadding),
            )
            VisionHubDestination.OBSTACLE -> ObstacleScreen(
                connectionState = connectionState,
                fallAlertState = fallAlertState,
                obstacleEnabled = obstacleEnabled,
                volume = volume,
                onToggleObstacle = { VisionDataHub.setObstacleEnabled(!obstacleEnabled) },
                onVolumeChange = onVolumeChange,
                onEmergencyHelp = onEmergencyHelp,
                onSensitivityClick = { showSensitivitySheet = true },
                modifier = Modifier.padding(innerPadding),
            )
            VisionHubDestination.RECOGNITION -> RecognitionScreen(
                localVisionState = localVisionState,
                onOpenHistory = {
                    historyQuery = ""
                    currentDestination = VisionHubDestination.HISTORY
                },
                onCaptureClick = onCaptureClick,
                onSpeakResult = onSpeakResult,
                modifier = Modifier.padding(innerPadding),
            )
            VisionHubDestination.DEVICE -> DeviceScreen(
                connectionState = connectionState,
                fallAlertState = fallAlertState,
                onBuzzer = {
                    ttsEngine?.speak("正在发出蜂鸣提示", TextToSpeech.QUEUE_FLUSH, null, null)
                },
                onFlashlight = {
                    ttsEngine?.speak("手电筒已开启", TextToSpeech.QUEUE_FLUSH, null, null)
                },
                modifier = Modifier.padding(innerPadding),
            )
            VisionHubDestination.PROFILE -> ProfileScreen(
                onOpenHistory = {
                    historyQuery = ""
                    currentDestination = VisionHubDestination.HISTORY
                },
                onObstacleSensitivity = { showSensitivitySheet = true },
                onLogout = { /* future: user session management */ },
                modifier = Modifier.padding(innerPadding),
            )
            VisionHubDestination.HISTORY -> HistoryScreen(
                historyRecords = filteredHistoryRecords,
                historyQuery = historyQuery,
                onBack = { currentDestination = VisionHubDestination.PROFILE },
                onOpenObstacle = { currentDestination = VisionHubDestination.OBSTACLE },
                onVoiceInput = onVoiceInput,
                onClearQuery = { historyQuery = "" },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SensitivitySheetContent(
    current: FallDetectionConfig,
    onSelect: (FallDetectionConfig) -> Unit,
) {
    val options = listOf(
        "低灵敏度（较难触发）" to SensitivityPreference.LOW,
        "中灵敏度（默认）" to SensitivityPreference.MEDIUM,
        "高灵敏度（较易触发）" to SensitivityPreference.HIGH,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "跌倒检测灵敏度",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = PrimaryText,
        )
        options.forEach { (label, config) ->
            val isSelected = current == config
            PillActionButton(
                label = label,
                icon = Icons.Filled.Tune,
                containerColor = if (isSelected) WarmYellow else SurfaceSoft,
                contentColor = PrimaryText,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(config) },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
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
            obstacleEnabled = true,
        )
    }
}
