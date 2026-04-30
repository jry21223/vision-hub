package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "vision_hub_service_channel"
    const val NOTIFICATION_ID = 1001
    const val ALERT_CHANNEL_ID = "vision_hub_alert_channel"
    const val ALERT_NOTIFICATION_ID = 1002

    private const val CHANNEL_NAME = "智能胸牌服务"
    private const val CHANNEL_DESCRIPTION = "保持智能胸牌实时连接，确保紧急情况下能快速响应"
    private const val NOTIFICATION_TITLE = "智能胸牌已连接"
    private const val NOTIFICATION_CONTENT = "您的智能守护已启动，点击可返回应用"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun createAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "紧急告警通知",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "跌倒、障碍、服药提醒等云端推送告警"
            enableVibration(true)
            enableLights(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showAlertNotification(context: Context, title: String, body: String) {
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(ALERT_NOTIFICATION_ID, notification)
    }

    fun buildServiceNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_CONTENT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
