package com.example.socialmediaproject.ui.postwithcomment

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.CommentAdapter
import com.example.socialmediaproject.adapter.ImagePostAdapter
import com.example.socialmediaproject.adapter.MentionSuggestionAdapter
import com.example.socialmediaproject.databinding.FragmentPostWithCommentBinding
import com.example.socialmediaproject.dataclass.Comment
import com.example.socialmediaproject.dataclass.Friend
import com.example.socialmediaproject.dataclass.Message
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.fragmentwithoutviewmodel.FriendShareDialogFragment
import com.example.socialmediaproject.fragmentwithoutviewmodel.ViewingImageFragment
import com.example.socialmediaproject.ui.comment.CommentViewModel
import com.example.socialmediaproject.ui.comment.FriendInfo
import com.example.socialmediaproject.ui.home.HomeViewModel
import com.example.socialmediaproject.ui.mainpage.MainPageFragment
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostWithCommentFragment : Fragment() {
    private lateinit var binding: FragmentPostWithCommentBinding
    private lateinit var postId: String
    private lateinit var commentId: String
    private lateinit var viewModel: PostWithCommentViewModel
    private val commentViewModel: CommentViewModel by viewModels()
    private lateinit var homeViewModel: HomeViewModel
    private var replyingTo: Comment? = null
    private val db=FirebaseFirestore.getInstance()
    private lateinit var adapter: CommentAdapter
    private val auth=FirebaseAuth.getInstance()
    private var recyclerViewState: Parcelable? = null
    private val expandedCommentIds = mutableSetOf<String>()
    private lateinit var post: PostViewModel
    private var hasLoadedComments = false
    private lateinit var mentionAdapter: MentionSuggestionAdapter
    private val allFriends = mutableListOf<FriendInfo>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentPostWithCommentBinding.inflate(inflater, container, false)
        postId=arguments?.getString("post_id")?:""
        viewModel=ViewModelProvider(requireActivity())[PostWithCommentViewModel::class.java]
        homeViewModel=ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        db.collection("Posts").document(postId).get().addOnSuccessListener {
            result->if (result.exists()) {
                commentId=arguments?.getString("comment_id")?:""
                commentViewModel.postId=postId
                viewModel.postId=postId
                viewModel.fetchPost()
                viewModel.listenToStats()
                viewModel.fetchCurrentUserAvatar()
                viewModel.listenToLikeState()
            }
            else {
                binding.totalContainer.visibility=View.GONE
                binding.commentInputLayout.visibility=View.GONE
                binding.textViewPostDeleted.visibility=View.VISIBLE
            }
        }
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupUI()
        observeComments()
        if (!hasLoadedComments) {
            if (commentViewModel.postId != postId) {
                commentViewModel.resetComments()
                commentViewModel.postId = postId
                commentViewModel.loadInitialComments()
            }
            hasLoadedComments=true 
        }
        setupLoadMore()
        db.collection("Posts").document(postId).get().addOnSuccessListener {
            result->if (result.exists()) {
                val vm = result.toObject(PostViewModel::class.java)
                if (vm != null) {
                    vm.id=result.id
                    post = vm
                }
            }
        }
        binding.imageViewLike.setOnClickListener {
            homeViewModel.toggleLike(post)
        }
        viewModel.postData.observe(viewLifecycleOwner) {
            binding.textViewPostContent.text=it.getString("content")
            binding.textViewTimestamp.text=viewModel.getTimeAgo(it.getLong("timestamp")?:0)
            binding.imageViewLike.setOnClickListener {
                db.collection("Posts").document(postId).get().addOnSuccessListener {
                    result->if (result.exists()) {
                        val vm=result.toObject(PostViewModel::class.java)
                        vm?.id=result.id
                        if (vm!=null) {
                            homeViewModel.toggleLike(vm)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Không có internet!", Toast.LENGTH_SHORT).show()
                }
            }
            val imgUrls=it.get("imageurl") as? List<String> ?: emptyList()
            if (imgUrls.isNotEmpty()) {
                binding.recyclerViewImages.visibility=View.VISIBLE
                val recyclerView=binding.recyclerViewImages
                val imageAdapter = ImagePostAdapter(imgUrls) { imagePosition ->
                    val gotofragment= ViewingImageFragment()
                    val bundle=Bundle()
                    bundle.putString("IMAGE_URL", imgUrls[imagePosition])
                    gotofragment.arguments=bundle
                    findNavController().navigate(R.id.viewingimagefragment, bundle)
                }
                recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                recyclerView.setHasFixedSize(true)
                recyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())
                recyclerView.adapter = imageAdapter
            }
            else {
                binding.recyclerViewImages.visibility = View.GONE
            }
        }
        viewModel.postUser.observe(viewLifecycleOwner) {
            binding.textViewUsername.text=it.getString("name")
            Glide.with(requireContext())
                .load(it.getString("avatarurl"))
                .error(R.drawable.avataricon)
                .placeholder(R.drawable.avataricon)
                .into(binding.imageViewUserAvatar)
            val postUserId=it.getString("userid")
            binding.imageViewUserAvatar.setOnClickListener {
                val goToFragment= MainPageFragment()
                val bundle=Bundle()
                bundle.putString("wall_user_id", postUserId)
                goToFragment.arguments=bundle
                findNavController().navigate(R.id.navigation_mainpage, bundle)
            }
        }
        viewModel.statsLiveData.observe(viewLifecycleOwner) {
            (likecount, commentcount, sharecount)->
                binding.textViewLikeCount.text=likecount.toString()
                binding.textViewCommentCount.text=commentcount.toString()
        }
        binding.imageViewShare.setOnClickListener {
            val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
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
        viewModel.isPostLiked.observe(viewLifecycleOwner) {
            if (it) binding.imageViewLike.setImageResource(R.drawable.smallheartedicon)
            else binding.imageViewLike.setImageResource(R.drawable.smallhearticon)
        }
        parentFragmentManager.setFragmentResultListener(
            "editCommentRequest",
            viewLifecycleOwner) { _, bundle ->
            val editedId = bundle.getString("commentId") ?: return@setFragmentResultListener
            val newContent = bundle.getString("newContent") ?: return@setFragmentResultListener
            val idx = adapter.comments.indexOfFirst { it.id == editedId }
            if (idx != -1) {
                adapter.comments[idx].content = newContent
                adapter.notifyItemChanged(idx)
            } else {
                for ((parentIdx, parent) in adapter.comments.withIndex()) {
                    val replyIdx = parent.replies.indexOfFirst { it.id == editedId }
                    if (replyIdx != -1) {
                        parent.replies[replyIdx].content = newContent
                        adapter.notifyItemChanged(parentIdx)
                        break
                    }
                }
            }
        }
        binding.buttonMore.setOnClickListener {
            val popup= PopupMenu(requireContext(), binding.buttonMore)
            val menuInflater: MenuInflater = popup.menuInflater
            menuInflater.inflate(R.menu.post_management_menu, popup.menu)
            popup.setOnMenuItemClickListener {
                item->when (item.itemId) {
                    R.id.btnEditPost->{
                        true
                    }
                    R.id.btnDeletePost->{
                        true
                    }
                    R.id.btnHideOrUnhidePost->{
                        true
                    }
                    else->false
                }
            }
            popup.show()
        }
        commentViewModel.isLoadingLive.observe(viewLifecycleOwner) {
            isLoading->if (isLoading) {
                binding.commentLoadingProgress.visibility=View.VISIBLE
            }
            else {
                binding.commentLoadingProgress.visibility=View.GONE
            }
        }
        mentionAdapter = MentionSuggestionAdapter { id, name ->
            insertMention(name, id)
            binding.rvMentionSuggestions.visibility = View.GONE
        }
        binding.rvMentionSuggestions.adapter = mentionAdapter
        binding.rvMentionSuggestions.layoutManager = LinearLayoutManager(requireContext())

        loadUserFriends { friends ->
            allFriends.clear()
            allFriends.addAll(friends)
        }

        binding.etCommentInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    binding.rvMentionSuggestions.visibility = View.GONE
                    return
                }

                val cursorPos = binding.etCommentInput.selectionStart
                val textBeforeCursor = s.substring(0, cursorPos)

                val atIndex = textBeforeCursor.lastIndexOf('@')
                if (atIndex == -1 || (atIndex > 0 && textBeforeCursor[atIndex - 1] != ' ' && textBeforeCursor[atIndex - 1] != '\n')) {
                    binding.rvMentionSuggestions.visibility = View.GONE
                    return
                }

                val keyword = textBeforeCursor.substring(atIndex + 1)

                if (keyword.contains(" ") || keyword.isEmpty()) {
                    binding.rvMentionSuggestions.visibility = View.GONE
                    return
                }

                val filtered = allFriends.filter {
                    it.name.startsWith(keyword, ignoreCase = true)
                }

                if (filtered.isNotEmpty()) {
                    mentionAdapter.submitList(filtered)
                    binding.rvMentionSuggestions.visibility = View.VISIBLE
                } else {
                    binding.rvMentionSuggestions.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAdapter() {
        adapter = CommentAdapter(
            comments = mutableListOf(),
            currentUserId = auth.currentUser?.uid ?: "",
            onReplyClicked = { comment ->
                replyingTo = comment
                binding.tvReplyingTo.visibility = View.VISIBLE
                binding.btnCancelReply.visibility = View.VISIBLE
                binding.tvReplyingTo.text = "Đang trả lời: ${comment.username}"
                if (binding.etCommentInput.text.toString().isBlank()) {
                    val mention = SpannableString("@${comment.username} ")
                    val yellowSpan = ForegroundColorSpan(Color.parseColor("#FFD700"))
                    mention.setSpan(yellowSpan, 0, mention.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.etCommentInput.setText(mention)
                    binding.etCommentInput.setSelection(mention.length)
                }
            },
            onLikeClicked = { comment ->
                commentViewModel.toggleLikeComment(comment.id) {
                    val index = adapter.comments.indexOfFirst { it.id == comment.id }
                    if (index != -1) adapter.notifyItemChanged(index)
                }
            },
            onReplyLikeClicked = { reply ->
                commentViewModel.toggleLikeComment(reply.id) {
                    val index = adapter.comments.indexOfFirst { it.id == reply.id }
                    if (index != -1) adapter.notifyItemChanged(index)
                    else {
                        for (i in adapter.comments.indices) {
                            val parent = adapter.comments[i]
                            val replyIndex = parent.replies.indexOfFirst { it.id == reply.id }
                            if (replyIndex != -1) {
                                adapter.notifyItemChanged(i)
                                break
                            }
                        }
                    }
                }
            },
            highlightCommentId = null,
            onCommentClicked = { userId ->
                val bundle = Bundle()
                bundle.putString("wall_user_id", userId)
                findNavController().navigate(R.id.navigation_mainpage, bundle)
            },
            expandedCommentIds = expandedCommentIds,
            onDeleteCommentClicked = { comment ->
                confirmDeleteComment(comment.id)
            },
            onEditCommentClicked = { comment ->
                editComment(comment)
            }
        )
        binding.rvComments.adapter = adapter
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun insertMention(name: String, userId: String) {
        val fullText = binding.etCommentInput.text ?: return
        val cursorPos = binding.etCommentInput.selectionStart
        val textBeforeCursor = fullText.substring(0, cursorPos)

        val match = Regex("@(\\w{1,20})$").find(textBeforeCursor) ?: return
        val start = match.range.first
        val end = match.range.last + 1

        fullText.delete(start, end)

        val mention = SpannableString("@$name ")
        val colorSpan = ForegroundColorSpan(Color.parseColor("#FFD700"))
        mention.setSpan(colorSpan, 0, mention.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        fullText.insert(start, mention)
    }

    private fun confirmDeleteComment(commentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa bình luận này không?")
            .setPositiveButton("Có") { _, _ ->
                deleteComment(commentId)
            }
            .setNegativeButton("Không", null)
            .show()
    }

    fun deleteCommentRecursively(
        commentRef: DocumentReference,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit) {
        Toast.makeText(requireContext(), "Đang xóa...", Toast.LENGTH_SHORT).show()
        commentRef.firestore.collection("comments")
            .whereEqualTo("parentId", commentRef.id)
            .get()
            .addOnSuccessListener { snap ->
                val children = snap.documents
                if (children.isEmpty()) {
                    commentRef.delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
                else {
                    val tasks = children.map { childDoc ->
                        val tcs = TaskCompletionSource<Void>()
                        deleteCommentRecursively(
                            childDoc.reference,
                            { tcs.setResult(null) },
                            { tcs.setException(it) }
                        )
                        tcs.task
                    }
                    Tasks.whenAll(tasks)
                        .addOnSuccessListener {
                            commentRef.delete()
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { onFailure(it) }
                        }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteComment(commentId: String) {
        val commentRef = db.collection("comments").document(commentId)
        commentRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                val parentId = doc.getString("parentId")
                val uiOnSuccess = {
                    commentViewModel.removeCommentLocally(commentId)
                    Toast.makeText(requireContext(), "Xóa thành công", Toast.LENGTH_SHORT).show()
                }
                deleteCommentRecursively(
                    commentRef,
                    onSuccess = uiOnSuccess,
                    onFailure = { e ->
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Xóa lỗi", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    fun editComment(comment: Comment) {
        val bundle = Bundle()
        bundle.putString("commentId", comment.id)
        bundle.putString("avatarUrl", comment.avatarurl)
        bundle.putString("content", comment.content)
        bundle.putString("time", getTimeAgo(comment.timestamp))
        bundle.putString("username", comment.username)
        findNavController().navigate(R.id.navigation_edit_comment, bundle)
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 7 -> "$days ngày trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun setupLoadMore() {
        binding.rvComments.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastVisibleItemPosition == adapter.itemCount - 1) {
                    commentViewModel.loadMoreComments()
                }
            }
        })
    }

    private fun setupUI() {
        binding.btnSendComment.setOnClickListener {
            val text = binding.etCommentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                commentViewModel.postComment(text, replyingTo?.id, postId)
                binding.etCommentInput.setText("")
                binding.tvReplyingTo.visibility = View.GONE
                binding.btnCancelReply.visibility = View.GONE
                replyingTo = null
            }
            else {
                binding.etCommentInput.error = "Không nhập gì mà đòi comment à!"
            }
        }
        binding.btnCancelReply.setOnClickListener {
            replyingTo = null
            binding.tvReplyingTo.visibility = View.GONE
            binding.btnCancelReply.visibility = View.GONE
        }
        db.collection("Users").document(auth.currentUser?.uid?:"").get().addOnSuccessListener {
            result->if (result.exists()) {
                if (result.getString("avatarurl")!="") {
                    Glide.with(requireContext())
                        .load(result.getString("avatarurl"))
                        .placeholder(R.drawable.avataricon)
                        .error(R.drawable.avataricon)
                        .into(binding.ivUserAvatar)
                }
        }
        }
    }

    private fun observeComments() {
        commentViewModel.comments.observe(viewLifecycleOwner) { newComments ->
            if (newComments != null) {
                if (newComments.isEmpty()) {
                    binding.rvComments.isVisible = false
                    binding.noCommentsLayout.isVisible = true
                } else {
                    binding.rvComments.isVisible = true
                    binding.noCommentsLayout.isVisible = false
                    adapter.updateFullComments(newComments)
                }
            }
            else {
                binding.rvComments.isVisible = false
                binding.noCommentsLayout.isVisible = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        recyclerViewState = binding.rvComments.layoutManager?.onSaveInstanceState()
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

    private fun loadUserFriends(onLoaded: (List<FriendInfo>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("Users").document(currentUid).get()
            .addOnSuccessListener { currentDoc ->
                val friendIds = (currentDoc.get("friends") as? List<String>) ?: emptyList()

                if (friendIds.isEmpty()) {
                    onLoaded(emptyList())
                    return@addOnSuccessListener
                }

                val batches = friendIds.chunked(10)
                val friendList = mutableListOf<FriendInfo>()
                var completed = 0

                for (batch in batches) {
                    db.collection("Users")
                        .whereIn("userid", batch)
                        .get()
                        .addOnSuccessListener { snap ->
                            for (doc in snap.documents) {
                                val id = doc.getString("userid") ?: continue
                                val name = doc.getString("name") ?: continue
                                val avatar = doc.getString("avatarurl") ?: ""
                                friendList.add(FriendInfo(id, name, avatar))
                            }
                            completed++
                            if (completed == batches.size) {
                                onLoaded(friendList)
                            }
                        }
                        .addOnFailureListener {
                            completed++
                            if (completed == batches.size) {
                                onLoaded(friendList)
                            }
                        }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
                onLoaded(emptyList())
            }
    }
}