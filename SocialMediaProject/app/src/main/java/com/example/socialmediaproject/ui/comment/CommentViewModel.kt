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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CommentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val _comments = MutableLiveData<MutableList<Comment>>(mutableListOf())
    val comments: LiveData<MutableList<Comment>> = _comments
    private var lastVisibleComment: DocumentSnapshot? = null
    private var isLoading = false
    private val pageSize = 6
    var postId: String = ""

    fun loadInitialComments() {
        if (isLoading || (_comments.value?.isNotEmpty() == true)) return
        isLoading = true
        db.collection("comments")
        .whereEqualTo("postId", postId)
        .whereEqualTo("parentId", null)
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .limit(pageSize.toLong())
        .get()
        .addOnSuccessListener { snapshot ->
            lastVisibleComment = snapshot.documents.lastOrNull()
            val parents = snapshot.documents.mapNotNull { it.toComment() }
            fetchRepliesForParents(parents)
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
        }
        .addOnCompleteListener { isLoading = false }
    }

    fun loadMoreComments() {
        if (isLoading || lastVisibleComment == null) return
        isLoading = true
        db.collection("comments")
        .whereEqualTo("postId", postId)
        .whereEqualTo("parentId", null)
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .startAfter(lastVisibleComment!!)
        .limit(pageSize.toLong())
        .get()
        .addOnSuccessListener { snapshot ->
            lastVisibleComment = snapshot.documents.lastOrNull()
            val parents = snapshot.documents.mapNotNull { it.toComment() }
            fetchRepliesForParents(parents)
        }
        .addOnCompleteListener { isLoading = false }
    }

    private fun fetchRepliesForParents(parents: List<Comment>) {
        if (parents.isEmpty()) return

        val parentIds = parents.map { it.id }
        val allUserIds = parents.map { it.userId }.toMutableSet()

        db.collection("comments")
        .whereIn("parentId", parentIds)
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { replySnapshot ->
            val replies = replySnapshot.documents.mapNotNull { it.toComment() }
            allUserIds.addAll(replies.map { it.userId })
            if (allUserIds.isEmpty()) {
                addParentsToComments(parents)
                return@addOnSuccessListener
            }
            val userIdList = allUserIds.toList()
            val batches = userIdList.chunked(10)
            val userMap = mutableMapOf<String, Pair<String, String>>()
            var completedBatches = 0
            batches.forEach { batch ->
                db.collection("Users")
                .whereIn("userid", batch)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    userSnapshot.documents.forEach { doc ->
                        val id = doc.getString("userid") ?: return@forEach
                        val name = doc.getString("name") ?: "Unknown"
                        val avatar = doc.getString("avatarurl") ?: ""
                        userMap[id] = Pair(name, avatar)
                    }
                    completedBatches++
                    if (completedBatches == batches.size) {
                        val parentsWithReplies = parents.map { parent ->
                            parent.copy(
                                username = userMap[parent.userId]?.first ?: "Unknown",
                                avatarurl = userMap[parent.userId]?.second ?: "",
                                replies = replies.filter { it.parentId == parent.id }
                                    .map { reply ->
                                        reply.copy(
                                            username = userMap[reply.userId]?.first ?: "Unknown",
                                            avatarurl = userMap[reply.userId]?.second ?: ""
                                        )
                                    }.toMutableList()
                            )
                        }
                        addParentsToComments(parentsWithReplies)
                    }
                }
                .addOnFailureListener {
                    completedBatches++
                    if (completedBatches == batches.size) {
                        addParentsToComments(parents)
                    }
                }
            }
        }
    }

    private fun addParentsToComments(parents: List<Comment>) {
        val currentList = _comments.value ?: mutableListOf()
        currentList.addAll(parents)
        _comments.postValue(currentList)
    }

    fun toggleLikeComment(commentId: String, onFinish: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val commentRef = db.collection("comments").document(commentId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            val likes = snapshot.get("likes") as? List<String> ?: listOf()
            val updatedLikes = if (likes.contains(userId)) {
                likes - userId
            } else {
                likes + userId
            }
            transaction.update(commentRef, "likes", updatedLikes)
        }.addOnSuccessListener {
            updateLocalCommentLikes(commentId, userId)
            onFinish()
        }.addOnFailureListener {
            onFinish()
        }
    }

    private fun updateLocalCommentLikes(commentId: String, userId: String) {
        val updatedList = _comments.value?.map { comment ->
            if (comment.id == commentId) {
                val isLiked = comment.likes.contains(userId)
                val newLikes = if (isLiked) {
                    comment.likes - userId
                } else {
                    comment.likes + userId
                }
                comment.copy(likes = newLikes)
            } else {
                val updatedReplies = comment.replies.map { reply ->
                    if (reply.id == commentId) {
                        val isLiked = reply.likes.contains(userId)
                        val newLikes = if (isLiked) {
                            reply.likes - userId
                        } else {
                            reply.likes + userId
                        }
                        reply.copy(likes = newLikes)
                    } else reply
                }
                comment.copy(replies = updatedReplies.toMutableList())
            }
        }?.toMutableList()
        updatedList?.let { _comments.postValue(it) }
    }

    private fun DocumentSnapshot.toComment(): Comment? {
        return this.toObject(Comment::class.java)?.copy(id = id)
    }

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
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
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
        db.collection("Users").document(auth.currentUser?.uid ?: "").get()
        .addOnSuccessListener { result ->
            if (result.exists()) {
                val sendername = result.getString("name") ?: "Ai đó"
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
                            commentId = commentId
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
            }
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
