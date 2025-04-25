package com.example.socialmediaproject.dataclass

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val receiverId: String = "",
    val senderId: String = "",
    val type: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val relatedPostId: String? = null,
    val relatedCommentId: String? = null,
    val relatedUserId: String? = null,
    val read: Boolean = false,
    var senderAvatarUrl: String=""
)
