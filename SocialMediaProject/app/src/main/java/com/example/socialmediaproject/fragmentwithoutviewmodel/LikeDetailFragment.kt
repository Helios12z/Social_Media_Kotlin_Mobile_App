package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.LikesAdapter
import com.example.socialmediaproject.databinding.FragmentLikeDetailBinding
import com.example.socialmediaproject.dataclass.User
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class LikeDetailFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentLikeDetailBinding
    private lateinit var likesAdapter: LikesAdapter
    private val likedUsers = mutableListOf<User>()
    private lateinit var postId: String
    private val db=FirebaseFirestore.getInstance()
    private var allLikedUserIds = listOf<String>()
    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentLikeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postId = arguments?.getString("post_id") ?: return
        setupRecyclerView()
        loadPostSummary()
        loadLikedUsers()
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupRecyclerView() {
        likesAdapter = LikesAdapter(likedUsers)
        binding.rvLikes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = likesAdapter
        }
        binding.rvLikes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (!isLoading && lastVisibleItem >= totalItemCount - 2) {
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                    db.collection("Users").document(currentUserId).get()
                    .addOnSuccessListener { currentUserDoc ->
                        val friendList = currentUserDoc["friends"] as? List<*> ?: emptyList<String>()
                        loadNextPage(friendList)
                    }
                }
            }
        })
    }

    private fun loadPostSummary() {
        db.collection("Posts").document(postId).get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) {
                val content = doc.getString("content") ?: ""
                binding.tvPostTitle.text = content
                db.collection("Users").document(doc.getString("userid")?:"").get().addOnSuccessListener {
                    result->if (result.exists()) {
                        val avatarurl=result.getString("avatarurl")
                        Glide.with(requireContext()).load(avatarurl)
                            .placeholder(R.drawable.avataricon)
                            .error(R.drawable.avataricon)
                            .into(binding.ivPostThumbnail)
                        db.collection("Likes")
                        .whereEqualTo("postid", postId)
                        .whereEqualTo("status", true)
                        .get()
                        .addOnSuccessListener {
                            binding.tvLikeCount.text = "${it.size()} lượt thích"
                        }
                    }
                }
            }
        }
    }

    private fun loadLikedUsers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("Users").document(currentUserId).get()
        .addOnSuccessListener { currentUserDoc ->
            val friendList = currentUserDoc["friends"] as? List<*> ?: emptyList<String>()
            db.collection("Likes")
                .whereEqualTo("postid", postId)
                .whereEqualTo("status", true)
                .get()
                .addOnSuccessListener { likeDocs ->
                    allLikedUserIds = likeDocs.mapNotNull { it.getString("userid") }
                    currentPage = 0
                    likedUsers.clear()
                    loadNextPage(friendList)
                }
        }
    }

    private fun loadNextPage(friendList: List<*>) {
        if (isLoading) return
        isLoading = true
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allLikedUserIds.size)
        if (start >= end) {
            isLoading = false
            return
        }
        val pageIds = allLikedUserIds.subList(start, end)
        db.collection("Users")
        .whereIn(FieldPath.documentId(), pageIds)
        .get()
        .addOnSuccessListener { userDocs ->
            for (doc in userDocs) {
                val id = doc.id
                val name = doc.getString("name") ?: ""
                val avatar = doc.getString("avatarurl") ?: ""
                val isFriend = friendList.contains(id)
                val email = if (isFriend) "Bạn bè" else "Người lạ"
                likedUsers.add(User(id, name, avatar, email))
            }
            likesAdapter.notifyDataSetChanged()
            currentPage++
            isLoading = false
        }
        .addOnFailureListener {
            isLoading = false
        }
    }
}