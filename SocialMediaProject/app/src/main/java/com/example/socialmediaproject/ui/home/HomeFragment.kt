package com.example.socialmediaproject.ui.home

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.socialmediaproject.adapter.FeedAdapter
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.R
import com.example.socialmediaproject.ui.chat.ChatViewModel
import com.example.socialmediaproject.ui.mainpage.MainPageFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

class HomeFragment : Fragment(), FeedAdapter.OnPostInteractionListener {

    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<PostViewModel>()
    private lateinit var homeviewmodel: HomeViewModel
    private lateinit var chatbutton: ImageView
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var badge: BadgeDrawable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        homeviewmodel=ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        chatViewModel=ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        chatbutton=view.findViewById(R.id.button_chat)
        chatbutton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatFragment)
        }
        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        return view
    }

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val glow=android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.vector_tittle_animation)
        view.findViewById<TextView>(R.id.textVector).startAnimation(glow)
        val chatContainer = view.findViewById<FrameLayout>(R.id.chat_icon_container)
        val chatButton = view.findViewById<ImageView>(R.id.button_chat)
        badge = BadgeDrawable.create(requireContext()).apply {
            isVisible = false
            maxCharacterCount = 3
            badgeGravity = BadgeDrawable.TOP_END
            setHorizontalOffset(dpToPx(-2f))
            setVerticalOffset(dpToPx(6f))
        }
        BadgeUtils.attachBadgeDrawable(badge, chatButton, chatContainer)
        chatViewModel.totalUnreadCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.clearNumber()
                badge.isVisible = false
            }
        }
    }

    private fun dpToPx(dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics).toInt()

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
        val post=homeviewmodel.postlist.value?.get(position)?:return
        val bundle=Bundle()
        bundle.putString("post_id", post.id)
        findNavController().navigate(R.id.navigation_comment_detail, bundle)
    }

    override fun onLikeCountClicked(postPosition: Int) {
        val post=homeviewmodel.postlist.value?.get(postPosition)?:return
        val bundle=Bundle()
        bundle.putString("post_id", post.id)
        findNavController().navigate(R.id.navigation_likedetail, bundle)
    }

    override fun onShareClicked(position: Int) {
        Toast.makeText(
            requireContext(),
            "Chia sẻ bài viết: ${postList[position].id}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onUserClicked(position: Int) {
        val post=homeviewmodel.postlist.value?.get(position)?:return
        val goToFragment=MainPageFragment()
        val bundle=Bundle()
        bundle.putString("wall_user_id", post.userId)
        goToFragment.arguments=bundle
        findNavController().navigate(R.id.action_homeFragment_to_mainPageFragment, bundle)
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
            val bundle = bundleOf("IMAGE_URL" to imageurl)
            findNavController().navigate(R.id.viewingimagefragment, bundle)
        }
    }
}