package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceApi {
    @GET("/api/v1/devices/online")
    suspend fun listOnlineDevices(): Response<OnlineDevicesResponse>

    @POST("/api/v1/user/{userId}/devices/bind")
    suspend fun bindDevice(
        @Path("userId") userId: String,
        @Body req: BindDeviceRequest,
    ): Response<BaseResponse>

    @POST("/api/v1/user/{userId}/devices/unbind")
    suspend fun unbindDevice(
        @Path("userId") userId: String,
        @Body req: UnbindDeviceRequest,
    ): Response<BaseResponse>

    @GET("/api/v1/user/{userId}/devices/bound")
    suspend fun getBoundDevice(@Path("userId") userId: String): Response<BoundDeviceResponse>
}

data class OnlineDevice(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
)

data class OnlineDevicesResponse(
    val success: Boolean,
    val devices: List<OnlineDevice>?,
)

data class BindDeviceRequest(val deviceId: String)
data class UnbindDeviceRequest(val deviceId: String)

data class BoundDeviceResponse(
    val success: Boolean,
    val message: String?,
    val device: OnlineDevice?,
)
