package com.example.socialmediaproject.dataclass

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatUser(
    val id: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    var lastMessage: String? = null,
    var timestamp: Long = 0,
    var unreadCount: Int = 0
): Parcelable
