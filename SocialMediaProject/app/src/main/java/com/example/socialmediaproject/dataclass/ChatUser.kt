package com.example.socialmediaproject.dataclass

data class ChatUser(
    val id: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val timestamp: String? = null,
    val unreadCount: Int = 0
)
