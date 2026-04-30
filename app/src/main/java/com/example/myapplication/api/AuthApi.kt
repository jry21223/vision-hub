package com.example.myapplication.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<AuthResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>

    @POST("/api/v1/auth/reset-password")
    suspend fun resetPassword(@Body req: ResetPasswordRequest): Response<BaseResponse>

    @POST("/api/v1/auth/fcm-token")
    suspend fun uploadFcmToken(@Body req: FcmTokenRequest): Response<BaseResponse>
}

data class LoginRequest(val phone: String, val password: String)
data class RegisterRequest(val phone: String, val password: String, val displayName: String)
data class ResetPasswordRequest(val phone: String, val verifyCode: String, val newPassword: String)
data class FcmTokenRequest(val userId: String, val fcmToken: String)

data class AuthResponse(
    val success: Boolean,
    val message: String?,
    val token: String?,
    val userId: String?,
    val displayName: String?,
)

data class BaseResponse(val success: Boolean, val message: String?)
