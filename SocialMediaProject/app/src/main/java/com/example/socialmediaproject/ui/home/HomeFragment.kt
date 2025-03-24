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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        auth=FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        getUserInterests(userId) { userInterests ->
            Log.d("USER INTERESTS: ", userInterests.toString())
            if (userInterests.isEmpty()) {
                Toast.makeText(requireContext(), "Không có bài viết phù hợp", Toast.LENGTH_SHORT)
                    .show()
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
                    for (doc in documents) {
                        val userid=doc.getString("userid")?:""
                        var likecount=0
                        var commentcount=0
                        var sharecount=0
                        var isliked=false
                        val poststatdbref=FirebaseDatabase.getInstance().getReference("PostStats").child(doc.id)
                        poststatdbref.get().addOnSuccessListener {
                                snapshot->if (snapshot.exists()) {
                            likecount=snapshot.child("likecount").getValue(Int::class.java)?:0
                            commentcount=snapshot.child("commentcount").getValue(Int::class.java)?:0
                            sharecount=snapshot.child("sharecount").getValue(Int::class.java)?:0
                            isliked=snapshot.child("isliked").getValue(Boolean::class.java)?:false
                        }
                        }
                        val user=db.collection("Users").document(userid).get().addOnSuccessListener {
                                result->
                            val post = PostViewModel(
                                id=doc.id,
                                userId=doc.getString("userid")?:"",
                                userName = result.getString("name")?:"",
                                userAvatarUrl = result.getString("avatarurl")?:"",
                                content=doc.getString("content")?:"",
                                category = doc.get("category") as List<String>,
                                imageUrls = doc.get("imageurl") as List<String>,
                                timestamp = doc.getLong("timestamp")?:0,
                                likeCount=likecount,
                                commentCount = commentcount,
                                shareCount = sharecount,
                                isLiked = isliked
                            )
                            postList.add(post)
                            feedAdapter.notifyDataSetChanged()
                        }
                    }
                }
        }
    }

    override fun onLikeClicked(position: Int) {
        val post = postList[position]
        post.isLiked = !post.isLiked
        if (post.isLiked) {
            post.likeCount += 1
        } else {
            post.likeCount -= 1
        }
        feedAdapter.notifyItemChanged(position)
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
        Toast.makeText(
            requireContext(),
            "Xem ảnh ${imagePosition + 1} của bài đăng ${post.id}",
            Toast.LENGTH_SHORT
        ).show()
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