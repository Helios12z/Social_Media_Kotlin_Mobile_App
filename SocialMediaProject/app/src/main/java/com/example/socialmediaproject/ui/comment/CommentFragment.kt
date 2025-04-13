package com.example.socialmediaproject.ui.comment

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.CommentAdapter
import com.example.socialmediaproject.databinding.FragmentCommentBinding
import com.example.socialmediaproject.dataclass.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentFragment : Fragment() {

    private lateinit var viewModel: CommentViewModel
    private lateinit var binding: FragmentCommentBinding
    private var replyingTo: Comment? = null
    private lateinit var adapter: CommentAdapter
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var postId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentCommentBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[CommentViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postId=arguments?.getString("post_id") ?: return
        setupUI()
        observeComments(postId)
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
                binding.etCommentInput.error = "Làm sao không nhập gì mà đòi comment được bạn!"
            }
        }

        binding.btnCancelReply.setOnClickListener {
            replyingTo = null
            binding.tvReplyingTo.visibility = View.GONE
            binding.btnCancelReply.visibility = View.GONE
        }
    }

    private fun observeComments(postId: String) {
        viewModel.getComments { allComments ->
            val filteredComments = allComments.filter { it.postId == postId }
            val commentTree = buildCommentTree(filteredComments)

            adapter = CommentAdapter(
                comments = commentTree,
                currentUserId = auth.currentUser?.uid ?: "",
                onReplyClicked = { comment ->
                    replyingTo = comment
                    binding.tvReplyingTo.visibility = View.VISIBLE
                    binding.btnCancelReply.visibility = View.VISIBLE
                    binding.tvReplyingTo.text = "Đang trả lời: ${comment.userId}"
                },
                onLikeClicked = { comment ->
                    viewModel.toggleLikeComment(comment.id, auth.currentUser?.uid ?: "")
                },
                onReplyLikeClicked = { reply ->
                    viewModel.toggleLikeComment(reply.id, auth.currentUser?.uid ?: "")
                }
            )

            binding.rvComments.apply {
                adapter = this@CommentFragment.adapter
                layoutManager = LinearLayoutManager(context)
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
}