package com.example.socialmediaproject.ui.mainpage

import android.app.AlertDialog
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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.FeedAdapter
import com.example.socialmediaproject.databinding.FragmentMainPageBinding
import com.example.socialmediaproject.dataclass.ChatUser
import com.example.socialmediaproject.dataclass.Friend
import com.example.socialmediaproject.dataclass.Message
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.fragmentwithoutviewmodel.FriendShareDialogFragment
import com.example.socialmediaproject.service.PostActionWorker
import com.example.socialmediaproject.service.PostUpdatingService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialmediaproject.activity.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
    private var isBlockedUser = false
    private var isBlockedByUser = false
    private var loading : LoadingDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentMainPageBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[MainPageViewModel::class.java]
        wallUserId = arguments?.getString("wall_user_id") ?: ""
        if (viewModel.wallUserId!=wallUserId) {
            viewModel.resetState()
        }
        viewModel.wallUserId=wallUserId
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        initViews()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.userInfo.observe(viewLifecycleOwner) {
            user-> if (user.userId!=wallUserId || viewModel.postlist.value.isNullOrEmpty()) {
                viewModel.loadPosts()
            }
            checkBlockStatus()
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
            if (user.fullName.isEmpty()) {
                binding.profileFullName.visibility=View.GONE
            }
            else {
                binding.profileFullName.visibility=View.VISIBLE
                binding.profileFullName.text=user.fullName
            }
            Glide.with(requireContext())
                .load(user.wallUrl)
                .placeholder(R.color.surface_color)
                .error(R.color.surface_color)
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
        viewModel.isSendingFriendRequest.observe(viewLifecycleOwner) { isSending ->
            isSendingFriendRequest = isSending
            updateFriendshipUI()
        }
        viewModel.isReceivingFriendRequest.observe(viewLifecycleOwner) { isReceiving ->
            isReceivingFriendRequest = isReceiving
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
            else if (binding.buttonAddFriend.text == "Đã gửi cho bạn lời mời kết bạn") {
                showBottomSheetCheck()
            }
            else viewModel.sendFriendRequest(binding.buttonAddFriend, binding.buttonChat, wallUserId)
        }
        binding.buttonUnfriend.setOnClickListener {
            showBottomSheet()
        }
        binding.friendListButton.setOnClickListener {
            val bundle=Bundle()
            bundle.putString("user_id", wallUserId)
            findNavController().navigate(R.id.navigation_friend_list, bundle)
        }
        recyclerViewFeed.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val visibleCount = lm.childCount
                val totalCount = lm.itemCount
                val firstPos = lm.findFirstVisibleItemPosition()
                if (!viewModel.isLoadingMore
                    && viewModel.canLoadMore
                    && visibleCount + firstPos >= totalCount) {
                    viewModel.loadPosts(isInitialLoad = false)
                }
            }
        })
        binding.profileDetails.setOnClickListener {
            val bundle=Bundle()
            bundle.putString("userId", wallUserId)
            findNavController().navigate(R.id.navigation_detail_information, bundle)
        }
        binding.buttonChat.setOnClickListener {
            val chatUser=ChatUser(
                id=wallUserId,
                username=viewModel.userInfo.value?.name?:"",
                avatarUrl=viewModel.userInfo.value?.avatarUrl
            )
            val bundle=Bundle()
            bundle.putParcelable("chatUser", chatUser)
            findNavController().navigate(R.id.navigation_chatdetail, bundle)
        }
        binding.buttonCancelFriendRequest.setOnClickListener {
            viewModel.cancelFriendRequest(binding.buttonAddFriend, wallUserId)
        }
        binding.buttonAcceptFriendRequest.setOnClickListener {
            viewModel.acceptFriendRequest(binding.buttonUnfriend, binding.buttonChat, binding.buttonAddFriend, wallUserId)
        }
        binding.buttonDeclineFriendRequest.setOnClickListener {
            viewModel.rejectFriendRequest(binding.buttonAddFriend, wallUserId)
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

    private fun showBottomSheetCheck() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val btn1 = view.findViewById<Button>(R.id.button1)
        val btn2 = view.findViewById<Button>(R.id.button2)
        btn1.text = "Chấp nhận"
        btn2.text = "Từ chối"
        btn1.setOnClickListener {
            viewModel.acceptFriendRequest(binding.buttonUnfriend, binding.buttonChat, binding.buttonAddFriend, wallUserId)
            dialog.dismiss()
        }
        btn2.setOnClickListener {
            viewModel.rejectFriendRequest(binding.buttonAddFriend, wallUserId)
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).showNavigationWithBlur()
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
            if (isLoading) {
                loading=LoadingDialogFragment()
                loading?.show(parentFragmentManager, "loading")
            }
            else {
                loading?.dismiss()
                loading=null
            }
        }
    }

    override fun onLikeClicked(position: Int) {
        val post=viewModel.postlist.value?.get(position)?:return
        viewModel.toggleLike(post, position)
    }

    override fun onLikeCountClicked(postPosition: Int) {
        val post=viewModel.postlist.value?.get(postPosition)?:return
        val bundle=Bundle()
        bundle.putString("post_id", post.id)
        findNavController().navigate(R.id.navigation_likedetail, bundle)
    }

    override fun onCommentClicked(position: Int) {
        val post=viewModel.postlist.value?.get(position)?:return
        val bundle=Bundle()
        bundle.putString("post_id", post.id)
        findNavController().navigate(R.id.navigation_comment_detail, bundle)
    }

    override fun onShareClicked(position: Int) {
        val post = viewModel.postlist.value?.get(position) ?: return
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val shareLink = "Bài viết được chia sẻ"
        val dialog = FriendShareDialogFragment.newInstance()
        dialog.setOnFriendSelectedListener(object : FriendShareDialogFragment.OnFriendSelectedListener {
            override fun onFriendSelected(friend: Friend) {
                val receiverId = friend.id
                val chatId = if (senderId < receiverId)
                    "${senderId}_$receiverId"
                else
                    "${receiverId}_$senderId"
                val message = Message(
                    senderId = senderId,
                    receiverId = receiverId,
                    text = shareLink,
                    timestamp = Timestamp.now(),
                    link = true,
                    postId = post.id
                )
                sendMessage(
                    chatId = chatId,
                    message = message,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Chia sẻ thành công!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { e -> e.printStackTrace()
                        Toast.makeText(requireContext(), "Share thất bại", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        })
        dialog.show(parentFragmentManager, "FriendShareDialog")
    }

    override fun onUserClicked(position: Int) {
        //do nothing
    }

    override fun onMoreOptionsClicked(position: Int, anchorView: View) {
        val post=viewModel.postlist.value?.get(position)?:return
        val postId = post.id
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val hidden = (doc.get("hiddenPosts") as? List<String>) ?: emptyList()
                val isHidden = hidden.contains(postId)
                PopupMenu(requireContext(), anchorView).apply {
                    inflate(R.menu.post_management_menu)
                    menu.findItem(R.id.btnHideOrUnhidePost).title = if (isHidden) "Hủy ẩn bài đăng" else "Ẩn bài đăng"
                    menu.findItem(R.id.btnDeletePost).isVisible=(post.userId==uid || doc.getString("role").equals("Admin"))
                    menu.findItem(R.id.btnEditPost).isVisible=(post.userId==uid)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.btnDeletePost -> {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Xác nhận xóa")
                                    .setMessage("Bạn có chắc chắn muốn xóa post này?")
                                    .setPositiveButton("Có") { _, _ ->
                                        val data = workDataOf(
                                            "postId" to postId,
                                            "action" to "delete"
                                        )
                                        WorkManager.getInstance(requireContext())
                                            .enqueue(
                                                OneTimeWorkRequestBuilder<PostActionWorker>()
                                                    .setInputData(data)
                                                    .build()
                                            )
                                        feedAdapter.removeAt(position)
                                    }
                                    .setNegativeButton("Không", null)
                                    .show()
                                true
                            }
                            R.id.btnEditPost -> {
                                if (!PostUpdatingService.isUpdating)
                                findNavController().navigate(
                                    R.id.navigation_editPost,
                                    bundleOf("postId" to postId)
                                )
                                else Toast.makeText(requireContext(), "Vui lòng đợi bài trước cập nhật xong!", Toast.LENGTH_SHORT).show()
                                true
                            }
                            R.id.btnHideOrUnhidePost -> {
                                val action = if (isHidden) "unhide" else "hide"
                                val data = workDataOf(
                                    "postId" to postId,
                                    "action" to action
                                )
                                WorkManager.getInstance(requireContext())
                                    .enqueue(
                                        OneTimeWorkRequestBuilder<PostActionWorker>()
                                            .setInputData(data)
                                            .build()
                                    )
                                if (action=="hide") Toast.makeText(requireContext(), "Bài viết này sẽ không hiển thị trên bảng feed của bạn", Toast.LENGTH_SHORT).show()
                                else Toast.makeText(requireContext(), "Đã hủy ẩn bài viết", Toast.LENGTH_SHORT).show()
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi", Toast.LENGTH_SHORT).show()
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
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    private fun updateFriendshipUI() {
        if (isCurrentUserFlag) {
            binding.buttonAddFriend.visibility = View.GONE
            binding.buttonUnfriend.visibility = View.GONE
            binding.buttonChat.visibility = View.GONE
            binding.buttonCancelFriendRequest.visibility = View.GONE
            binding.buttonAcceptFriendRequest.visibility = View.GONE
            binding.buttonDeclineFriendRequest.visibility = View.GONE
        } else {
            if (isFriendFlag) {
                binding.buttonAddFriend.visibility = View.GONE
                binding.buttonUnfriend.visibility = View.VISIBLE
                binding.buttonChat.visibility = View.VISIBLE
                binding.buttonCancelFriendRequest.visibility = View.GONE
                binding.buttonAcceptFriendRequest.visibility = View.GONE
                binding.buttonDeclineFriendRequest.visibility = View.GONE
            } else {
                if (isSendingFriendRequest) {
                    binding.buttonAddFriend.visibility = View.GONE
                    binding.buttonCancelFriendRequest.visibility = View.VISIBLE
                    binding.buttonChat.visibility = View.VISIBLE
                    binding.buttonUnfriend.visibility = View.GONE
                    binding.buttonAcceptFriendRequest.visibility = View.GONE
                    binding.buttonDeclineFriendRequest.visibility = View.GONE
                }
                else if (isReceivingFriendRequest) {
                    binding.buttonAddFriend.visibility = View.GONE
                    binding.buttonAcceptFriendRequest.visibility = View.VISIBLE
                    binding.buttonDeclineFriendRequest.visibility = View.VISIBLE
                    binding.buttonChat.visibility = View.VISIBLE
                    binding.buttonUnfriend.visibility = View.GONE
                    binding.buttonCancelFriendRequest.visibility = View.GONE
                }
                else {
                    binding.buttonAddFriend.visibility = View.VISIBLE
                    binding.buttonUnfriend.visibility = View.GONE
                    binding.buttonChat.visibility = View.VISIBLE
                    binding.buttonCancelFriendRequest.visibility = View.GONE
                    binding.buttonAcceptFriendRequest.visibility = View.GONE
                    binding.buttonDeclineFriendRequest.visibility = View.GONE
                }
            }
        }
    }

    fun sendMessage(
        chatId: String,
        message: Message,
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null) {
        val db = FirebaseFirestore.getInstance()
        db.collection("chats")
            .document(chatId)
            .set(mapOf("exists" to true), SetOptions.merge())
            .addOnFailureListener { e ->
                onError?.invoke(e)
            }
        val msgRef = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()
        val msgWithId = message.copy(id = msgRef.id)
        msgRef.set(msgWithId)
        .addOnSuccessListener {
            onSuccess?.invoke()
        }
        .addOnFailureListener { e ->
            onError?.invoke(e)
        }
    }

    private fun checkBlockStatus() {
        val db = FirebaseFirestore.getInstance()
        val meId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val themId = viewModel.wallUserId
        db.collection("Users").document(meId)
        .collection("BlockedUsers").document(themId)
        .get()
        .addOnSuccessListener { doc ->
            isBlockedUser = doc.exists()
            db.collection("Users").document(themId)
            .collection("BlockedUsers").document(meId)
            .get()
            .addOnSuccessListener { doc2 ->
                isBlockedByUser = doc2.exists()
                applyBlockUI()
            }
        }
    }

    private fun applyBlockUI() {
        if (isBlockedByUser) {
            binding.run {
                informationContainer.visibility=View.GONE
                buttonAddFriend.isEnabled=false
                buttonAddFriend.text="Bạn đã bị chặn"
                recyclerViewFeed.visibility = View.GONE
                swipeRefreshLayout.visibility = View.GONE
            }
            return
        }
        if (isBlockedUser) {
            binding.buttonAddFriend.apply {
                isEnabled = false
                text = "Bạn đã chặn người này"
            }
            binding.buttonChat.visibility = View.GONE
        }
        else {
            updateFriendshipUI()
        }
    }
}