package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AlertApi {
    @GET("/api/v1/alerts")
    suspend fun getAlerts(
        @Query("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): Response<AlertsResponse>
}

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
