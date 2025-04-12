package com.example.socialmediaproject.ui.mainpage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.FeedAdapter
import com.example.socialmediaproject.databinding.FragmentMainPageBinding
import com.example.socialmediaproject.dataclass.PostViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainPageFragment : Fragment(), FeedAdapter.OnPostInteractionListener {
    private lateinit var viewModel: MainPageViewModel
    private lateinit var binding: FragmentMainPageBinding
    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<PostViewModel>()
    private var wallUserId =  ""
    private var isCurrentUserFlag = false
    private var isFriendFlag = false
    private var isSendingFriendRequest = false
    private var isReceivingFriendRequest = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentMainPageBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[MainPageViewModel::class.java]
        wallUserId = arguments?.getString("wall_user_id") ?: ""
        viewModel.wallUserId=wallUserId
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        initViews()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.userInfo.observe(viewLifecycleOwner) {
            user-> if (user.userId!=wallUserId || viewModel.postlist.value.isNullOrEmpty()) {
               viewModel.loadPosts()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadUserData(wallUserId)
        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            Glide.with(requireContext())
                .load(user.avatarUrl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(binding.profileAvatar)
            binding.profileUsername.text = user.name
            if (user.bio.isEmpty()) {
                binding.profileBio.visibility = View.GONE
            } else {
                binding.profileBio.visibility = View.VISIBLE
                binding.profileBio.text = user.bio
            }
            Glide.with(requireContext())
                .load(user.wallUrl)
                .placeholder(R.color.white)
                .error(R.color.white)
                .into(binding.wallImage)
            binding.profileAvatar.setOnClickListener {
                if (user.avatarUrl.isNotEmpty()) {
                    val bundle = bundleOf("IMAGE_URL" to user.avatarUrl)
                    findNavController().navigate(R.id.viewingimagefragment, bundle)
                }
            }
            binding.wallImage.setOnClickListener {
                if (user.wallUrl.isNotEmpty()) {
                    val bundle = bundleOf("IMAGE_URL" to user.wallUrl)
                    findNavController().navigate(R.id.viewingimagefragment, bundle)
                }
            }
        }
        viewModel.isCurrentUser.observe(viewLifecycleOwner) { isCurrent ->
            isCurrentUserFlag = isCurrent
            updateFriendshipUI()
        }
        viewModel.isFriend.observe(viewLifecycleOwner) { isFriend ->
            isFriendFlag = isFriend
            updateFriendshipUI()
        }
        viewModel.followersCount.observe(viewLifecycleOwner) {
            binding.profileFollowersCount.text = it.toString()
        }
        viewModel.postsCount.observe(viewLifecycleOwner) {
            binding.profilePostsCount.text = it.toString()
        }
        binding.buttonAddFriend.setOnClickListener {
            if (binding.buttonAddFriend.text == "Đã gửi lời mời") {
                showBottomSheetUnfriend()
            }
            else viewModel.sendFriendRequest(binding.buttonAddFriend, binding.buttonChat, wallUserId)
        }
        binding.buttonUnfriend.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun showBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val btn1 = view.findViewById<Button>(R.id.button1)
        val btn2 = view.findViewById<Button>(R.id.button2)
        btn1.text = "Xóa kết bạn"
        btn2.text = "Hủy"
        btn1.setOnClickListener {
            viewModel.unfriend(binding.buttonUnfriend, binding.buttonChat, binding.buttonAddFriend, wallUserId)
            dialog.dismiss()
        }
        btn2.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showBottomSheetUnfriend() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val btn1 = view.findViewById<Button>(R.id.button1)
        val btn2 = view.findViewById<Button>(R.id.button2)
        btn1.text = "Hủy lời mời"
        btn2.text = "Hủy"
        btn1.setOnClickListener {
            viewModel.cancelFriendRequest(binding.buttonAddFriend, wallUserId)
            dialog.dismiss()
        }
        btn2.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    private fun initViews() {
        recyclerViewFeed = binding.recyclerViewPosts
        swipeRefreshLayout = binding.swipeRefreshLayout
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
            viewModel.refreshFeed()
        }
    }

    private fun observeViewModel() {
        viewModel.postlist.observe(viewLifecycleOwner) { posts ->
            feedAdapter.updatePosts(posts)
        }
        viewModel.isloading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    override fun onLikeClicked(position: Int) {
        val post=viewModel.postlist.value?.get(position)?:return
        viewModel.toggleLike(post, position)
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
        val post=viewModel.postlist.value?.get(position)?:return
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
        val post=viewModel.postlist.value?.get(postPosition)?:return
        val images=post?.imageUrls
        val imageurl=images?.get(imagePosition)
        if (imageurl!=null) {
            val bundle = bundleOf("IMAGE_URL" to imageurl)
            findNavController().navigate(R.id.viewingimagefragment, bundle)
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    private fun updateFriendshipUI() {
        if (isCurrentUserFlag) {
            binding.buttonAddFriend.visibility = View.GONE
            binding.buttonUnfriend.visibility = View.GONE
            binding.buttonChat.visibility = View.GONE
        } else {
            if (isFriendFlag) {
                binding.buttonAddFriend.visibility = View.GONE
                binding.buttonUnfriend.visibility = View.VISIBLE
                binding.buttonChat.visibility = View.VISIBLE
            } else {
                binding.buttonAddFriend.visibility = View.VISIBLE
                binding.buttonUnfriend.visibility = View.GONE
                binding.buttonChat.visibility = View.GONE
            }
        }
    }
}