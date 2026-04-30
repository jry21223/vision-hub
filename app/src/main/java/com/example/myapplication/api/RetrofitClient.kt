package com.example.myapplication.api

import com.example.myapplication.auth.AuthTokenHolder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    const val BASE_URL = "http://47.94.146.53:3000"
}

private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = AuthTokenHolder.token
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

object RetrofitClient {

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(AuthInterceptor())
        if (com.example.myapplication.BuildConfig.DEBUG) {
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

    val userProfileApi: UserProfileApi by lazy { retrofit.create(UserProfileApi::class.java) }
    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val elderlyApi: ElderlyApi by lazy { retrofit.create(ElderlyApi::class.java) }
    val deviceApi: DeviceApi by lazy { retrofit.create(DeviceApi::class.java) }
    val alertApi: AlertApi by lazy { retrofit.create(AlertApi::class.java) }

    fun createAiServiceApi(baseUrl: String): AiServiceApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(AiServiceApi::class.java)
    }
}
