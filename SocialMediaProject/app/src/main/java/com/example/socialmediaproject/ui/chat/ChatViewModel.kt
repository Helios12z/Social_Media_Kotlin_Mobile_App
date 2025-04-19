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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _chatUsers = MutableLiveData<List<ChatUser>>()
    val chatUsers: LiveData<List<ChatUser>> = _chatUsers
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private var friendsListener: ListenerRegistration? = null
    private var isInitialized = false
    private var currentUserId = ""

    fun initializeFriends(userId: String) {
        if (isInitialized && userId == currentUserId) return
        cleanupListeners()
        currentUserId = userId
        _chatUsers.value = emptyList()
        friendsListener = db.collection("Users").document(userId)
            .addSnapshotListener { userDoc, error ->
                if (error != null || userDoc == null) {
                    Log.e("ChatViewModel", "Error listening to user doc", error)
                    return@addSnapshotListener
                }
                val friends = userDoc["friends"] as? List<String> ?: emptyList()
                loadChatPartners(userId, friends.toMutableSet())
            }
        isInitialized = true
    }

    private fun loadChatPartners(currentUserId: String, initialFriendSet: MutableSet<String>) {
        val userMap = mutableMapOf<String, ChatUser>()
        db.collection("chats").get().addOnSuccessListener { chatDocs ->
            val friendSet = initialFriendSet.toMutableSet()
            for (chatDoc in chatDocs) {
                val chatId = chatDoc.id
                if (chatId.contains(currentUserId)) {
                    val parts = chatId.split("_")
                    if (parts.size == 2) {
                        val otherId = parts.first { it != currentUserId }
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
                    setupChatListeners(currentUserId, userId, userMap)
                }
            }
        }
    }

    private fun setupChatListeners(currentUserId: String, partnerId: String, userMap: MutableMap<String, ChatUser>) {
        val chatId = if (currentUserId < partnerId) "${currentUserId}_$partnerId"
        else "${partnerId}_$currentUserId"
        val lastMsgListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                val lastMsg = snapshot?.documents?.firstOrNull()?.getString("text") ?: ""
                userMap[partnerId]?.let {
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
                userMap[partnerId]?.let {
                    it.unreadCount = unread
                    _chatUsers.postValue(userMap.values.toList())
                }
            }
        listenerRegistrations.add(lastMsgListener)
        listenerRegistrations.add(unreadListener)
    }

    private fun cleanupListeners() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        friendsListener?.remove()
        friendsListener = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }
}