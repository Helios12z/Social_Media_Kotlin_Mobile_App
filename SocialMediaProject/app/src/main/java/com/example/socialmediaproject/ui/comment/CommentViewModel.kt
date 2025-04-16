package com.example.socialmediaproject.ui.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaproject.dataclass.Comment
import com.example.socialmediaproject.service.OneSignalHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class CommentViewModel : ViewModel() {
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()
    private val db: FirebaseFirestore =FirebaseFirestore.getInstance()

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
            }
            .addOnFailureListener {

            }
        }
    }

    fun getComments(onResult: (List<Comment>) -> Unit) {
        db.collection("comments")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) return@addSnapshotListener
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
                        onResult(comments)
                    }
                }
                .addOnFailureListener {
                    completedBatches++
                    if (completedBatches == batches.size) {
                        onResult(comments)
                    }
                }
            }
        }
    }

    fun toggleLikeComment(commentId: String, userId: String) {
        val commentRef = db.collection("comments").document(commentId)
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
            // Success
        }.addOnFailureListener {
            //Failure
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
}