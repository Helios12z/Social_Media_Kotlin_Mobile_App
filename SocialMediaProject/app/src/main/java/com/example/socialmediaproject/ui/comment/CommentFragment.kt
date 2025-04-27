package com.example.socialmediaproject.ui.comment

import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.CommentAdapter
import com.example.socialmediaproject.databinding.FragmentCommentBinding
import com.example.socialmediaproject.dataclass.Comment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class CommentFragment : Fragment() {
    private lateinit var viewModel: CommentViewModel
    private lateinit var binding: FragmentCommentBinding
    private var replyingTo: Comment? = null
    private lateinit var adapter: CommentAdapter
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var postId: String
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var recyclerViewState: Parcelable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentCommentBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[CommentViewModel::class.java]
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
        recyclerViewState?.let {
            binding.rvComments.layoutManager?.onRestoreInstanceState(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postId=arguments?.getString("post_id") ?: return
        viewModel.postId=postId
        setupAdapter()
        setupUI()
        observeComments(postId)
        val sortedComments = viewModel.getSortedCommentsForDisplay(postId)
        val commentTree = buildCommentTree(sortedComments)
        adapter.updateComments(commentTree)
        binding.etCommentInput.addTextChangedListener(object : TextWatcher {
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
                if (binding.etCommentInput.text.toString().trim().isEmpty()) {
                    binding.etCommentInput.setText("@${comment.username} ")
                }
            },
            onLikeClicked = { comment ->
                viewModel.toggleLikeComment(comment.id, auth.currentUser?.uid ?: "") {
                    val position = adapter.comments.indexOfFirst { it.id == comment.id }
                    if (position != -1) {
                        adapter.notifyItemChanged(position)
                    } else {
                        for (i in adapter.comments.indices) {
                            val parentComment = adapter.comments[i]
                            val replyPosition = parentComment.replies.indexOfFirst { it.id == comment.id }
                            if (replyPosition != -1) {
                                adapter.notifyItemChanged(i)
                                break
                            }
                        }
                    }
                }
            },
            onReplyLikeClicked = { reply ->
                viewModel.toggleLikeComment(reply.id, auth.currentUser?.uid ?: "") {
                    val position = adapter.comments.indexOfFirst { it.id == reply.id }
                    if (position != -1) {
                        adapter.notifyItemChanged(position)
                    } else {
                        for (i in adapter.comments.indices) {
                            val comment = adapter.comments[i]
                            val replyPosition = comment.replies.indexOfFirst { it.id == reply.id }
                            if (replyPosition != -1) {
                                adapter.notifyItemChanged(i)
                                break
                            }
                        }
                    }
                }
            },
            onCommentClicked = { userId->
                val bundle=Bundle()
                bundle.putString("wall_user_id", userId)
                findNavController().navigate(R.id.navigation_mainpage, bundle)
            },
            expandedCommentIds = viewModel.expandedCommentIds
        )
        binding.rvComments.apply {
            adapter = this@CommentFragment.adapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun setupUI() {
        binding.btnSendComment.setOnClickListener {
            val text = binding.etCommentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.postComment(text, replyingTo?.id, postId)
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

    private fun observeComments(postId: String) {
        viewModel.getComments { allComments ->
            val filteredComments = allComments.filter { it.postId == postId }
            val commentTree = buildCommentTree(filteredComments)
            adapter.updateComments(commentTree)
        }
        viewModel.onCommentLikedSuccessfully = { likedCommentId ->
            val position = adapter.comments.indexOfFirst { it.id == likedCommentId }
            if (position != -1) {
                adapter.notifyItemChanged(position)
            } else {
                for (i in adapter.comments.indices) {
                    val comment = adapter.comments[i]
                    val replyPosition = comment.replies.indexOfFirst { it.id == likedCommentId }
                    if (replyPosition != -1) {
                        adapter.notifyItemChanged(i)
                        break
                    }
                }
            }
        }
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

    override fun onPause() {
        super.onPause()
        recyclerViewState = binding.rvComments.layoutManager?.onSaveInstanceState()
    }
}