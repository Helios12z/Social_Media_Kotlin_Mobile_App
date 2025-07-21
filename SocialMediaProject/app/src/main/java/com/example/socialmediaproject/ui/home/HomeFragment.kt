package com.example.socialmediaproject.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.example.socialmediaproject.adapter.FeedAdapter
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.Friend
import com.example.socialmediaproject.dataclass.Message
import com.example.socialmediaproject.fragmentwithoutviewmodel.FriendShareDialogFragment
import com.example.socialmediaproject.service.PostActionWorker
import com.example.socialmediaproject.service.PostUpdatingService
import com.example.socialmediaproject.ui.chat.ChatViewModel
import com.example.socialmediaproject.ui.mainpage.MainPageFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class HomeFragment : Fragment(), FeedAdapter.OnPostInteractionListener {

    private lateinit var recyclerViewFeed: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedAdapter
    private val postList = mutableListOf<PostViewModel>()
    private lateinit var homeviewmodel: HomeViewModel
    private lateinit var chatbutton: ImageView
    private lateinit var searchbutton: ImageView
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var badge: BadgeDrawable
    private lateinit var avatar: ImageView
    private var isLoading = false
    private var isLastPage = false
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        homeviewmodel=ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        chatViewModel=ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        avatar=view.findViewById(R.id.image_avatar)
        chatbutton=view.findViewById(R.id.button_chat)
        chatbutton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatFragment)
        }
        searchbutton=view.findViewById(R.id.button_search)
        searchbutton.setOnClickListener {
            findNavController().navigate(R.id.navigation_search_users_and_posts)
        }
        auth=FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()
        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        return view
    }

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val chatContainer = view.findViewById<FrameLayout>(R.id.chat_container)
        val chatButton = view.findViewById<ImageView>(R.id.button_chat)
        badge = BadgeDrawable.create(requireContext()).apply {
            isVisible = false
            maxCharacterCount = 3
            badgeGravity = BadgeDrawable.TOP_END
        }
        chatButton.post {
            BadgeUtils.attachBadgeDrawable(badge, chatButton, chatContainer)
        }
        chatViewModel.totalUnreadCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.clearNumber()
                badge.isVisible = false
            }
        }
        val currentUserId=auth.currentUser?.uid
        if (currentUserId!=null) {
            db.collection("Users").document(currentUserId).get().addOnSuccessListener {
                result->if (result.exists()) {
                    val imageurl=result.getString("avatarurl")
                    Glide.with(requireContext())
                        .load(imageurl)
                        .error(R.drawable.avataricon)
                        .placeholder(R.drawable.avataricon)
                        .into(avatar)
                    avatar.setOnClickListener {
                        val bundle=Bundle()
                        bundle.putString("wall_user_id", currentUserId)
                        findNavController().navigate(R.id.action_homeFragment_to_mainPageFragment, bundle)
                    }
                }
            }
        }
    }

    private fun initViews(view: View) {
        recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(requireContext(), postList, this)
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerViewFeed.apply {
            this.layoutManager = layoutManager
            adapter = feedAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition + 1) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadMorePosts()
                        }
                    }
                }
            })
        }
    }

    private fun loadMorePosts() {
        if (isLoading || isLastPage) return
        isLoading = true
        homeviewmodel.loadMorePosts()
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
            this.isLoading = isLoading
            swipeRefreshLayout.isRefreshing = isLoading
        }
        homeviewmodel.canLoadMore.observe(viewLifecycleOwner) { canLoadMore ->
            isLastPage = !canLoadMore
        }
    }

    override fun onLikeClicked(position: Int) {
        val post=homeviewmodel.postlist.value?.get(position)?:return
        homeviewmodel.toggleLike(post)
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
        val post = homeviewmodel.postlist.value?.get(position) ?: return
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
                    senderId   = senderId,
                    receiverId = receiverId,
                    text       = shareLink,
                    timestamp  = Timestamp.now(),
                    link = true,
                    postId = post.id
                )
                sendMessage(
                    chatId    = chatId,
                    message   = message,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Chia sẻ thành công!", Toast.LENGTH_SHORT).show()
                    },
                    onError   = { e -> e.printStackTrace()
                        Toast.makeText(requireContext(), "Share thất bại", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        })
        dialog.show(parentFragmentManager, "FriendShareDialog")
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
        val post=homeviewmodel.postlist.value?.get(position)?:return
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
                            homeviewmodel.hidePostLocally(postId)
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
        val post=homeviewmodel.postlist.value?.get(postPosition)?:return
        val images=post?.imageUrls
        val imageurl=images?.get(imagePosition)
        if (imageurl!=null) {
            val bundle = bundleOf("IMAGE_URL" to imageurl)
            findNavController().navigate(R.id.viewingimagefragment, bundle)
        }
    }

    override fun onExpandClick(postPosition: Int) {
        val post=homeviewmodel.postlist.value?.get(postPosition)?:return
        val bundle=Bundle()
        bundle.putString("post_id", post.id)
        bundle.putString("comment_id", "")
        findNavController().navigate(R.id.navigation_postWithComment, bundle)
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
}