package com.example.socialmediaproject.ui.likedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class LikeDetailViewModel: ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _likedUsers = MutableLiveData<List<User>>()
    val likedUsers: LiveData<List<User>> = _likedUsers

    private var allLikedUserIds: List<String> = emptyList()
    private var currentPage = 0
    private val pageSize = 20
    private var isLoading = false
    private var currentPostId: String? = null

    fun loadInitial(postId: String, currentUserId: String) {
        if (postId == currentPostId) return

        currentPostId = postId
        currentPage = 0
        allLikedUserIds = emptyList()
        _likedUsers.value = emptyList()
        isLoading = false

        db.collection("Likes")
            .whereEqualTo("postid", postId)
            .whereEqualTo("status", true)
            .get()
            .addOnSuccessListener { likeDocs ->
                allLikedUserIds = likeDocs.mapNotNull { it.getString("userid") }
                loadNextPage(currentUserId)
            }
    }

    fun loadNextPage(currentUserId: String) {
        if (isLoading || currentPage * pageSize >= allLikedUserIds.size) return
        isLoading = true

        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allLikedUserIds.size)
        val pageIds = allLikedUserIds.subList(start, end)

        db.collection("Users").document(currentUserId).get().addOnSuccessListener { doc ->
            val friendList = doc["friends"] as? List<*> ?: emptyList<String>()

            db.collection("Users")
                .whereIn(FieldPath.documentId(), pageIds)
                .get()
                .addOnSuccessListener { userDocs ->
                    val currentList = _likedUsers.value?.toMutableList() ?: mutableListOf()

                    for (userDoc in userDocs) {
                        val id = userDoc.id
                        val name = userDoc.getString("name") ?: ""
                        val avatar = userDoc.getString("avatarurl") ?: ""
                        val isSelf = id == currentUserId
                        val isFriend = friendList.contains(id)
                        val email = when {
                            isSelf -> "Bạn"
                            isFriend -> "Bạn bè"
                            else -> "Người lạ"
                        }
                        currentList.add(User(id, name, avatar, email))
                    }

                    _likedUsers.value = currentList
                    currentPage++
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }
}