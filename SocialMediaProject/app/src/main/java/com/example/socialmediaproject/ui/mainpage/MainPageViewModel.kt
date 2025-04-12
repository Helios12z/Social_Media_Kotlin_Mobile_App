package com.example.socialmediaproject.ui.mainpage

import android.util.Log
import android.view.View
import android.widget.Button
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaproject.dataclass.FriendRequest
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.dataclass.RequestStatus
import com.example.socialmediaproject.dataclass.UserMainPageInfo
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.concurrent.thread

class MainPageViewModel : ViewModel() {
    private val db: FirebaseFirestore=FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
    var wallUserId = ""

    private val _postlist=MutableLiveData<List<PostViewModel>>()
    val postlist: LiveData<List<PostViewModel>> = _postlist

    private val _isloading= MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> = _isloading

    val userInfo = MutableLiveData<UserMainPageInfo>()
    val isFriend = MutableLiveData<Boolean>()
    val isCurrentUser = MutableLiveData<Boolean>()
    val isSendingFriendRequest = MutableLiveData<Boolean>()
    val isReceivingFriendRequest = MutableLiveData<Boolean>()
    val followersCount = MutableLiveData<Int>()
    val postsCount = MutableLiveData<Int>()

    fun loadUserData(wallUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: ""
        db.collection("Users").document(wallUserId).get().addOnSuccessListener { result ->
            if (result.exists()) {
                val avatarUrl = result.getString("avatarurl") ?: ""
                val name = result.getString("name") ?: ""
                val bio = result.getString("bio") ?: ""
                val wallUrl = result.getString("wallurl") ?: ""
                userInfo.value = UserMainPageInfo(avatarUrl, result.id, name, bio, wallUrl)
                isCurrentUser.value = (currentUserId == wallUserId)
                val friendList = result.get("friends") as? List<String> ?: emptyList()
                followersCount.value = friendList.size
                db.collection("Posts").whereEqualTo("userid", wallUserId)
                    .get().addOnSuccessListener { posts ->
                        postsCount.value = posts.size()
                    }
                if (currentUserId != wallUserId) {
                    db.collection("Users").document(currentUserId).get().addOnSuccessListener { currentUserDoc ->
                        if (currentUserDoc.exists()) {
                            val currentUserFriends = currentUserDoc.get("friends") as? List<String>
                            isFriend.value = currentUserFriends?.contains(wallUserId) == true
                        }
                    }
                }
            }
        }
    }

