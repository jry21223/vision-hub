package com.example.myapplication.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MediaApi {
    // spec: POST /upload/image — raw JPEG bytes, Content-Type: image/jpeg
    @Headers("Content-Type: image/jpeg")
    @POST("/upload/image")
    suspend fun uploadImage(@Body imageBytes: RequestBody): Response<BaseResponse>

    // spec: GET /get_text — returns latest OCR result from most recent image upload
    @GET("/get_text")
    suspend fun getOcrText(): Response<OcrTextResponse>

    // spec: POST /upload/audio — multipart/form-data, field name "audio"
    @Multipart
    @POST("/upload/audio")
    suspend fun uploadAudio(@Part audio: MultipartBody.Part): Response<BaseResponse>
}

data class OcrTextResponse(
    val text: String?,
    val timestamp: String?,
)
