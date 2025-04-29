package com.example.socialmediaproject.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.Constant
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
    private val userMap = mutableMapOf<String, ChatUser>()
    private val _totalUnreadCount = MutableLiveData(0)
    val totalUnreadCount: LiveData<Int> = _totalUnreadCount

    fun initializeFriends(userId: String) {
        if (isInitialized && userId == currentUserId) return
        cleanupListeners()
        currentUserId = userId
        _chatUsers.value = emptyList()
        friendsListener = db.collection("Users").document(userId)
            .addSnapshotListener { userDoc, error ->
                if (error != null || userDoc == null) return@addSnapshotListener
                val friends = userDoc["friends"] as? List<String> ?: emptyList()
                val initialSet = friends.toMutableSet().apply {
                    add(Constant.ChatConstants.VECTOR_AI_ID)
                }
                loadChatPartners(currentUserId, initialSet)
            }
        isInitialized = true
    }

    private fun loadChatPartners(currentUserId: String, initialFriendSet: MutableSet<String>) {
        userMap.clear()
        userMap[Constant.ChatConstants.VECTOR_AI_ID] = ChatUser(
            id = Constant.ChatConstants.VECTOR_AI_ID,
            username = Constant.ChatConstants.VECTOR_AI_NAME,
            avatarUrl = Constant.ChatConstants.VECTOR_AI_AVATAR_URL,
            lastMessage = "",
            unreadCount = 0,
            timestamp = 0
        )
        setupChatListeners(currentUserId, Constant.ChatConstants.VECTOR_AI_ID)
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
            for (partnerId in friendSet) {
                if (partnerId == Constant.ChatConstants.VECTOR_AI_ID) continue
                db.collection("Users").document(partnerId).get()
                .addOnSuccessListener { friendDoc ->
                    val chatUser = ChatUser(
                        id = partnerId,
                        username = friendDoc.getString("name") ?: "",
                        avatarUrl = friendDoc.getString("avatarurl"),
                        lastMessage = "",
                        unreadCount = 0,
                        timestamp = 0
                    )
                    userMap[partnerId] = chatUser
                    sortAndUpdateChatUsers()
                    setupChatListeners(currentUserId, partnerId)
                }
            }
        }
    }

    private fun setupChatListeners(currentUserId: String, partnerId: String) {
        val chatId = if (currentUserId < partnerId) "${currentUserId}_$partnerId"
        else "${partnerId}_$currentUserId"
        val lastMsgListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                val lastMsgDoc = snapshot?.documents?.firstOrNull()
                if (lastMsgDoc!=null) {
                    val lastMsg = lastMsgDoc?.getString("text") ?: ""
                    val senderId = lastMsgDoc?.getString("senderId") ?: ""
                    val timestamp = lastMsgDoc?.getTimestamp("timestamp")?.toDate()?.time ?: 0
                    val prefix = when (senderId) {
                        currentUserId -> "Bạn"
                        Constant.ChatConstants.VECTOR_AI_ID -> "VectorAI:"
                        else -> userMap[partnerId]?.username ?: ""
                    }
                    userMap[partnerId]?.let {
                        it.lastMessage = "$prefix: $lastMsg"
                        it.timestamp = timestamp
                        sortAndUpdateChatUsers()
                    }
                }
                else {
                    userMap[partnerId]?.let {
                        it.lastMessage = "Chưa có tin nhắn"
                        sortAndUpdateChatUsers()
                    }
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
                    sortAndUpdateChatUsers()
                }
            }
        listenerRegistrations += lastMsgListener
        listenerRegistrations += unreadListener
    }

    private fun sortAndUpdateChatUsers() {
        val sorted = userMap.values.sortedWith(
            compareByDescending<ChatUser> { it.unreadCount }.thenByDescending { it.timestamp }
        )
        _chatUsers.postValue(sorted)
        val total = userMap.values.sumBy { it.unreadCount }
        _totalUnreadCount.postValue(total)
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