package com.example.socialmediaproject.ui.postwithcomment

import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.CommentAdapter
import com.example.socialmediaproject.databinding.FragmentPostWithCommentBinding
import com.example.socialmediaproject.dataclass.Comment
import com.example.socialmediaproject.dataclass.PostViewModel
import com.example.socialmediaproject.ui.comment.CommentViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class PostWithCommentFragment : Fragment() {
    private lateinit var binding: FragmentPostWithCommentBinding
    private lateinit var postId: String
    private lateinit var commentId: String
    private lateinit var viewModel: PostWithCommentViewModel
    private val commentViewModel: CommentViewModel by viewModels()
    private var replyingTo: Comment? = null
    private val db=FirebaseFirestore.getInstance()
    private val realtimedb = Firebase.database("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app/")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentPostWithCommentBinding.inflate(inflater, container, false)
        postId=arguments?.getString("post_id")?:""
        viewModel=ViewModelProvider(requireActivity())[PostWithCommentViewModel::class.java]
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
        checkIfUserHasLikedPost()
        postId = arguments?.getString("post_id") ?: return
        commentId=arguments?.getString("comment_id")?:return
        var currentPost=PostViewModel()
        db.collection("Posts").document(postId).get().addOnSuccessListener {
            result->if (result.exists()) {
                currentPost.id=result.id
                db.collection("Likes").whereEqualTo("postid", postId).whereEqualTo("userid", FirebaseAuth.getInstance().currentUser?.uid).get().addOnSuccessListener {
                    result->if (!result.isEmpty) {
                        currentPost.isLiked=true
                    }
                    else currentPost.isLiked=false
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Lỗi Internet", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }
                realtimedb.getReference("PostStats").child(postId).get().addOnSuccessListener { result ->
                    if (result.exists()) {
                        currentPost.likeCount = result.child("likecount").getValue(Int::class.java) ?: 0
                        currentPost.commentCount = result.child("commentcount").getValue(Int::class.java) ?: 0
                    }
                    else {
                        Toast.makeText(requireContext(), "Bài viết không tồn tại", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Lỗi Internet", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }
            }
            else {
                Toast.makeText(requireContext(), "Bài viết không tồn tại", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi Internet", Toast.LENGTH_SHORT).show()
            return@addOnFailureListener
        }
        viewModel.init(postId)
        viewModel.postData.observe(viewLifecycleOwner) { post ->
            binding.textViewPostContent.text = post.getString("content")
            val timestamp = post.getLong("timestamp") ?: 0
            binding.textViewTimestamp.text = viewModel.getTimeAgo(timestamp)
        }
        viewModel.postUser.observe(viewLifecycleOwner) { user ->
            binding.textViewUsername.text = user.getString("name")
            Glide.with(requireContext())
                .load(user.getString("avatarurl"))
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(binding.imageViewUserAvatar)
        }
        viewModel.statsLiveData.observe(viewLifecycleOwner) { stats ->
            val (like, comment, share) = stats
            binding.textViewLikeCount.text = like.toString()
            binding.textViewCommentCount.text = comment.toString()
            binding.textViewShareCount.text = share.toString()
        }
        viewModel.userAvatarUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(requireContext())
                .load(url)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(binding.ivUserAvatar)
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        setupCommentSection()
        binding.imageViewLike.setOnClickListener {
            viewModel.toggleLike(currentPost)
        }
        viewModel.isPostLiked.observe(viewLifecycleOwner) { isLiked ->
            val icon = if (isLiked) R.drawable.smallheartedicon else R.drawable.smallhearticon
            binding.imageViewLike.setImageResource(icon)
        }
    }

    private fun setupCommentSection() {
        commentViewModel.postId = postId
        commentViewModel.getComments { allComments ->
            val filtered = allComments.filter { it.postId == postId }
            val commentTree = buildCommentTree(filtered)
            val adapter = CommentAdapter(
                comments = commentTree,
                currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                onReplyClicked = { comment -> replyTo(comment) },
                onLikeClicked = { comment ->
                    commentViewModel.toggleLikeComment(comment.id, FirebaseAuth.getInstance().currentUser?.uid ?: "")
                },
                onReplyLikeClicked = { reply ->
                    commentViewModel.toggleLikeComment(reply.id, FirebaseAuth.getInstance().currentUser?.uid ?: "")
                },
                highlightCommentId = commentId,
                onCommentClicked = {userid->
                    val bundle=Bundle()
                    bundle.putString("wall_user_id", userid)
                    findNavController().navigate(R.id.navigation_mainpage, bundle)
                }
            )
            binding.rvComments.adapter = adapter
            binding.rvComments.layoutManager = LinearLayoutManager(context?:return@getComments)
            commentId.let { targetId ->
                val index = commentTree.indexOfFirst { it.id == targetId }
                if (index != -1) {
                    binding.rvComments.scrollToPosition(index)
                } else {
                    val parentIndex = commentTree.indexOfFirst { it.replies.any { reply -> reply.id == targetId } }
                    if (parentIndex != -1) {
                        binding.rvComments.scrollToPosition(parentIndex)
                    }
                }
            }
            binding.etCommentInput.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    s ?: return
                    val pattern = Pattern.compile("@\\w+")
                    val matcher = pattern.matcher(s)
                    val spans = s.getSpans(0, s.length, ForegroundColorSpan::class.java)
                    spans.forEach { s.removeSpan(it) }
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        s.setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.teal_700)),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            viewModel.statsLiveData.observe(viewLifecycleOwner) {stats->
                val likecount=stats.first
                val commentcount=stats.second
                val sharecount=stats.third
                binding.textViewLikeCount.text=likecount.toString()
                binding.textViewCommentCount.text=commentcount.toString()
                binding.textViewShareCount.text=sharecount.toString()
            }
        }
        binding.btnSendComment.setOnClickListener {
            val text = binding.etCommentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                commentViewModel.postComment(text, replyingTo?.id, postId)
                binding.etCommentInput.setText("")
                hideReplyUI()
            } else {
                binding.etCommentInput.error = "Không nhập gì mà đòi comment à!"
            }
        }
        binding.btnCancelReply.setOnClickListener { hideReplyUI() }
    }

    private fun replyTo(comment: Comment) {
        replyingTo = comment
        binding.tvReplyingTo.visibility = View.VISIBLE
        binding.btnCancelReply.visibility = View.VISIBLE
        binding.tvReplyingTo.text = "Đang trả lời: ${comment.username}"
        if (binding.etCommentInput.text.toString().isBlank()) {
            binding.etCommentInput.setText("@${comment.username} ")
        }
    }

    private fun hideReplyUI() {
        replyingTo = null
        binding.tvReplyingTo.visibility = View.GONE
        binding.btnCancelReply.visibility = View.GONE
    }

    private fun buildCommentTree(comments: List<Comment>): List<Comment> {
        val rootComments = comments.filter { it.parentId == null }
        rootComments.forEach { parent ->
            parent.replies = collectAllRepliesFlat(parent.id, comments).toMutableList()
        }
        return rootComments
    }

    private fun collectAllRepliesFlat(rootId: String, allComments: List<Comment>): List<Comment> {
        val directReplies = allComments.filter { it.parentId == rootId }
        val allReplies = mutableListOf<Comment>()
        allReplies.addAll(directReplies)
        directReplies.forEach { reply ->
            allReplies.addAll(collectAllRepliesFlat(reply.id, allComments))
        }
        return allReplies
    }

    private fun checkIfUserHasLikedPost() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("Likes").whereEqualTo("postid", postId)
        .whereEqualTo("userid", userId).get().addOnSuccessListener {
            results->if (!results.isEmpty) {
                binding.imageViewLike.setImageResource(R.drawable.smallheartedicon)
            }
            else binding.imageViewLike.setImageResource(R.drawable.smallhearticon)
        }
    }
}