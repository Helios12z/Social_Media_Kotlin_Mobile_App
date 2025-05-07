package com.example.socialmediaproject.service

import android.content.Context
import android.app.AlertDialog
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore

class PostActionWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    private val db=FirebaseFirestore.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")

    override suspend fun doWork(): Result {
        val postId=inputData.getString("postId")?:return Result.failure()
        val action=inputData.getString("action")?:return Result.failure()
        return try{
            when(action) {
                "delete" -> deletePost(postId, applicationContext)
                "hide"->hidePost(postId)
                "unhide"->unhidePost(postId)
                "edit"->editPost(postId)
                else->{}
            }
            Result.success()
        }
        catch (e:Exception) {
            Result.retry()
        }
    }

    private suspend fun deletePost(postId: String, context: Context) {
        AlertDialog.Builder(context).setTitle("Xác nhận xóa")
        .setTitle("Bạn có chắc chắn muốn xóa post này?")
        .setPositiveButton("Có") { _, _ ->
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
                                                db.collection("Likes").document(document.id)
                                                    .delete()
                                            }
                                        }
                                    }
                            }
                        Toast.makeText(context, "Xóa post thành công!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Xóa post thất bại!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        .setNegativeButton("Không", null)
        .show()
    }


    private suspend fun editPost(postId: String) {

    }

    private suspend fun hidePost(postId: String) {

    }

    private suspend fun unhidePost(postId: String) {

    }
}