package com.example.socialmediaproject.ui.searchusersandposts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SearchUsersAndPostsViewModel : ViewModel() {
    private val _searchQuery = MutableLiveData<String>()
    private val db=FirebaseFirestore.getInstance()
    private val auth=FirebaseAuth.getInstance()
    private val _postlist=MutableLiveData<List<PostViewModel>>()
    val postlist: LiveData<List<PostViewModel>> = _postlist
    private val _userlist=MutableLiveData<List<User>>()
    val userlist: LiveData<List<User>> = _userlist

    suspend fun setSearchQuery(query: String) {
        val q = query.trim()
        _searchQuery.value = q
        if (q.isNotEmpty()) {
            searchUser(q)
            searchPost(q)
        } else {
            _userlist.value = emptyList()
            _postlist.value = emptyList()
        }
    }

    suspend fun searchPost(query: String) {
        withContext(Dispatchers.IO) {
            val currentUserId = auth.currentUser?.uid.orEmpty()
            if (currentUserId.isEmpty()) return@withContext
            val excluded = getExcludedUserIds(currentUserId)
            val keywords = query
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
            val snaps = db.collection("Posts")
                .get()
                .await()
            val results = snaps.documents.mapNotNull { doc ->
                val authorId = doc.getString("userid").orEmpty()
                if (authorId in excluded) return@mapNotNull null
                val privacy = doc.getString("privacy").orEmpty()
                if (privacy == "private") return@mapNotNull null
                if (privacy == "friends") {
                    val authorDoc = db.collection("Users")
                        .document(authorId)
                        .get()
                        .await()
                    val friends = authorDoc.get("friends") as? List<String> ?: emptyList()
                    if (currentUserId !in friends) return@mapNotNull null
                }
                val content = doc.getString("content").orEmpty()
                if (!keywords.any { kw -> content.contains(kw, ignoreCase = true) }) {
                    return@mapNotNull null
                }
                val authorDoc = db.collection("Users")
                    .document(authorId)
                    .get()
                    .await()
                val userName = authorDoc.getString("name").orEmpty()
                val userAvatar = authorDoc.getString("avatarurl").orEmpty()
                PostViewModel(
                    id = doc.id,
                    userId = authorId,
                    userName = userName,
                    userAvatarUrl = userAvatar,
                    content = content,
                    imageUrls = doc.get("imageurl") as? List<String> ?: emptyList(),
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    privacy = privacy
                )
            }
            withContext(Dispatchers.Main) {
                _postlist.value = results
            }
        }
    }

    suspend fun searchUser(query: String) {
        withContext(Dispatchers.IO) {
            val currentUserId = auth.currentUser?.uid.orEmpty()
            if (currentUserId.isEmpty()) return@withContext
            val excluded = getExcludedUserIds(currentUserId)
            val keywords = query.lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
            val snaps = db.collection("Users")
                .get()
                .await()
            val results = snaps.documents.mapNotNull { doc ->
                val uid = doc.id
                if (uid in excluded) return@mapNotNull null
                val name = doc.getString("name").orEmpty()
                val fullName = doc.getString("fullname").orEmpty()
                val bio = doc.getString("bio").orEmpty()
                val matches = keywords.any { kw ->
                    name.contains(kw, ignoreCase = true)
                            || fullName.contains(kw, ignoreCase = true)
                            || bio.contains(kw, ignoreCase = true)
                }
                if (!matches) return@mapNotNull null
                User(
                    userid = uid,
                    name = name,
                    fullName = fullName,
                    avatarurl = doc.getString("avatarurl").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    bio = bio,
                    friends = doc.get("friends") as? List<String> ?: emptyList()
                )
            }
            withContext(Dispatchers.Main) {
                _userlist.value = results
            }
        }
    }

    private suspend fun getExcludedUserIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        val myBlocked = db.collection("Users")
            .document(userId)
            .collection("BlockedUsers")
            .get()
            .await()
            .documents
            .map { it.id }
        val blockedMe = db.collectionGroup("BlockedUsers")
            .whereEqualTo("blockedUserId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.reference.parent.parent?.id
            }
        (myBlocked + blockedMe).distinct()
    }
}