package com.example.socialmediaproject.ui.likedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LikeDetailViewModel: ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _likedUsers = MutableLiveData<List<User>>()
    val likedUsers: LiveData<List<User>> = _likedUsers
    private val _postSummary = MutableLiveData<PostSummary>()
    val postSummary: LiveData<PostSummary> = _postSummary
    private var lastLoadedPostId: String? = null

    private var allLikedUserIds: List<String> = emptyList()
    private var currentPage = 0
    private val pageSize = 20
    private var isLoading = false
    private var currentPostId: String? = null

    private var postListener: ListenerRegistration? = null
    private var likeListener: ListenerRegistration? = null
    private var likesRealtimeListener: ListenerRegistration? = null

    fun loadInitial(postId: String, currentUserId: String) {
        if (postId == currentPostId) return

        currentPostId = postId
        currentPage = 0
        allLikedUserIds = emptyList()
        _likedUsers.value = emptyList()
        isLoading = false

        likesRealtimeListener?.remove()

        likesRealtimeListener = db.collection("Likes")
            .whereEqualTo("postid", postId)
            .whereEqualTo("status", true)
            .addSnapshotListener { likeDocs, _ ->
                if (likeDocs != null) {
                    allLikedUserIds = likeDocs.mapNotNull { it.getString("userid") }
                    currentPage = 0
                    _likedUsers.value = emptyList()
                    loadNextPage(currentUserId)
                }
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

    fun observePostSummary(postId: String) {
        if (postId == lastLoadedPostId) return
        lastLoadedPostId = postId

        postListener?.remove()
        likeListener?.remove()

        _postSummary.value = PostSummary("", "", 0)

        postListener = db.collection("Posts").document(postId)
            .addSnapshotListener { postDoc, _ ->
                if (postDoc != null && postDoc.exists()) {
                    val content = postDoc.getString("content") ?: ""
                    val userId = postDoc.getString("userid") ?: ""

                    db.collection("Users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val avatarUrl = userDoc.getString("avatarurl") ?: ""

                            val currentSummary = _postSummary.value ?: PostSummary("", "", 0)
                            _postSummary.postValue(
                                currentSummary.copy(content = content, avatarUrl = avatarUrl)
                            )
                        }
                }
            }

        likeListener = db.collection("Likes")
            .whereEqualTo("postid", postId)
            .whereEqualTo("status", true)
            .addSnapshotListener { querySnapshot, _ ->
                if (querySnapshot != null) {
                    val likeCount = querySnapshot.size()
                    val currentSummary = _postSummary.value ?: PostSummary("", "", 0)
                    _postSummary.postValue(currentSummary.copy(likeCount = likeCount))
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        postListener?.remove()
        likeListener?.remove()
        likesRealtimeListener?.remove()
    }
}

data class PostSummary(
    val content: String,
    val avatarUrl: String,
    val likeCount: Int
)
