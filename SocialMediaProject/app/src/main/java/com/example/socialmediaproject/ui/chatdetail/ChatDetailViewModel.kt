package com.example.socialmediaproject.ui.chatdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatDetailViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages
    private var listenerRegistration: ListenerRegistration? = null

    fun startListeningMessages(chatId: String, currentUserId: String) {
        listenerRegistration = db.collection("chats")
        .document(chatId)
        .collection("messages")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { doc ->
                val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                if (message != null && message.receiverId == currentUserId && !message.read) {
                    doc.reference.update("read", true)
                }
                message
            }
            _messages.value = list
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
    }

    fun sendMessage(chatId: String, message: Message) {
        val docRef=db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()
        val messageWithId = message.copy(id = docRef.id)
        docRef.set(messageWithId)
    }

    fun loadMessages(chatId: String, currentUserId: String) {
        db.collection("chats")
        .document(chatId)
        .collection("messages")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { snapshot ->
            val messages = snapshot.documents.mapNotNull { doc ->
                val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                if (message != null && message.receiverId == currentUserId && !message.read) {
                    doc.reference.update("read", true)
                }
                message
            }
            _messages.value = messages
        }
    }
}