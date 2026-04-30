package com.example.myapplication.util

import android.content.Context

object AuthPreference {
    private const val PREFS_NAME = "visionhub_prefs"
    private const val KEY_JWT_TOKEN = "jwt_token"
    private const val KEY_FCM_TOKEN = "fcm_token"

    fun saveJwt(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun loadJwt(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JWT_TOKEN, null)

    fun clearJwt(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_JWT_TOKEN).apply()
    }

    fun saveFcmToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun loadFcmToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
}
