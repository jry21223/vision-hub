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
    private val mutableConnectionState = MutableStateFlow(ConnectionState.STOPPED)
    private val mutableFallAlertState = MutableStateFlow(FallAlertState.IDLE)
    private val mutableLocalVisionState = MutableStateFlow(LocalVisionState.IDLE)
    private val mutableObstacleEnabled = MutableStateFlow(true)
    private val mutableFallConfig = MutableStateFlow(FallDetectionConfig.DEFAULT)

    val sensorPackets: SharedFlow<SensorPacket> = mutableSensorPackets.asSharedFlow()
    val imageFrames: SharedFlow<ByteArray> = mutableImageFrames.asSharedFlow()
    val connectionState: StateFlow<ConnectionState> = mutableConnectionState.asStateFlow()
    val fallAlertState: StateFlow<FallAlertState> = mutableFallAlertState.asStateFlow()
    val localVisionState: StateFlow<LocalVisionState> = mutableLocalVisionState.asStateFlow()
    val obstacleEnabled: StateFlow<Boolean> = mutableObstacleEnabled.asStateFlow()
    val fallConfig: StateFlow<FallDetectionConfig> = mutableFallConfig.asStateFlow()

    fun publishSensorPacket(packet: SensorPacket) {
        mutableSensorPackets.tryEmit(packet)
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
}
