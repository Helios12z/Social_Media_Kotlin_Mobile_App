package com.example.socialmediaproject.ui.comment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaproject.dataclass.Comment
import com.example.socialmediaproject.service.OneSignalHelper
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CommentViewModel : ViewModel() {
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()
    private val db: FirebaseFirestore =FirebaseFirestore.getInstance()
    var postId: String = ""
    private var cachedComments: List<Comment>? = null
    var onCommentLikedSuccessfully: ((commentId: String) -> Unit)? = null
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private var isProcessingLike = false
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    fun postComment(content: String, parentId: String? = null, postId: String) {
        viewModelScope.launch {
            val commentId = db.collection("comments").document().id
            val comment = Comment(
                id = commentId,
                content = content,
                userId = auth.currentUser?.uid ?: "",
                parentId = parentId,
                postId = postId,
                mentionedUserIds = emptyList(),
                notifiedUserIds = emptyList()
            )
            db.collection("comments")
            .document(commentId)
            .set(comment)
            .addOnSuccessListener {
                handleMentions(content, commentId)
                updateCommentCount(postId)
            }
            .addOnFailureListener {
                e->e.printStackTrace()
            }
        }
    }

    fun getComments(onResult: (List<Comment>) -> Unit) {
        cachedComments?.let {
            onResult(it)
            return
        }
        db.collection("comments")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) return@addSnapshotListener
            if (isProcessingLike) {
                isProcessingLike = false
                return@addSnapshotListener
            }
            val comments = snapshots.toObjects(Comment::class.java)
            val userIds = comments.map { it.userId }.toSet().filter { it.isNotBlank() }
            val batches = userIds.chunked(10)
            val userMap = mutableMapOf<String, Pair<String, String>>()
            var completedBatches = 0
            batches.forEach { batch ->
                db.collection("Users")
                .whereIn("userid", batch)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        val id = doc.getString("userid") ?: return@forEach
                        val username = doc.getString("name") ?: "Unknown"
                        val avatarurl = doc.getString("avatarurl") ?: ""
                        userMap[id] = Pair(username, avatarurl)
                    }
                    completedBatches++
                    if (completedBatches == batches.size) {
                        comments.forEach { comment ->
                            userMap[comment.userId]?.let { (username, avatar) ->
                                comment.username = username
                                comment.avatarurl = avatar
                            }
                        }
                        cachedComments = comments
                        onResult(comments)
                    }
                }
                .addOnFailureListener {
                    completedBatches++
                    if (completedBatches == batches.size) {
                        cachedComments = comments
                        onResult(comments)
                    }
                }
            }
        }
    }

    fun toggleLikeComment(commentId: String, userId: String) {
        isProcessingLike = true
        val commentRef = db.collection("comments").document(commentId)
        var willBeLiked = true
        cachedComments?.find { it.id == commentId }?.let { comment ->
            val currentLikes = comment.likes ?: listOf()
            willBeLiked = !currentLikes.contains(userId)
        }
        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val currentLikes = snapshot.get("likes") as? List<String> ?: listOf()
            val updatedLikes = if (currentLikes.contains(userId)) {
                currentLikes - userId
            } else {
                currentLikes + userId
            }
            transaction.update(commentRef, "likes", updatedLikes)
        }.addOnSuccessListener {
            cachedComments?.let { comments ->
                val updatedComment = comments.find { it.id == commentId }
                updatedComment?.let { comment ->
                    comment.likes = if (willBeLiked) {
                        (comment.likes ?: listOf()).filter { it != userId } + userId
                    } else {
                        (comment.likes ?: listOf()).filter { it != userId }
                    }
                    onCommentLikedSuccessfully?.invoke(commentId)
                }
                if (updatedComment == null) {
                    comments.forEach { parentComment ->
                        parentComment.replies?.find { it.id == commentId }?.let { reply ->
                            reply.likes = if (willBeLiked) {
                                (reply.likes ?: listOf()).filter { it != userId } + userId
                            } else {
                                (reply.likes ?: listOf()).filter { it != userId }
                            }
                            onCommentLikedSuccessfully?.invoke(commentId)
                        }
                    }
                }
            }
            isProcessingLike = false
        }.addOnFailureListener { e->
            e.printStackTrace()
            isProcessingLike = false
        }
    }

    private fun handleMentions(content: String, commentId: String) {
        val pattern = Pattern.compile("@([a-zA-Z0-9_]+)")
        val matcher = pattern.matcher(content)
        val mentionedUsernames = mutableSetOf<String>()
        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1))
        }
        if (mentionedUsernames.isEmpty()) return
        db.collection("Users").document(auth.currentUser?.uid?:"").get().addOnSuccessListener {
            result->if(result.exists()) {
               val sendername=result.getString("name") ?: "Ai đó"
                db.collection("Users")
                .whereIn("name", mentionedUsernames.toList())
                .get()
                .addOnSuccessListener { snapshot ->
                    val mentionedUserIds = mutableListOf<String>()
                    for (doc in snapshot.documents) {
                        val userId = doc.getString("userid") ?: continue
                        mentionedUserIds.add(userId)
                        OneSignalHelper.sendMentionNotification(
                            userId = userId,
                            message = "$sendername đã nhắc đến bạn trong một bình luận",
                            commentId=commentId
                        )
                        val notification = hashMapOf(
                            "receiverId" to userId,
                            "senderId" to auth.currentUser?.uid,
                            "type" to "mention",
                            "message" to "$sendername đã nhắc đến bạn trong một bình luận",
                            "timestamp" to Timestamp.now(),
                            "relatedPostId" to postId,
                            "relatedCommentId" to commentId,
                            "read" to false
                        )
                        db.collection("notifications").add(notification)
                    }
                    if (mentionedUserIds.isNotEmpty()) {
                        db.collection("comments")
                        .document(commentId)
                        .update(
                            mapOf(
                                "mentionedUserIds" to mentionedUserIds
                            )
                        )
                    }
                }
                .addOnFailureListener {
                    return@addOnFailureListener
                }
            }
            else {
                return@addOnSuccessListener
            }
        }
        .addOnFailureListener {
            return@addOnFailureListener
        }
    }

    private fun updateCommentCount(postId: String) {
        realtimedb.getReference("PostStats").child(postId).runTransaction(object: Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var commentCount = currentData.child("commentcount").getValue(Int::class.java) ?: 0
                commentCount += 1
                currentData.child("commentcount").value = commentCount
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                //handle error
            }
        })
    }
}