package com.example.socialmediaproject.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.example.socialmediaproject.PostViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class HomeViewModel : ViewModel() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")

    private val _postlist=MutableLiveData<List<PostViewModel>>()
    val postlist: LiveData<List<PostViewModel>> = _postlist

    private val _isloading= MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> = _isloading

    init {
        loadPosts()
    }

    private fun loadPosts() {
        _isloading.value=true
        auth = FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        getUserInterests(userId) { userInterests ->
            Log.d("USER INTERESTS: ", userInterests.toString())
            if (userInterests.isEmpty()) {
                _isloading.value=false
                _postlist.value= emptyList()
                return@getUserInterests
            }
            val cleanedUserInterests = userInterests.map { it.trim() }
            db.collection("Posts")
                .whereArrayContainsAny("category", cleanedUserInterests)
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
                            .whereEqualTo("userid", userId)
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
                            Log.d("LIKE COUNT", likecount.toString())
                            val sharecount = ref.child("sharecount").getValue(Int::class.java) ?: 0
                            val commentcount = ref.child("commentcount").getValue(Int::class.java) ?: 0
                            var isliked = false
                            if (!likeResults.isEmpty) {
                                for (result in likeResults) {
                                    Log.d("LIKE USERID", result.getString("userid").toString())
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
    }

    fun refreshFeed() {
        loadPosts()
    }

    private fun getUserInterests(userId: String, callback: (List<String>) -> Unit) {
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val interests = document.get("interests") as? List<String> ?: emptyList()
                callback(interests)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    public fun toggleLike(post: PostViewModel, position: Int) {
        auth=FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()
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
}