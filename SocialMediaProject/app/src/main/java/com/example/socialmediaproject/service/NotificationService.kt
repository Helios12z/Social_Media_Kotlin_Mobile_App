package com.example.socialmediaproject.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.socialmediaproject.R

class NotificationService: Service() {
    companion object {
        const val CHANNEL_ID = "account_update_channel"
        const val NOTIF_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Thông báo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo kết quả"
            }
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(chan)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        when (intent.action) {
            ACTION.START.name -> {
                val content = intent.getStringExtra("content") ?: "Bắt đầu..."
                startInForeground(content)
            }
            ACTION.UPDATE.name -> {
                val content = intent.getStringExtra("content") ?: "Hoàn tất!"
                updateNotification(content)
            }
            ACTION.STOP.name -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    enum class ACTION {
        START,
        STOP,
        UPDATE
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Thông báo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Thông báo kết quả" }
            getSystemService<NotificationManager>()?.createNotificationChannel(chan)
        }
    }

    private fun startInForeground(content: String) {
        ensureChannel()
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.uploadicon)
            .setContentTitle("Thông báo")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        startForeground(NOTIF_ID, n)
    }

    private fun updateNotification(content: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.uploadicon)
            .setContentTitle("Thông báo")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        startForeground(NOTIF_ID, n)
    }
}