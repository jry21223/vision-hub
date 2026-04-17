package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VisionHubService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var fallDetectionEngine = FallDetectionEngine()
    private val localVisionAnalyzer = LocalVisionAnalyzer()
    private val latencyMonitor = DeviceLatencyMonitor(scope = serviceScope)
    private val tcpServer = VisionTcpServer(
        scope = serviceScope,
        decoder = VisionStreamDecoder(),
    )

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        VisionDataHub.updateConnectionState(ConnectionState.STARTING)
        VisionDataHub.updateFallAlertState(FallAlertState.IDLE)
        VisionDataHub.updateLocalVisionState(LocalVisionState.IDLE)

        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            if (!isHeld) {
                acquire()
            }
        }
        observeFallConfig()
        observeSensorPackets()
        observeImageFrames()
        latencyMonitor.start()
        tcpServer.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildServiceNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                FOREGROUND_SERVICE_TYPES,
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        tcpServer.stop()
        latencyMonitor.stop()
        serviceScope.cancel()
        wakeLock?.let { currentWakeLock ->
            if (currentWakeLock.isHeld) {
                currentWakeLock.release()
            }
        }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun observeFallConfig() {
        serviceScope.launch {
            VisionDataHub.fallConfig.collect { newConfig ->
                fallDetectionEngine = FallDetectionEngine(config = newConfig)
            }
        }
    }

    private fun observeSensorPackets() {
        serviceScope.launch {
            VisionDataHub.sensorPackets.collect { packet ->
                val outcome = fallDetectionEngine.process(packet)
                val fallAlertState = when (outcome.state) {
                    FallDetectionState.IDLE -> FallAlertState.IDLE
                    FallDetectionState.DETECTING -> FallAlertState.DETECTING
                    FallDetectionState.FALL_CONFIRMED -> FallAlertState.FALL_CONFIRMED
                    FallDetectionState.COOLDOWN -> FallAlertState.EMERGENCY_CALLING
                }
                VisionDataHub.updateFallAlertState(fallAlertState)
                if (outcome.shouldTriggerEmergency) {
                    val handler = EmergencyCallHandler(config = VisionDataHub.emergencyContact.value)
                    val didStartCall = handler.triggerEmergencyCall(this@VisionHubService)
                    val nextState = if (didStartCall) {
                        FallAlertState.EMERGENCY_CALLING
                    } else {
                        FallAlertState.FALL_CONFIRMED
                    }
                    VisionDataHub.updateFallAlertState(nextState)
                }
            }
        }
    }

    private fun observeImageFrames() {
        serviceScope.launch {
            VisionDataHub.imageFrames.collect { frame ->
                if (!VisionDataHub.obstacleEnabled.value) return@collect
                VisionDataHub.updateLocalVisionState(LocalVisionState.PROCESSING)
                val result = localVisionAnalyzer.analyze(this@VisionHubService, frame)
                VisionDataHub.updateLocalVisionState(result)
            }
        }
    }

    companion object {
        private const val WAKE_LOCK_TAG = "com.example.myapplication:VisionHubWakeLock"
        private const val FOREGROUND_SERVICE_TYPES =
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }
}