    fun loadPosts() {
        _isloading.value=true
        val currentUserId=auth.currentUser?.uid ?: ""
        if (wallUserId==currentUserId) {
            db.collection("Posts")
            .whereEqualTo("userid", wallUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val finalPostList = mutableListOf<PostViewModel>()
                val tasks = mutableListOf<Task<*>>()
                Log.d("NUMBER OF POSTS GOT: ", documents.size().toString())
                for (doc in documents) {
                    val userid = doc.getString("userid") ?: ""
                    val userTask = db.collection("Users").document(userid).get()
                    val postStatsTask = realtimedb.getReference("PostStats").child(doc.id).get()
                    val likesTask = db.collection("Likes")
                    .whereEqualTo("userid", currentUserId)
                    .whereEqualTo("postid", doc.id)
                    .get()
                    tasks.add(userTask)
                    tasks.add(postStatsTask)
                    tasks.add(likesTask)
                    Tasks.whenAllComplete(listOf(userTask, postStatsTask, likesTask)).addOnSuccessListener { results ->
                        val userDoc = (results[0] as Task<DocumentSnapshot>).result
                        val ref = (results[1] as Task<DataSnapshot>).result
                        val likeResults = (results[2] as Task<QuerySnapshot>).result

                        val likecount = ref.child("likecount").getValue(Int::class.java) ?: 0
                        val sharecount = ref.child("sharecount").getValue(Int::class.java) ?: 0
                        val commentcount = ref.child("commentcount").getValue(Int::class.java) ?: 0
                        var isliked = false
                        if (!likeResults.isEmpty) {
                            for (result in likeResults) {
                                isliked = result.getBoolean("status") ?: false
                            }
                        }
                        val post = PostViewModel(
                            id = doc.id,
                            userId = userid,
                            userName = userDoc.getString("name") ?: "",
                            userAvatarUrl = userDoc.getString("avatarurl") ?: "",
                            content = doc.getString("content") ?: "",
                            category = doc.get("category") as? List<String> ?: emptyList(),
                            imageUrls = doc.get("imageurl") as? List<String> ?: emptyList(),
                            timestamp = doc.getLong("timestamp") ?: 0,
                            likeCount = likecount,
                            commentCount = commentcount,
                            shareCount = sharecount,
                            isLiked = isliked
                        )
                        finalPostList.add(post)
                    }
                }
                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    _postlist.value=finalPostList.sortedByDescending { it.timestamp }
                    _isloading.value=false
                }
            }
        }
        else {
            db.collection("Users").document(wallUserId).get().addOnSuccessListener {
                result->if (result.exists()) {
                    val friends=result.get("friends") as? List<String>
                    if (friends?.contains(currentUserId) == true) {
                        db.collection("Posts")
                        .whereEqualTo("userid", wallUserId)
                        .whereIn("privacy", listOf("Công khai", "Bạn bè"))
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { documents ->
                            val finalPostList = mutableListOf<PostViewModel>()
                            val tasks = mutableListOf<Task<*>>()
                            for (doc in documents) {
                                val userid = doc.getString("userid") ?: ""
                                val userTask = db.collection("Users").document(userid).get()
                                val postStatsTask = realtimedb.getReference("PostStats").child(doc.id).get()
                                val likesTask = db.collection("Likes")
                                    .whereEqualTo("userid", currentUserId)
                                    .whereEqualTo("postid", doc.id)
                                    .get()
                                tasks.add(userTask)
                                tasks.add(postStatsTask)
                                tasks.add(likesTask)
                                Tasks.whenAllComplete(listOf(userTask, postStatsTask, likesTask)).addOnSuccessListener { results ->
                                    val userDoc = (results[0] as Task<DocumentSnapshot>).result
                                    val ref = (results[1] as Task<DataSnapshot>).result
                                    val likeResults = (results[2] as Task<QuerySnapshot>).result

                                    val likecount = ref.child("likecount").getValue(Int::class.java) ?: 0
                                    val sharecount = ref.child("sharecount").getValue(Int::class.java) ?: 0
                                    val commentcount = ref.child("commentcount").getValue(Int::class.java) ?: 0
                                    var isliked = false
                                    if (!likeResults.isEmpty) {
                                        for (result in likeResults) {
                                            isliked = result.getBoolean("status") ?: false
                                        }
                                    }
                                    val post = PostViewModel(
                                        id = doc.id,
                                        userId = userid,
                                        userName = userDoc.getString("name") ?: "",
                                        userAvatarUrl = userDoc.getString("avatarurl") ?: "",
                                        content = doc.getString("content") ?: "",
                                        category = doc.get("category") as? List<String> ?: emptyList(),
                                        imageUrls = doc.get("imageurl") as? List<String> ?: emptyList(),
                                        timestamp = doc.getLong("timestamp") ?: 0,
                                        likeCount = likecount,
                                        commentCount = commentcount,
                                        shareCount = sharecount,
                                        isLiked = isliked
                                    )
                                    finalPostList.add(post)
                                }
                            }
                            Tasks.whenAllComplete(tasks).addOnSuccessListener {
                                _postlist.value=finalPostList.sortedByDescending { it.timestamp }
                                _isloading.value=false
                            }
                        }
                        .addOnFailureListener() {
                            Log.e("LOI LAY DATABASE: ", it.toString())
                        }
                    }
                    else {
                        db.collection("Posts")
                        .whereEqualTo("userid", wallUserId)
                        .whereEqualTo("privacy", "Công khai")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { documents ->
                            val finalPostList = mutableListOf<PostViewModel>()
                            val tasks = mutableListOf<Task<*>>()
                            for (doc in documents) {
                                val userid = doc.getString("userid") ?: ""
                                val userTask = db.collection("Users").document(userid).get()
                                val postStatsTask = realtimedb.getReference("PostStats").child(doc.id).get()
                                val likesTask = db.collection("Likes")
                                    .whereEqualTo("userid", currentUserId)
                                    .whereEqualTo("postid", doc.id)
                                    .get()
                                tasks.add(userTask)
                                tasks.add(postStatsTask)
                                tasks.add(likesTask)
                                Tasks.whenAllComplete(listOf(userTask, postStatsTask, likesTask)).addOnSuccessListener { results ->
                                    val userDoc = (results[0] as Task<DocumentSnapshot>).result
                                    val ref = (results[1] as Task<DataSnapshot>).result
                                    val likeResults = (results[2] as Task<QuerySnapshot>).result

                                    val likecount = ref.child("likecount").getValue(Int::class.java) ?: 0
                                    val sharecount = ref.child("sharecount").getValue(Int::class.java) ?: 0
                                    val commentcount = ref.child("commentcount").getValue(Int::class.java) ?: 0
                                    var isliked = false
                                    if (!likeResults.isEmpty) {
                                        for (result in likeResults) {
                                            isliked = result.getBoolean("status") ?: false
                                        }
                                    }
                                    val post = PostViewModel(
                                        id = doc.id,
                                        userId = userid,
                                        userName = userDoc.getString("name") ?: "",
                                        userAvatarUrl = userDoc.getString("avatarurl") ?: "",
                                        content = doc.getString("content") ?: "",
                                        category = doc.get("category") as? List<String> ?: emptyList(),
                                        imageUrls = doc.get("imageurl") as? List<String> ?: emptyList(),
                                        timestamp = doc.getLong("timestamp") ?: 0,
                                        likeCount = likecount,
                                        commentCount = commentcount,
                                        shareCount = sharecount,
                                        isLiked = isliked
                                    )
                                    finalPostList.add(post)
                                }
                            }
                            Tasks.whenAllComplete(tasks).addOnSuccessListener {
                                _postlist.value=finalPostList.sortedByDescending { it.timestamp }
                                _isloading.value=false
                            }
                        }
                        .addOnFailureListener() {
                            Log.e("LOI LAY DATABASE: ", it.toString())
                        }
                    }
                }
            }
        }
    }

    fun refreshFeed() {
        loadPosts()
    }

    fun toggleLike(post: PostViewModel, position: Int) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("Likes")
            .whereEqualTo("userid", userId)
            .whereEqualTo("postid", post.id)
            .get().addOnSuccessListener {
                    results ->
                if (!results.isEmpty) {
                    for (result in results) {
                        post.isLiked = result.getBoolean("status") ?: false
                    }
                    updateLikeStatus(post, position, userId, results)
                } else {
                    updateLikeStatus(post, position, userId, null)
                }
            }
            .addOnFailureListener { e->
                Log.e("LOI LAY DATABASE: ", e.toString())
            }
    }

    private fun updateLikeStatus(post: PostViewModel, position: Int, userId: String, results: QuerySnapshot?) {
        val ref=realtimedb.getReference("PostStats").child(post.id)
        ref.get().addOnSuccessListener { result ->
            val likeCount = result.child("likecount").getValue(Int::class.java) ?: 0
            val updates = HashMap<String, Any>()
            if (post.isLiked) {
                updates["likecount"] = (likeCount - 1).coerceAtLeast(0)
                post.likeCount -= 1
                post.isLiked = false
                results?.let {
                    for (result in it) {
                        db.collection("Likes").document(result.id).delete()
                    }
                }
            }
            else {
                updates["likecount"] = likeCount + 1
                post.likeCount += 1
                post.isLiked = true
                results?.let {
                    for (document in it.documents) {
                        db.collection("Likes").document(document.id).update("status", true)
                    }
                } ?: run {
                    val item = hashMapOf(
                        "userid" to userId,
                        "postid" to post.id,
                        "status" to true
                    )
                    db.collection("Likes").add(item)
                }
            }

            ref.updateChildren(updates).addOnCompleteListener {
                _postlist.value = _postlist.value
            }
        }
    }

    fun acceptFriendRequest() {

    }

    fun rejectFriendRequest() {

    }

    fun cancelFriendRequest(buttonCancle: Button, receiverId: String) {
        viewModelScope.launch {
            buttonCancle.isEnabled=false
            buttonCancle.text="Đang hủy"
            val senderId = auth.currentUser?.uid ?: return@launch
            try {
                val requestDocRef = db.collection("friend_requests").document("${senderId}_${receiverId}")
                val requestSnapshot = requestDocRef.get().await()
                if (!requestSnapshot.exists()) {
                    buttonCancle.text="Kết bạn"
                    buttonCancle.isEnabled=true
                    return@launch
                }
                requestDocRef.delete().await()
                buttonCancle.text="Kết bạn"
                buttonCancle.isEnabled=true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unfriend(buttonFriend: Button, buttonChat: Button, buttonAdd: Button, receiverId: String) {
        viewModelScope.launch {
            buttonFriend.isEnabled=false
            buttonChat.isEnabled=false
            val senderId = auth.currentUser?.uid ?: return@launch
            try {
                val userdoc=db.collection("Users").document(senderId).get().await()
                if (userdoc.exists()) {
                    val friendlist = userdoc.get("friends") as? List<String>
                    if (friendlist?.contains(receiverId) == false) {
                        buttonFriend.visibility= View.GONE
                        buttonChat.visibility=View.GONE
                        buttonAdd.visibility=View.VISIBLE
                        return@launch
                    }
                }
                buttonFriend.setText("Đang hủy kết bạn")
                db.collection("Users").document(senderId).update("friends", FieldValue.arrayRemove(receiverId)).await()
                db.collection("Users").document(receiverId).update("friends", FieldValue.arrayRemove(senderId)).await()
                buttonFriend.visibility= View.GONE
                buttonChat.visibility=View.GONE
                buttonAdd.visibility=View.VISIBLE
            }
            catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendFriendRequest(sendButton: Button, chatButton: Button, receiverId: String) {
        viewModelScope.launch {
            val senderId = auth.currentUser?.uid ?: return@launch
            try {
                val senderDoc = db.collection("Users").document(senderId).get().await()
                if (senderDoc.exists()) {
                    val friendList = senderDoc.get("friends") as? List<String>
                    if (friendList?.contains(receiverId) == true) {
                        sendButton.text = "Bạn bè"
                        chatButton.visibility = View.VISIBLE
                        return@launch
                    }
                }
                sendButton.text = "Đang gửi..."
                sendButton.isEnabled = false
                val requestId = "${senderId}_${receiverId}"
                val reverseRequestId = "${receiverId}_${senderId}"
                val requestRef = db.collection("friend_requests").document(requestId)
                val reverseRequestRef = db.collection("friend_requests").document(reverseRequestId)
                val existingRequest = requestRef.get().await()
                val reverseRequest = reverseRequestRef.get().await()
                if (reverseRequest.exists() && reverseRequest.getString("status") == "pending") {
                    db.runBatch { batch ->
                        batch.update(reverseRequestRef, "status", "accepted")
                        val userRef1 = db.collection("Users").document(senderId)
                        val userRef2 = db.collection("Users").document(receiverId)
                        batch.update(userRef1, "friends", FieldValue.arrayUnion(receiverId))
                        batch.update(userRef2, "friends", FieldValue.arrayUnion(senderId))
                    }.await()
                    sendButton.text = "Bạn bè"
                    chatButton.visibility = View.VISIBLE
                    sendButton.isEnabled = true
                    return@launch
                }
                if (!existingRequest.exists()) {
                    val newRequest = FriendRequest(
                        senderId = senderId,
                        receiverId = receiverId,
                        status = "pending",
                        notified = false,
                        timestamp = Date()
                    )
                    requestRef.set(newRequest).await()
                }
                sendButton.text = "Đã gửi lời mời"
                sendButton.isEnabled = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}