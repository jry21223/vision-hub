package com.example.myapplication.auth

object AuthTokenHolder {
    @Volatile var token: String? = null
    @Volatile var userId: String = ""
}
