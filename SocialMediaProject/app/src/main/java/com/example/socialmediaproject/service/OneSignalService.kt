package com.example.socialmediaproject.service

import com.example.socialmediaproject.dataclass.NotificationPayload
import com.example.socialmediaproject.dataclass.NotificationResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OneSignalService {
    @Headers(
        "Authorization: Basic os_v2_app_4nkobofcfndgfbuwnusdd5yzdt63swgsa7ge6ivfyvrpuehjncx4yb2e2jvkuez6qqe36fc25lgolfu7dqocg3m66on2z35kdxtdnyq",
        "Content-Type: application/json"
    )
    @POST("notifications")
    suspend fun sendNotification(@Body payload: NotificationPayload): retrofit2.Response<NotificationResponse>
}