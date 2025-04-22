package com.example.socialmediaproject.ui.postwithcomment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.PostViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import okhttp3.internal.notify
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostWithCommentViewModel: ViewModel() {
    private val db=FirebaseFirestore.getInstance()
    private val auth= FirebaseAuth.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
    val postData = MutableLiveData<DocumentSnapshot>()
    val postUser = MutableLiveData<DocumentSnapshot>()
    val statsLiveData = MutableLiveData<Triple<Int, Int, Int>>()
    val userAvatarUrl = MutableLiveData<String>()
    val errorMessage = MutableLiveData<String>()
    private var postId: String = ""

    fun init(postId: String) {
        if (this.postId == postId && postData.value != null) return
        this.postId = postId
        fetchPost()
        listenToStats()
        fetchCurrentUserAvatar()
    }

    private fun fetchPost() {
        db.collection("Posts").document(postId).get()
        .addOnSuccessListener { post ->
            postData.postValue(post)
            val userId = post.getString("userid") ?: ""
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { user ->
                    postUser.postValue(user)
                }
                .addOnFailureListener { errorMessage.postValue("Không tải được người dùng") }
        }
        .addOnFailureListener { errorMessage.postValue("Không tải được bài viết") }
    }

    private fun listenToStats() {
        realtimedb.getReference("PostStats").child(postId)
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val like = snapshot.child("likecount").getValue(Int::class.java) ?: 0
                val comment = snapshot.child("commentcount").getValue(Int::class.java) ?: 0
                val share = snapshot.child("sharecount").getValue(Int::class.java) ?: 0
                statsLiveData.postValue(Triple(like, comment, share))
            }
            override fun onCancelled(error: DatabaseError) {
                errorMessage.postValue("Không thể lấy dữ liệu thống kê")
            }
        })
    }

    private fun fetchCurrentUserAvatar() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("Users").document(currentUserId).get()
        .addOnSuccessListener { user ->
            val avatar = user.getString("avatarurl") ?: ""
            userAvatarUrl.postValue(avatar)
        }
        .addOnFailureListener { errorMessage.postValue("Không tải được avatar") }
    }

    fun getTimeAgo(timestamp: Long): String {
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
                    postData.notify()
                } else {
                    //log error
                }
            }
        })
    }
}