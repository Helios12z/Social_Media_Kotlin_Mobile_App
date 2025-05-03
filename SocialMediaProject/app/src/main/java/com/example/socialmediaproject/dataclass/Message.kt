package com.example.socialmediaproject.dataclass

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val removed: Boolean = false,
    val link: Boolean = false,
    val picture: Boolean=false,
    val postId: String = ""
)
