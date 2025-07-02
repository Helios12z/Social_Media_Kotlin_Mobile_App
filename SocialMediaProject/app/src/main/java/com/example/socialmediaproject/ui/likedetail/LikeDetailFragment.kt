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
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
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
        loadPostSummary()
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    private fun loadPostSummary() {
        val loading=LoadingDialogFragment()
        loading.show(childFragmentManager, "loading")
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
                            loading.dismiss()
                        }
                        .addOnFailureListener {
                            loading.dismiss()
                            Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else {
                        loading.dismiss()
                        Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    loading.dismiss()
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                loading.dismiss()
                Toast.makeText(requireContext(), "Bài viết không tồn tại", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            loading.dismiss()
            Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
        }
    }
}