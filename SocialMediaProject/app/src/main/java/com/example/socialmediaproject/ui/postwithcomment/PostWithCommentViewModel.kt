package com.example.socialmediaproject.ui.postwithcomment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
    val isPostLiked = MutableLiveData<Boolean>()
    var postId: String = ""

    fun fetchPost() {
        db.collection("Posts").document(postId).get()
        .addOnSuccessListener { post ->
            postData.postValue(post)
            val userId = post.getString("userid") ?: ""
            if (userId!="")
            db.collection("Users").document(userId).get()
            .addOnSuccessListener { user ->
                postUser.postValue(user)
            }
            .addOnFailureListener { errorMessage.postValue("Không tải được người dùng") }
            else return@addOnSuccessListener
        }
        .addOnFailureListener { errorMessage.postValue("Không tải được bài viết") }
    }

    fun listenToStats() {
        realtimedb.getReference("PostStats")
        .child(postId)
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val like = snapshot.child("likecount").getValue(Int::class.java) ?: 0
                db.collection("comments")
                    .whereEqualTo("postId", postId)
                    .count()
                    .get(AggregateSource.SERVER)
                    .addOnSuccessListener { aggSnap ->
                        val commentCount = aggSnap.count.toInt()
                        statsLiveData.postValue(Triple(like, commentCount, 0))
                    }
                    .addOnFailureListener {
                        statsLiveData.postValue(Triple(like, 0, 0))
                    }
            }
            override fun onCancelled(error: DatabaseError) {
                errorMessage.postValue("Không thể lấy dữ liệu: ${error.message}")
            }
        })
    }

    fun listenToLikeState() {
        db.collection("Likes")
            .whereEqualTo("postid", postId)
            .whereEqualTo("userid", auth.currentUser?.uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    isPostLiked.postValue(false)
                    return@addSnapshotListener
                }
                val liked = snap?.documents?.isNotEmpty() ?: false
                isPostLiked.postValue(liked)
            }
    }

    fun fetchCurrentUserAvatar() {
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
}