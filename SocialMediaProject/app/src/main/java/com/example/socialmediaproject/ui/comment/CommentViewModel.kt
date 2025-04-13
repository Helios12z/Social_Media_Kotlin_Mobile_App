package com.example.socialmediaproject.ui.comment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaproject.dataclass.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()
    private val db: FirebaseFirestore =FirebaseFirestore.getInstance()

    fun postComment(content: String, parentId: String? = null, postId: String) {
        viewModelScope.launch {
            val commentId = db.collection("comments").document().id
            Log.d("CommentViewModel", "Posting comment with ID: $commentId")
            val comment = Comment(
                id = commentId,
                content = content,
                userId = auth.currentUser?.uid ?: "",
                parentId = parentId,
                postId = postId
            )
            Log.d("CommentViewModel", "Posting comment: $comment")
            db.collection("comments")
                .document(commentId)
                .set(comment)
                .addOnSuccessListener {

                }
                .addOnFailureListener {

                }
        }
    }

    fun getComments(onResult: (List<Comment>) -> Unit) {
        db.collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {

                    return@addSnapshotListener
                }
                val comments = snapshots?.toObjects(Comment::class.java) ?: listOf()
                onResult(comments)
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
            // Fail
        }
    }
}