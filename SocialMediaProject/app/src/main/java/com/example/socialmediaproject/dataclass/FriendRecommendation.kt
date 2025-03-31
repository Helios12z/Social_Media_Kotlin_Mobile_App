package com.example.socialmediaproject.dataclass

data class FriendRecommendation(val userId: String,
                                val name: String,
                                val avatarurl: String,
                                val mutualFriendsCount: Int = 0,
                                var requestStatus: RequestStatus = RequestStatus.NONE)

enum class RequestStatus {
    NONE,
    SENDING,
    SENT,
    ACCEPTED,
    ERROR
}
