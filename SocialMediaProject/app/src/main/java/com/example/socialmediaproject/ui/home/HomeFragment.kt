package com.example.socialmediaproject.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialmediaproject.FeedAdapter
import com.example.socialmediaproject.PostViewModel
import com.example.socialmediaproject.R
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

class HomeFragment : Fragment(), FeedAdapter.OnPostInteractionListener {

    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<PostViewModel>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        loadPosts()
        return view
    }

    private fun initViews(view: View) {
        recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(requireContext(), postList, this)
        recyclerViewFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
        swipeRefreshLayout.setOnRefreshListener {
            refreshFeed()
        }
    }

    private fun refreshFeed() {
        loadPosts()
        swipeRefreshLayout.isRefreshing = false
        Toast.makeText(requireContext(), "Đã làm mới bảng tin", Toast.LENGTH_SHORT).show()
    }

    private fun loadPosts() {
        postList.clear()
        val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        getUserInterests(userId) { userInterests ->
            Log.d("USER INTERESTS: ", userInterests.toString())
            if (userInterests.isEmpty()) {
                Toast.makeText(requireContext(), "Không có bài viết phù hợp", Toast.LENGTH_SHORT).show()
                return@getUserInterests
            }
            val db = FirebaseFirestore.getInstance()
            val cleanedUserInterests = userInterests.map { it.trim() }
            db.collection("Posts")
                .whereArrayContainsAny("category", cleanedUserInterests)
                .whereEqualTo("privacy", "Công khai")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    postList.clear()
                    Log.d("DOCUMENTS SIZE: ", documents.size().toString())
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
                        finalPostList.sortByDescending { it.timestamp }
                        postList.clear()
                        postList.addAll(finalPostList)
                        feedAdapter.notifyDataSetChanged()
                    }
                }
        }
    }


    override fun onLikeClicked(position: Int) {
        db=FirebaseFirestore.getInstance()
        val post = postList[position]
        val auth=FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        val realtimedb=Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val likeRef = db.collection("Likes").whereEqualTo("userid", userId).whereEqualTo("postid", post.id).get().addOnSuccessListener {
            results-> if (!results.isEmpty) {
                for (result in results) {
                    post.isLiked=result.getBoolean("status")?:false
                }
                val ref=realtimedb.getReference("PostStats").child(post.id)
                ref.get().addOnSuccessListener {
                        result->
                    val likecount=result.child("likecount").getValue(Int::class.java)?:0
                    val updates = HashMap<String, Any>()
                    if (post.isLiked) {
                        updates["likecount"] = (likecount - 1).coerceAtLeast(0)
                        post.likeCount -= 1
                        post.isLiked=false
                        post.isLiked=false
                        ref.updateChildren(updates).addOnSuccessListener {  }
                        db.collection("Likes").whereEqualTo("userid", userId).whereEqualTo("postid", post.id).get().addOnSuccessListener {
                            results->if (!results.isEmpty) {
                                for (result in results) {
                                    db.collection("Likes").document(result.id).delete().addOnSuccessListener {  }
                                }
                            }
                        }
                        feedAdapter.notifyItemChanged(position)
                    }
                    else {
                        updates["likecount"] = likecount + 1
                        post.likeCount += 1
                        post.isLiked=true
                        db.collection("Likes").whereEqualTo("userid", userId).whereEqualTo("postid", post.id).get().addOnSuccessListener {
                            results->if (!results.isEmpty) {
                                for (document in results.documents) {
                                    db.collection("Likes").document(document.id).update("status", true).addOnSuccessListener {  }
                                }
                            }
                            else {
                                val item= hashMapOf(
                                    "userid" to userId,
                                    "postid" to post.id,
                                    "status" to true
                                )
                                db.collection("Likes").add(item).addOnSuccessListener { }
                            }
                        }
                    }
                    ref.updateChildren(updates).addOnCompleteListener { }
                    feedAdapter.notifyItemChanged(position)
                }
            }
            else {
                val ref=realtimedb.getReference("PostStats").child(post.id)
                ref.get().addOnSuccessListener {
                    result->
                        val likecount=result.child("likecount").getValue(Int::class.java)?:0
                        val updates = HashMap<String, Any>()
                        if (post.isLiked) {
                            updates["likecount"] = (likecount - 1).coerceAtLeast(0)
                            post.likeCount -= 1
                            post.isLiked=false
                            ref.updateChildren(updates).addOnSuccessListener {  }
                            db.collection("Likes").whereEqualTo("userid", userId).whereEqualTo("postid", post.id).get().addOnSuccessListener {
                                results->if (!results.isEmpty) {
                                    for (result in results) {
                                        db.collection("Likes").document(result.id).delete().addOnSuccessListener {  }
                                    }
                                }
                            }
                            feedAdapter.notifyItemChanged(position)
                        }
                        else {
                            updates["likecount"] = likecount + 1
                            post.likeCount += 1
                            post.isLiked=true
                            db.collection("Likes").whereEqualTo("userid", userId).whereEqualTo("postid", post.id).get().addOnSuccessListener {
                                results->if (!results.isEmpty) {
                                for (document in results.documents) {
                                    db.collection("Likes").document(document.id).update("status", true).addOnSuccessListener {  }
                                }
                            }
                            else {
                                val item= hashMapOf(
                                    "userid" to userId,
                                    "postid" to post.id,
                                    "status" to true
                                )
                                db.collection("Likes").add(item).addOnSuccessListener { }
                            }
                            }
                        }
                        ref.updateChildren(updates).addOnCompleteListener { }
                        feedAdapter.notifyItemChanged(position)
                    }
                }
        }
        .addOnFailureListener{e->
            Log.e("LOI LAY DATABASE: ", e.toString())
        }
    }

    override fun onCommentClicked(position: Int) {
        Toast.makeText(
            requireContext(),
            "Mở bình luận cho bài viết: ${postList[position].id}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onShareClicked(position: Int) {
        Toast.makeText(
            requireContext(),
            "Chia sẻ bài viết: ${postList[position].id}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onUserClicked(position: Int) {
        Toast.makeText(
            requireContext(),
            "Xem trang cá nhân của: ${postList[position].userName}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onMoreOptionsClicked(position: Int, anchorView: View) {
        PopupMenu(requireContext(), anchorView).apply {
            //inflate(R.menu.post_options_menu)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    /*R.id.action_save -> {
                        Toast.makeText(requireContext(), "Đã lưu bài viết", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_report -> {
                        Toast.makeText(requireContext(), "Đã báo cáo bài viết", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_hide -> {
                        postList.removeAt(position)
                        feedAdapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "Đã ẩn bài viết", Toast.LENGTH_SHORT).show()
                        true
                    }*/
                    else -> false
                }
            }
            show()
        }
    }

    override fun onImageClicked(postPosition: Int, imagePosition: Int) {
        val post = postList[postPosition]
    }

    private fun getUserInterests(userId: String, callback: (List<String>) -> Unit) {
        db = FirebaseFirestore.getInstance()
        db.collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val interests = document.get("interests") as? List<String> ?: emptyList()
                    callback(interests)
                } else {
                    callback(emptyList())
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
}