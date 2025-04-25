package com.example.socialmediaproject.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.Notification
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _notificationsLiveData = MutableLiveData<List<Notification>>()
    val notificationsLiveData: LiveData<List<Notification>> = _notificationsLiveData

    fun fetchNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("notifications")
        .whereEqualTo("receiverId", currentUserId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val notifications = snapshot.documents.mapNotNull {
                it.toObject(Notification::class.java)?.copy(id = it.id)
            }
            val tasks = notifications.map { notification ->
                db.collection("Users").document(notification.senderId)
                    .get()
                    .continueWith { task ->
                        val avatarUrl = task.result?.getString("avatarurl")?:""
                        notification.copy(senderAvatarUrl = avatarUrl)
                    }
            }
            Tasks.whenAllSuccess<Notification>(tasks)
            .addOnSuccessListener { completedNotifications ->
                _notificationsLiveData.postValue(completedNotifications)
            }
        }
    }

    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId).update("read", true)
    }
}