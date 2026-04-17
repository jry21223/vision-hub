package com.example.myapplication

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object VisionDataHub {
    private val mutableSensorPackets = MutableSharedFlow<SensorPacket>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableImageFrames = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableDeviceCommands = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableConnectionState = MutableStateFlow(ConnectionState.STOPPED)
    private val mutableFallAlertState = MutableStateFlow(FallAlertState.IDLE)
    private val mutableLocalVisionState = MutableStateFlow(LocalVisionState.IDLE)
    private val mutableObstacleEnabled = MutableStateFlow(true)
    private val mutableFallConfig = MutableStateFlow(FallDetectionConfig.DEFAULT)
    private val mutableEmergencyContact = MutableStateFlow(EmergencyContactConfig.DEFAULT)
    private val mutableUserProfile = MutableStateFlow(UserProfile())
    private val mutableAiServiceConfig = MutableStateFlow(AiServiceConfig.DEFAULT)
    private val mutableDeviceBattery = MutableStateFlow<Int?>(null)
    private val mutableRadarDistance = MutableStateFlow<Int?>(null)
    private val mutableRemoteDeviceIp = MutableStateFlow<String?>(null)
    private val mutableNetworkLatencyMs = MutableStateFlow<Int?>(null)

    val sensorPackets: SharedFlow<SensorPacket> = mutableSensorPackets.asSharedFlow()
    val imageFrames: SharedFlow<ByteArray> = mutableImageFrames.asSharedFlow()
    val deviceCommands: SharedFlow<String> = mutableDeviceCommands.asSharedFlow()
    val connectionState: StateFlow<ConnectionState> = mutableConnectionState.asStateFlow()
    val fallAlertState: StateFlow<FallAlertState> = mutableFallAlertState.asStateFlow()
    val localVisionState: StateFlow<LocalVisionState> = mutableLocalVisionState.asStateFlow()
    val obstacleEnabled: StateFlow<Boolean> = mutableObstacleEnabled.asStateFlow()
    val fallConfig: StateFlow<FallDetectionConfig> = mutableFallConfig.asStateFlow()
    val emergencyContact: StateFlow<EmergencyContactConfig> = mutableEmergencyContact.asStateFlow()
    val userProfile: StateFlow<UserProfile> = mutableUserProfile.asStateFlow()
    val aiServiceConfig: StateFlow<AiServiceConfig> = mutableAiServiceConfig.asStateFlow()
    val deviceBattery: StateFlow<Int?> = mutableDeviceBattery.asStateFlow()
    val radarDistance: StateFlow<Int?> = mutableRadarDistance.asStateFlow()
    val remoteDeviceIp: StateFlow<String?> = mutableRemoteDeviceIp.asStateFlow()
    val networkLatencyMs: StateFlow<Int?> = mutableNetworkLatencyMs.asStateFlow()

    fun publishSensorPacket(packet: SensorPacket) {
        mutableSensorPackets.tryEmit(packet)
        mutableRadarDistance.value = packet.radarDist
        mutableDeviceBattery.value = packet.batteryPct
    }

    fun publishImageFrame(jpegBytes: ByteArray) {
        mutableImageFrames.tryEmit(jpegBytes.copyOf())
    }

    fun updateConnectionState(state: ConnectionState) {
        if (mutableConnectionState.value != state) {
            mutableConnectionState.value = state
        }
    }

    fun updateFallAlertState(state: FallAlertState) {
        if (mutableFallAlertState.value != state) {
            mutableFallAlertState.value = state
        }
    }

    fun updateLocalVisionState(state: LocalVisionState) {
        if (mutableLocalVisionState.value != state) {
            mutableLocalVisionState.value = state
        }
    }

    fun setObstacleEnabled(enabled: Boolean) {
        mutableObstacleEnabled.value = enabled
    }

    fun updateFallConfig(config: FallDetectionConfig) {
        mutableFallConfig.value = config
    }

    fun updateEmergencyContact(config: EmergencyContactConfig) {
        mutableEmergencyContact.value = config
    }

    fun sendDeviceCommand(command: String) {
        mutableDeviceCommands.tryEmit(command)
    }

    fun sendDeviceCommand(command: DeviceCommand) {
        sendDeviceCommand(command.commandString)
    }

    fun updateUserProfile(profile: UserProfile) {
        mutableUserProfile.value = profile
    }

    fun updateAiServiceConfig(config: AiServiceConfig) {
        mutableAiServiceConfig.value = config
    }

    fun updateRemoteDeviceIp(ip: String?) {
        mutableRemoteDeviceIp.value = ip
    }

    fun updateNetworkLatencyMs(latencyMs: Int?) {
        mutableNetworkLatencyMs.value = latencyMs
    }

    fun clearConnectionRuntimeInfo() {
        mutableRadarDistance.value = null
        mutableRemoteDeviceIp.value = null
        mutableNetworkLatencyMs.value = null
        mutableDeviceBattery.value = null
    }
}

/**
 * Type-safe device commands sent to the ESP32 badge.
 */
enum class DeviceCommand(val commandString: String) {
    BUZZER_ON("BUZZER:ON"),
    FLASHLIGHT_TOGGLE("FLASHLIGHT:TOGGLE"),
}
