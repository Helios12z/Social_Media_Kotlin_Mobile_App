package com.example.socialmediaproject

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PostViewModel(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val content: String = "",
    val category: List<String> = listOf(),
    val imageUrls: List<String> = listOf(),
    val timestamp: Long = 0L,
    var likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
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
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}