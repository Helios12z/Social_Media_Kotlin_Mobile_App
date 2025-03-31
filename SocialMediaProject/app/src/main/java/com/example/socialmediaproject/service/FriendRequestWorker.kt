package com.example.socialmediaproject.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FriendRequestWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        checkForFriendRequests()
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
            e.printStackTrace()
        }
    }
}