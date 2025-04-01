package com.example.socialmediaproject.service

import android.util.Log
import com.example.socialmediaproject.dataclass.NotificationContent
import com.example.socialmediaproject.dataclass.NotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OneSignalService {
    @Headers(
        "Authorization: Basic os_v2_app_4nkobofcfndgfbuwnusdd5yzdt63swgsa7ge6ivfyvrpuehjncx4yb2e2jvkuez6qqe36fc25lgolfu7dqocg3m66on2z35kdxtdnyq",
        "Content-Type: application/json"
    )
    @POST("notifications")
    suspend fun sendNotification(@Body payload: NotificationPayload): retrofit2.Response<ResponseBody>
}

fun sendPushNotification(userId: String, message: String) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://onesignal.com/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(OneSignalService::class.java)
    val payload = NotificationPayload(
        appId = "e354e0b8-a22b-4662-8696-6d2431f7191c",
        includedExternalUserIds = listOf(userId),
        contents = NotificationContent(en=message, vie = message)
    )
    CoroutineScope(Dispatchers.IO).launch {
        try {
            service.sendNotification(payload)
        } catch (e: Exception) {
            Log.e("OneSignalSend_ERROR", "Network/Send Error: ${e.message}", e)
        }
    }
}