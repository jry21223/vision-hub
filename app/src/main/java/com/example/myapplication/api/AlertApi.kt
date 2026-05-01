package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AlertApi {
    // --- spec: POST /alarm/post (fall / obstacle shared endpoint) ---
    @POST("/alarm/post")
    suspend fun postAlarm(@Body req: AlarmPostRequest): Response<BaseResponse>

    // --- guardian-app: fetch alert history ---
    @GET("/api/v1/alerts")
    suspend fun getAlerts(
        @Query("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): Response<AlertsResponse>
}

// alarmType: "FALL" or "OBSTACLE"; angle/angleX/latitude/longitude are null for obstacle alarms
data class AlarmPostRequest(
    val deviceId: String,
    val alarmType: String,
    val angle: Float? = null,
    val angleX: Float? = null,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class AlertRecord(
    val id: String,
    val type: String,
    val timestamp: Long,
    val detail: String,
    val deviceId: String,
)

data class AlertsResponse(
    val success: Boolean,
    val message: String?,
    val data: List<AlertRecord>?,
)
