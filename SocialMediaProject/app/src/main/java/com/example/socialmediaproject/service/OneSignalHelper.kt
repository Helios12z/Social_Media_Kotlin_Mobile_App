package com.example.socialmediaproject.service

import android.util.Log
import com.example.socialmediaproject.dataclass.NotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.socialmediaproject.dataclass.NotificationContent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object OneSignalHelper {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://onesignal.com/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val service by lazy {
        retrofit.create(OneSignalService::class.java)
    }

    fun sendMentionNotification(playerId: String, message: String, commentId: String) {
        val payload = NotificationPayload(
            appId = "e354e0b8-a22b-4662-8696-6d2431f7191c",
            includedExternalUserIds = listOf(playerId),
            contents = NotificationContent(en = message, vie=message)
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.sendNotification(payload)
                if (response.isSuccessful) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("comments")
                        .document(commentId)
                        .update("notifiedUserIds", FieldValue.arrayUnion(playerId))
                        .addOnSuccessListener {

                        }
                        .addOnFailureListener {

                        }
                } else {
                    Log.e("OneSignal", "Mention failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendPushNotification(userId: String, message: String) {
        val payload = NotificationPayload(
            appId = "e354e0b8-a22b-4662-8696-6d2431f7191c",
            includedExternalUserIds = listOf(userId),
            contents = NotificationContent(en = message, vie = message)
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.sendNotification(payload)
                if (response.isSuccessful) {
                    updateNotifiedStatusIfNeeded(userId)
                } else {
                    //Log error
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateNotifiedStatusIfNeeded(userId: String) {
        val db = FirebaseFirestore.getInstance()
        try {
            val snapshot = db.collection("friend_requests")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .whereEqualTo("notified", false)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "notified", true)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
