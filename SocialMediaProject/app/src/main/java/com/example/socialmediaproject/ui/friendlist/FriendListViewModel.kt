package com.example.socialmediaproject.ui.friendlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.Friend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.min

class FriendListViewModel : ViewModel() {
    companion object { private const val PAGE_SIZE = 10 }
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _friends = MutableLiveData<List<Friend>>(emptyList())
    val friends: LiveData<List<Friend>> = _friends
    private var currentUserFriendIds = emptySet<String>()
    private var targetFriendIds = emptyList<String>()
    private var currentPage = 0
    private var hasMore = true
    var isLoading = false
    private var lastTargetUserId: String? = null

    fun loadInitialFriends(targetUserId: String) {
        if (targetUserId == lastTargetUserId && targetFriendIds.isNotEmpty()) return
        lastTargetUserId = targetUserId
        targetFriendIds = emptyList()
        currentUserFriendIds = emptySet()
        currentPage = 0
        hasMore     = true
        isLoading   = false
        _friends.value = emptyList()
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("Users").document(currentUid).get()
        .addOnSuccessListener { currentDoc ->
            @Suppress("UNCHECKED_CAST")
            currentUserFriendIds = (currentDoc.get("friends") as? List<String>)?.toSet() ?: emptySet()
            db.collection("Users").document(targetUserId).get()
            .addOnSuccessListener { targetDoc ->
                @Suppress("UNCHECKED_CAST")
                targetFriendIds = (targetDoc.get("friends") as? List<String>)
                    ?.toList() ?: emptyList()
                currentPage = 0
                hasMore     = true
                _friends.value = emptyList()
                loadNextPage()
            }
            .addOnFailureListener {
                it.printStackTrace()
                return@addOnFailureListener
            }
        }
        .addOnFailureListener {
            it.printStackTrace()
            return@addOnFailureListener
        }
    }

    fun loadNextPage() {
        if (isLoading || !hasMore) return
        isLoading = true
        val start = currentPage * PAGE_SIZE
        val end   = min(targetFriendIds.size, start + PAGE_SIZE)
        val ids   = targetFriendIds.subList(start, end)
        if (ids.isEmpty()) {
            hasMore = false
            isLoading = false
            return
        }
        db.collection("Users")
        .whereIn(FieldPath.documentId(), ids)
        .get()
        .addOnSuccessListener { snap ->
            val loaded = snap.documents.mapNotNull { d ->
                val theirFriends = (d.get("friends") as? List<String>)?.toSet() ?: emptySet()
                val isFriend = currentUserFriendIds.contains(d.id)
                val mutual = theirFriends.intersect(currentUserFriendIds).size
                Friend(
                    id = d.id,
                    displayName = d.getString("name") ?: "",
                    avatarUrl = d.getString("avatarurl") ?: "",
                    isFriend = isFriend,
                    mutualFriendCount = mutual,
                    fullName = d.getString("fullname") ?: ""
                )
            }
            val sorted = ids.mapNotNull { id -> loaded.find { it.id == id } }
            val current = _friends.value ?: emptyList()
            _friends.value = current + sorted
            hasMore = end < targetFriendIds.size
            currentPage++
            isLoading = false
        }
        .addOnFailureListener {
            it.printStackTrace()
            isLoading = false
        }
    }
}