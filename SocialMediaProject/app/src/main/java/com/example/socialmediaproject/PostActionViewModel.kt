package com.example.socialmediaproject

import android.content.Context
import androidx.lifecycle.ViewModel
import android.app.AlertDialog
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class PostActionViewModel: ViewModel() {
    private val db=FirebaseFirestore.getInstance()

    fun deletePost(postId: String, context: Context) {
        AlertDialog.Builder(context).setTitle("Xác nhận xóa")
        .setTitle("Bạn có chắc chắn muốn xóa post này?")
        .setPositiveButton("Có") {
            _, _ ->db.collection("Posts").document(postId).delete().addOnSuccessListener {
                Toast.makeText(context, "Xóa post thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Xóa post thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Không", null)
        .show()
    }

    fun editPost(postId: String) {

    }

    fun hideOrUnhidePost(postId: String) {

    }
}