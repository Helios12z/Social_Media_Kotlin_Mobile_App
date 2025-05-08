package com.example.socialmediaproject.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.PostViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlin.math.ln

class HomeViewModel : ViewModel() {
    private val db=FirebaseFirestore.getInstance()
    private val auth=FirebaseAuth.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val _postlist=MutableLiveData<List<PostViewModel>>()
    val postlist: LiveData<List<PostViewModel>> = _postlist
    private val _isloading= MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> = _isloading
    private var userFriends = listOf<String>()
    private var currentUserId = ""
    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean> = _canLoadMore
    private var lastVisiblePost: DocumentSnapshot? = null
    private val postsPerPage = 10
    private var isLoadingMore = false
    private val allLoadedPosts = mutableListOf<PostViewModel>()
    private var hiddenPostIds = emptyList<String>()
    private var blockedUserIds = listOf<String>()

    init {
        loadInitialPosts()
    }

    private fun loadInitialPosts() {
        _isloading.value = true
        val userId = auth.currentUser?.uid ?: ""
        currentUserId = userId
        getUserFriends(userId) { friends ->
            userFriends = friends
            getUserHiddenPosts(userId) {
                hidden->hiddenPostIds=hidden
                getUserInterests(userId) { userInterests ->
                    val cleanedUserInterests = userInterests.map { it.trim() }
                    if (cleanedUserInterests.isEmpty()) {
                        _isloading.value = false
                        _postlist.value = emptyList()
                        _canLoadMore.value = false
                        return@getUserInterests
                    }
                    getExcludedUserIds(userId) {
                        excluded->blockedUserIds=excluded
                        loadPagedPosts(cleanedUserInterests, true)
                    }
                }
            }
        }
    }

    fun loadMorePosts() {
        if (isLoadingMore || lastVisiblePost == null) return
        val userId = auth.currentUser?.uid ?: ""
        getUserInterests(userId) { userInterests ->
            val cleanedUserInterests = userInterests.map { it.trim() }
            loadPagedPosts(cleanedUserInterests, false)
        }
    }

