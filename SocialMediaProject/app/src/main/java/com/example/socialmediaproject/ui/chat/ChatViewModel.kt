package com.example.socialmediaproject.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.ChatUser
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel : ViewModel() {
    private val db: FirebaseFirestore=FirebaseFirestore.getInstance()
    private val _chatUsers=MutableLiveData<List<ChatUser>>()
    val chatUsers: LiveData<List<ChatUser>> = _chatUsers

    fun loadFriends(currentUserId: String) {
        db.collection("Users")
        .document(currentUserId)
        .get()
        .addOnSuccessListener { doc ->
            val friends = doc["friends"] as? List<String> ?: emptyList()
            if (friends.isEmpty()) {
                _chatUsers.value = emptyList()
                return@addOnSuccessListener
            }
            val usersList = mutableListOf<ChatUser>()
            var loaded = 0
            for (friendId in friends) {
                db.collection("Users").document(friendId).get()
                .addOnSuccessListener { friendDoc ->
                    val chatUser = ChatUser(
                        id = friendId,
                        username = friendDoc.getString("name") ?: "",
                        avatarUrl = friendDoc.getString("avatarurl"),
                    )
                    usersList.add(chatUser)
                }
                .addOnCompleteListener {
                    loaded++
                    if (loaded == friends.size) {
                        _chatUsers.value = usersList
                    }
                }
            }
        }
    }
}