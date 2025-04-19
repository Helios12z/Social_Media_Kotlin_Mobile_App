package com.example.socialmediaproject.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.ChatUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {
    private val db: FirebaseFirestore=FirebaseFirestore.getInstance()
    private val _chatUsers=MutableLiveData<List<ChatUser>>()
    val chatUsers: LiveData<List<ChatUser>> = _chatUsers
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    fun loadFriends(currentUserId: String) {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()

        val userMap = mutableMapOf<String, ChatUser>()
        _chatUsers.value = emptyList()
        db.collection("Users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val friends = userDoc["friends"] as? List<String> ?: emptyList()
                val friendSet = friends.toMutableSet()
                db.collection("chats").get().addOnSuccessListener { chatDocs ->
                    for (chatDoc in chatDocs) {
                        val chatId = chatDoc.id
                        Log.d("CHAT ID: ", chatId.toString())
                        if (chatId.contains(currentUserId)) {
                            val parts = chatId.split("_")
                            if (parts.size == 2) {
                                val otherId = parts.first { it != currentUserId }
                                Log.d("OTHER USER ID: ", otherId.toString())
                                friendSet.add(otherId)
                            }
                        }
                    }

                    if (friendSet.isEmpty()) {
                        _chatUsers.value = emptyList()
                        return@addOnSuccessListener
                    }

                    for (userId in friendSet) {
                        db.collection("Users").document(userId).get()
                            .addOnSuccessListener { friendDoc ->
                                val chatUser = ChatUser(
                                    id = userId,
                                    username = friendDoc.getString("name") ?: "",
                                    avatarUrl = friendDoc.getString("avatarurl"),
                                    lastMessage = "",
                                    unreadCount = 0
                                )

                                userMap[userId] = chatUser
                                _chatUsers.value = userMap.values.toList()

                                val chatId = if (currentUserId < userId)
                                    "${currentUserId}_$userId"
                                else
                                    "${userId}_$currentUserId"

                                val lastMsgListener = db.collection("chats")
                                    .document(chatId)
                                    .collection("messages")
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .limit(1)
                                    .addSnapshotListener { snapshot, _ ->
                                        val lastMsg = snapshot?.documents?.firstOrNull()?.getString("text") ?: ""
                                        userMap[userId]?.let {
                                            it.lastMessage = lastMsg
                                            _chatUsers.postValue(userMap.values.toList())
                                        }
                                    }
                                val unreadListener = db.collection("chats")
                                    .document(chatId)
                                    .collection("messages")
                                    .whereEqualTo("receiverId", currentUserId)
                                    .whereEqualTo("read", false)
                                    .addSnapshotListener { snapshot, _ ->
                                        val unread = snapshot?.size() ?: 0
                                        userMap[userId]?.let {
                                            it.unreadCount = unread
                                            _chatUsers.postValue(userMap.values.toList())
                                        }
                                    }
                                listenerRegistrations.add(lastMsgListener)
                                listenerRegistrations.add(unreadListener)
                            }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistrations.forEach { it.remove() }
    }
}