    private fun loadPagedPosts(userInterests: List<String>, isInitialLoad: Boolean) {
        isLoadingMore = true
        if (isInitialLoad) {
            _isloading.value = true
        }
        var postsQuery = db.collection("Posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(postsPerPage.toLong())
        if (!isInitialLoad && lastVisiblePost != null) {
            postsQuery = postsQuery.startAfter(lastVisiblePost!!)
        }
        postsQuery.get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                _canLoadMore.value = false
                _isloading.value = false
                isLoadingMore = false
                return@addOnSuccessListener
            }
            lastVisiblePost = documents.documents[documents.size() - 1]
            _canLoadMore.value = documents.size() == postsPerPage
            val pagePostList = mutableListOf<PostViewModel>()
            val tasks = mutableListOf<Task<*>>()
            for (doc in documents) {
                val userid = doc.getString("userid") ?: ""
                if (userid in blockedUserIds) continue
                val userTask = db.collection("Users").document(userid).get()
                val postStatsTask = realtimedb.getReference("PostStats").child(doc.id).get()
                val likesTask = db.collection("Likes")
                    .whereEqualTo("userid", currentUserId)
                    .whereEqualTo("postid", doc.id)
                    .get()
                tasks.add(userTask)
                tasks.add(postStatsTask)
                tasks.add(likesTask)
                Tasks.whenAllComplete(listOf(userTask, postStatsTask, likesTask)).addOnSuccessListener { results ->
                    val userDoc = (results[0] as Task<DocumentSnapshot>).result
                    val ref = (results[1] as Task<DataSnapshot>).result
                    val likeResults = (results[2] as Task<QuerySnapshot>).result
                    val likecount = ref.child("likecount").getValue(Int::class.java) ?: 0
                    var isliked = false
                    if (!likeResults.isEmpty) {
                        for (result in likeResults) {
                            isliked = result.getBoolean("status") ?: false
                        }
                    }
                    val post = PostViewModel(
                        id = doc.id,
                        userId = userid,
                        userName = userDoc.getString("name") ?: "",
                        userAvatarUrl = userDoc.getString("avatarurl") ?: "",
                        content = doc.getString("content") ?: "",
                        category = doc.get("category") as? List<String> ?: emptyList(),
                        imageUrls = doc.get("imageurl") as? List<String> ?: emptyList(),
                        timestamp = doc.getLong("timestamp") ?: 0,
                        likeCount = likecount,
                        isLiked = isliked,
                        privacy = doc.getString("privacy") ?: ""
                    )
                    pagePostList.add(post)
                }
            }
            Tasks.whenAllComplete(tasks).addOnSuccessListener {
                val noHidden = pagePostList.filterNot { post ->
                    hiddenPostIds.contains(post.id)
                }
                val filteredPosts = noHidden.filter { post ->
                    when(post.privacy) {
                        "Công khai" -> true
                        "Bạn bè" -> userFriends.contains(post.userId) || post.userId == currentUserId
                        "Riêng tư" -> post.userId == currentUserId
                        else -> false
                    }
                }
                val sortedPagePosts = filteredPosts.sortedByDescending { post ->
                    calculatePostDisplayValue(post, userInterests)
                }
                if (isInitialLoad) {
                    allLoadedPosts.clear()
                    allLoadedPosts.addAll(sortedPagePosts)
                }
                else allLoadedPosts.addAll(sortedPagePosts)
                _postlist.value = allLoadedPosts.toList()
                _isloading.value = false
                isLoadingMore = false
            }
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
            _isloading.value = false
            isLoadingMore = false
        }
    }

    fun refreshFeed() {
        lastVisiblePost = null
        allLoadedPosts.clear()
        loadInitialPosts()
    }

    private fun getUserInterests(userId: String, callback: (List<String>) -> Unit) {
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val interests = document.get("interests") as? List<String> ?: emptyList()
                callback(interests)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    private fun getUserFriends(userId: String, callback: (List<String>) -> Unit) {
        db.collection("Users").document(userId).get().addOnSuccessListener {
            result->if (result.exists()) {
                val friends = result.get("friends") as? List<String> ?: emptyList()
                callback(friends)
            }
        }
        .addOnFailureListener {
            callback(emptyList())
        }
    }

    private fun calculatePostDisplayValue(post: PostViewModel, userInterests: List<String>): Double {
        val TIME_WEIGHT = 0.6
        val INTERACTION_WEIGHT = 0.15
        val CATEGORY_WEIGHT = 0.15
        val SELF_POST_WEIGHT = 0.05
        val FRIEND_POST_WEIGHT = 0.05
        var value = 0.0
        val currentTimeMillis = System.currentTimeMillis()
        val postTimeMillis = post.timestamp
        val timeDifferenceHours = (currentTimeMillis - postTimeMillis) / (1000 * 60 * 60)
        val timeScore = maxOf(0.0, 1.0 - (timeDifferenceHours / 48.0))
        val veryRecentBonus = if (timeDifferenceHours < 1) 0.2 else 0.0
        val likes = post.likeCount
        val interactionScore = minOf(1.0, ln((likes + 1).toDouble()) / 5.0)
        var categoryScore = 0.0
        post.category.forEach { category ->
            if (userInterests.any { it.equals(category, ignoreCase = true) }) {
                categoryScore += 0.25
            }
        }
        categoryScore = minOf(1.0, categoryScore)
        val selfPostScore = if (post.userId == currentUserId) 1.0 else 0.0
        val friendPostScore = if (userFriends.contains(post.userId)) 1.0 else 0.0
        value = (timeScore * TIME_WEIGHT) +
                (interactionScore * INTERACTION_WEIGHT) +
                (categoryScore * CATEGORY_WEIGHT) +
                (selfPostScore * SELF_POST_WEIGHT) +
                (friendPostScore * FRIEND_POST_WEIGHT) +
                veryRecentBonus
        if (post.userId == currentUserId && likes > 0) {
            value += 0.05 * minOf(1.0, ln(likes.toDouble()) / 5.0)
        }
        return value
    }

    fun toggleLike(post: PostViewModel) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("Likes")
            .whereEqualTo("userid", userId)
            .whereEqualTo("postid", post.id)
            .get().addOnSuccessListener {
                results ->
                if (!results.isEmpty) {
                    for (result in results) {
                        post.isLiked = result.getBoolean("status") ?: false
                    }
                    updateLikeStatus(post, userId, results)
                } else {
                    updateLikeStatus(post, userId, null)
                }
            }
            .addOnFailureListener { e->
                e.printStackTrace()
            }
    }

    private fun updateLikeStatus(post: PostViewModel, userId: String, results: QuerySnapshot?) {
        val ref = realtimedb.getReference("PostStats").child(post.id)
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var likeCount = currentData.child("likecount").getValue(Int::class.java) ?: 0
                if (post.isLiked) {
                    likeCount = (likeCount - 1).coerceAtLeast(0)
                    currentData.child("likecount").value = likeCount
                } else {
                    likeCount += 1
                    currentData.child("likecount").value = likeCount
                }
                return Transaction.success(currentData)
            }
            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (committed) {
                    if (post.isLiked) {
                        post.likeCount -= 1
                        post.isLiked = false
                        results?.let {
                            for (result in it) {
                                db.collection("Likes").document(result.id).delete()
                            }
                        }
                    } else {
                        post.likeCount += 1
                        post.isLiked = true
                        results?.let {
                            for (document in it.documents) {
                                db.collection("Likes").document(document.id).update("status", true)
                            }
                        } ?: run {
                            val item = hashMapOf(
                                "userid" to userId,
                                "postid" to post.id,
                                "status" to true
                            )
                            db.collection("Likes").add(item)
                        }
                    }
                    _postlist.value = _postlist.value
                } else {
                    //log error
                }
            }
        })
    }

    private fun getUserHiddenPosts(userId: String, callback: (List<String>) -> Unit) {
        db.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val hidden = doc.get("hiddenPosts") as? List<String> ?: emptyList()
                callback(hidden)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun hidePostLocally(postId: String) {
        allLoadedPosts.removeAll { it.id == postId }
        _postlist.value = allLoadedPosts.toList()
    }

    private fun getExcludedUserIds(userId: String, cb: (List<String>) -> Unit) {
        val myBlockedTask = db.collection("Users")
            .document(userId)
            .collection("BlockedUsers")
            .get()

        val blockedMeTask = db.collectionGroup("BlockedUsers")
            .whereEqualTo("blockedUserId", userId)
            .get()

        Tasks.whenAllSuccess<QuerySnapshot>(listOf(myBlockedTask, blockedMeTask))
            .addOnSuccessListener { results ->
                val myBlocked = results[0].documents.map { it.id }
                val blockedMe = results[1].documents.mapNotNull { doc ->
                    doc.reference.parent.parent?.id
                }
                val excluded = (myBlocked + blockedMe).distinct()
                cb(excluded)
            }
            .addOnFailureListener { e ->
                cb(emptyList())
            }
    }
}