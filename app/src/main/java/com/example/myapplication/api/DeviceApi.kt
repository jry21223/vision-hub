package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DeviceApi {
    // --- spec: POST /device/register ---
    @POST("/device/register")
    suspend fun registerDevice(@Body req: DeviceRegisterRequest): Response<BaseResponse>

    // --- spec: GET /device_bind ---
    @GET("/device_bind")
    suspend fun getDeviceBindStatus(@Query("deviceId") deviceId: String): Response<DeviceBindStatusResponse>

    // --- guardian-app device management ---
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

// spec /device/register request — token is HMAC-SHA256(SECRET_KEY, deviceId + ":" + displayCode)
data class DeviceRegisterRequest(
    val deviceId: String,
    val displayCode: String,
    val token: String,
)

data class DeviceBindStatusResponse(
    val success: Boolean,
    val bound: Boolean,
    val message: String?,
)

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
