package com.example.socialmediaproject.dataclass

data class Friend(
    val id: String,
    val displayName: String,
    val avatarUrl: String,
    var isFriend: Boolean,
    var mutualFriendCount: Int,
    var fullName: String
)
