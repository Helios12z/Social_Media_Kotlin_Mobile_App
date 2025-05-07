package com.example.socialmediaproject.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PostActionWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    private val db=FirebaseFirestore.getInstance()
    private val auth=FirebaseAuth.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")

    override suspend fun doWork(): Result {
        val postId=inputData.getString("postId")?:return Result.failure()
        val action=inputData.getString("action")?:return Result.failure()
        return try{
            when(action) {
                "delete" -> deletePost(postId)
                "hide"->hidePost(postId)
                "unhide"->unhidePost(postId)
                else->{}
            }
            Result.success()
        }
        catch (e:Exception) {
            Result.retry()
        }
    }

    private fun deletePost(postId: String) {
        db.collection("Posts").document(postId).delete().addOnSuccessListener {
            realtimedb.getReference("PostStats").child(postId).removeValue()
            .addOnSuccessListener {
                db.collection("comments").whereEqualTo("postId", postId).get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            for (document in result) {
                                db.collection("comments").document(document.id).delete()
                            }
                        }
                        db.collection("Likes").whereEqualTo("postid", postId).get()
                        .addOnSuccessListener { result ->
                            if (!result.isEmpty) {
                                for (document in result) {
                                    db.collection("Likes").document(document.id).delete()
                                }
                            }
                        }
                    }
            }
            .addOnFailureListener {
                Result.retry()
            }
        }
    }

    private suspend fun hidePost(postId: String) {
        val userDoc=db.collection("Users").document(auth.currentUser?.uid?:"").get().await()
        if (userDoc.exists()) {
            db.collection("Users").document(userDoc.id).update("hiddenPosts", FieldValue.arrayUnion(postId)).await()
        }
    }

    private suspend fun unhidePost(postId: String) {
        val userDoc=db.collection("Users").document(auth.currentUser?.uid?:"").get().await()
        if (userDoc.exists()) {
            db.collection("Users").document(userDoc.id).update("hiddenPosts", FieldValue.arrayRemove(postId)).await()
        }
    }
}