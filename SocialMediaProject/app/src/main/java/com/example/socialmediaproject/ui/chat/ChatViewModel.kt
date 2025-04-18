package com.example.socialmediaproject.ui.chat

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
            val userMap = mutableMapOf<String, ChatUser>()
            _chatUsers.value = usersList
            for (friendId in friends) {
                db.collection("Users").document(friendId).get()
                .addOnSuccessListener { friendDoc ->
                    val chatUser = ChatUser(
                        id = friendId,
                        username = friendDoc.getString("name") ?: "",
                        avatarUrl = friendDoc.getString("avatarurl"),
                        lastMessage = "",
                        unreadCount = 0
                    )
                    userMap[friendId] = chatUser
                    _chatUsers.value = userMap.values.toList()
                    val chatId = if (currentUserId < friendId)
                        currentUserId + "_" + friendId
                    else
                        friendId + "_" + currentUserId
                    val lastMsgListener = db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.documents?.firstOrNull()?.let { doc ->
                                chatUser.lastMessage = doc.getString("text") ?: ""
                                _chatUsers.postValue(userMap.values.toList())
                            }
                        }
                    val unreadListener = db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .whereEqualTo("receiverId", currentUserId)
                        .whereEqualTo("read", false)
                        .addSnapshotListener { snapshot, _ ->
                            chatUser.unreadCount = snapshot?.size() ?: 0
                            _chatUsers.postValue(userMap.values.toList())
                        }
                    listenerRegistrations.add(lastMsgListener)
                    listenerRegistrations.add(unreadListener)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistrations.forEach { it.remove() }
    }
}