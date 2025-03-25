package com.example.socialmediaproject.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialmediaproject.FeedAdapter
import com.example.socialmediaproject.PostViewModel
import com.example.socialmediaproject.R
import com.example.socialmediaproject.fragmentwithoutviewmodel.ViewingImageFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(), FeedAdapter.OnPostInteractionListener {

    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<PostViewModel>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var homeviewmodel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        homeviewmodel=ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

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
            homeviewmodel.refreshFeed()
        }
    }

    private fun observeViewModel() {
        homeviewmodel.postlist.observe(viewLifecycleOwner) { posts ->
            feedAdapter.updatePosts(posts)
        }

        homeviewmodel.isloading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    override fun onLikeClicked(position: Int) {
        val post=homeviewmodel.postlist.value?.get(position)?:return
        homeviewmodel.toggleLike(post, position)
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
        val post=homeviewmodel.postlist.value?.get(postPosition)?:return
        val images=post?.imageUrls
        val imageurl=images?.get(imagePosition)
        if (imageurl!=null) {
            val fragment= ViewingImageFragment.newInstance(imageurl)
            requireActivity().supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment_activity_main, fragment)
                .addToBackStack(null)
                .commit()
        }
    }
}