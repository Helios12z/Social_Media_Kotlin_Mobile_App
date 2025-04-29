package com.example.socialmediaproject

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class Constant {
    object ChatConstants {
        const val VECTOR_AI_ID = "Ordinary_VectorAI"
        const val VECTOR_AI_NAME = "VectorAI"
        const val VECTOR_AI_AVATAR_URL = "@drawable/vectorai"
    }

    object PresenceHelper {
        private val db = FirebaseFirestore.getInstance()
        fun updateLastActive(uid: String) {
            db.collection("Users")
            .document(uid)
            .update("lastActive", FieldValue.serverTimestamp(),
                "status", "online")
            .addOnFailureListener {
                it.printStackTrace()
            }
        }
        fun setOffline(uid: String) {
            db.collection("Users")
            .document(uid)
            .update(
                "lastActive", FieldValue.serverTimestamp(),
                "status", "offline"
            )
        }
    }
}