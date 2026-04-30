package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ElderlyApi {
    @POST("/api/v1/user/{userId}/elderly-profile")
    suspend fun saveElderlyProfile(
        @Path("userId") userId: String,
        @Body profile: ElderlyProfileRequest,
    ): Response<BaseResponse>

    @GET("/api/v1/user/{userId}/elderly-profile")
    suspend fun getElderlyProfile(@Path("userId") userId: String): Response<ElderlyProfileResponse>
}

data class ElderlyProfileRequest(
    val name: String,
    val age: Int,
    val gender: String,
    val phone: String,
    val medicalNotes: String,
)

data class ElderlyProfileResponse(
    val success: Boolean,
    val message: String?,
    val data: ElderlyProfileData?,
)

data class ElderlyProfileData(
    val name: String,
    val age: Int,
    val gender: String,
    val phone: String,
    val medicalNotes: String,
)
