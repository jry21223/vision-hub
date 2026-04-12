package com.example.myapplication.api

import com.example.myapplication.UpdateProfileRequest
import com.example.myapplication.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 用户档案 API 服务
 */
interface UserProfileApi {

    /**
     * 获取用户档案
     */
    @GET("/api/v1/user/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<UserProfileResponse>

    /**
     * 更新用户档案
     */
    @POST("/api/v1/user/{userId}/profile")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest,
    ): Response<UserProfileResponse>
}
