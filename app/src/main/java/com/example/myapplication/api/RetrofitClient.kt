package com.example.myapplication.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 后端服务器配置
 * 注意：生产环境应使用 HTTPS
 */
object ApiConfig {
    const val BASE_URL = "http://47.94.146.53:3000"
}

/**
 * Retrofit 客户端提供者
 */
object RetrofitClient {

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        // 仅在 Debug 模式下启用日志
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val userProfileApi: UserProfileApi by lazy {
        retrofit.create(UserProfileApi::class.java)
    }
}

/**
 * BuildConfig stub for non-Android environments
 */
object BuildConfig {
    const val DEBUG = true
}
