package com.example.socialmediaproject.ui.likedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.LikesAdapter
import com.example.socialmediaproject.databinding.FragmentLikeDetailBinding
import com.example.socialmediaproject.dataclass.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialmediaproject.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class LikeDetailFragment : Fragment() {
    private lateinit var binding: FragmentLikeDetailBinding
    private lateinit var likesAdapter: LikesAdapter
    private val likedUsers = mutableListOf<User>()
    private lateinit var postId: String
    private val db=FirebaseFirestore.getInstance()
    private var allLikedUserIds = listOf<String>()
    private var currentPage = 0
    private val pageSize = 10
    private var isLoading = false
    private lateinit var viewModel: LikeDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentLikeDetailBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[LikeDetailViewModel::class.java]
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        postId = arguments?.getString("post_id") ?: return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        likesAdapter = LikesAdapter(mutableListOf()) { userId ->
            val bundle = Bundle().apply {
                putString("wall_user_id", userId)
            }
            findNavController().navigate(R.id.navigation_mainpage, bundle)
        }
        binding.rvLikes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = likesAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    if (lastVisible >= totalItemCount - 2) {
                        viewModel.loadNextPage(currentUserId)
                    }
                }
            })
        }

        viewModel.likedUsers.observe(viewLifecycleOwner) { users ->
            likesAdapter.submitList(users)
        }

        viewModel.loadInitial(postId, currentUserId)
        viewModel.observePostSummary(postId)

        viewModel.postSummary.observe(viewLifecycleOwner) { summary ->
            binding.tvPostTitle.text = summary.content

            Glide.with(requireContext())
                .load(summary.avatarUrl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(binding.ivPostThumbnail)

            binding.tvLikeCount.text = "${summary.likeCount} lượt thích"
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }
}