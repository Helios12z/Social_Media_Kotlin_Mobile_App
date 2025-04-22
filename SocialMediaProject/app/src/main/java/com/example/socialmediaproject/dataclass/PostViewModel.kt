package com.example.socialmediaproject.dataclass

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PostViewModel(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var userAvatarUrl: String = "",
    var content: String = "",
    var category: List<String> = listOf(),
    var imageUrls: List<String> = listOf(),
    var timestamp: Long = 0L,
    var likeCount: Int = 0,
    var commentCount: Int = 0,
    var shareCount: Int = 0,
    var isLiked: Boolean = false
) {
    fun getTimeAgo(): String {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - timestamp
        val seconds = timeDifference / 1000
        return when {
            seconds < 60 -> "Vừa xong"
            seconds < 3600 -> "${seconds / 60} phút trước"
            seconds < 86400 -> "${seconds / 3600} giờ trước"
            seconds < 604800 -> "${seconds / 86400} ngày trước"
            else -> {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}