package com.example.socialmediaproject.ui.chatdetail

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.socialmediaproject.dataclass.Message
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ChatDetailViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages
    private var listenerRegistration: ListenerRegistration? = null
    private var lastVisibleMessage: DocumentSnapshot? = null
    private var isLoadingMore = false

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
        val chatRef = db.collection("chats").document(chatId)
        chatRef.set(mapOf("exists" to true), SetOptions.merge())
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
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(20)
        .get()
        .addOnSuccessListener { snapshot ->
            val documents = snapshot.documents.reversed()
            val messages = documents.mapNotNull { doc ->
                val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                if (message != null && message.receiverId == currentUserId && !message.read) {
                    doc.reference.update("read", true)
                }
                message
            }
            lastVisibleMessage = snapshot.documents.lastOrNull()
            _messages.value = messages
        }
    }

    fun loadOlderMessages(chatId: String, currentUserId: String, context: Context) {
        if (isLoadingMore || lastVisibleMessage == null) return
        isLoadingMore = true
        lastVisibleMessage?.let { lastDoc ->
            db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastDoc)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents.reversed()
                val newMessages = documents.mapNotNull { doc ->
                    val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                    if (message != null && message.receiverId == currentUserId && !message.read) {
                        doc.reference.update("read", true)
                    }
                    message
                }
                lastVisibleMessage = snapshot.documents.lastOrNull()
                val currentList = _messages.value?.toMutableList() ?: mutableListOf()
                _messages.postValue(newMessages + currentList)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load more messages", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                isLoadingMore = false
            }
        }
    }
}