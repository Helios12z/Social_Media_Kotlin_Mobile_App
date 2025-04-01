package com.example.socialmediaproject.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FriendRequestWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        checkForFriendRequests()
        scheduleNextWorker()
        return Result.success()
    }

    private suspend fun checkForFriendRequests() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        try {
            val snapshots = db.collection("friend_requests")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            val count = snapshots.size()
            if (count > 0) {
                sendPushNotification(currentUserId, "Bạn có $count lời mời kết bạn mới!")
            }
        } catch (e: Exception) {
            Log.e("ONE SIGNAL ERROR", e.message.toString())
        }
    }

    private fun scheduleNextWorker() {
        val workRequest = OneTimeWorkRequestBuilder<FriendRequestWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag("FriendRequestWorker")
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
}