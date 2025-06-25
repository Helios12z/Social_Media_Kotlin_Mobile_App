package com.example.socialmediaproject.ui.comment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.CommentAdapter
import com.example.socialmediaproject.databinding.FragmentCommentBinding
import com.example.socialmediaproject.dataclass.Comment
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentFragment : Fragment() {
    private lateinit var viewModel: CommentViewModel
    private lateinit var binding: FragmentCommentBinding
    private var replyingTo: Comment? = null
    private lateinit var adapter: CommentAdapter
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var postId: String
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var recyclerViewState: Parcelable? = null
    private val expandedCommentIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentCommentBinding.inflate(inflater, container, false)
        viewModel=ViewModelProvider(requireActivity())[CommentViewModel::class.java]
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        postId = arguments?.getString("post_id") ?: ""
        viewModel.postId = postId
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
        viewModel.resetComments()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isLoadingLive.observe(viewLifecycleOwner) { isLoading ->
            val fm = childFragmentManager
            val existing = fm.findFragmentByTag("loading") as? LoadingDialogFragment
            if (!viewModel.comments.value.isNullOrEmpty()) {
                existing?.dismissAllowingStateLoss()
                return@observe
            }
            if (isLoading) {
                if (existing == null) {
                    LoadingDialogFragment().show(fm, "loading")
                }
            } else {
                existing?.dismissAllowingStateLoss()
            }
        }
        setupAdapter()
        setupUI()
        observeComments()
        viewModel.loadInitialComments()
        setupLoadMore()
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
                    binding.etCommentInput.setText("@${comment.username} ")
                }
            },
            onLikeClicked = { comment ->
                viewModel.toggleLikeComment(comment.id) {
                    val index = adapter.comments.indexOfFirst { it.id == comment.id }
                    if (index != -1) adapter.notifyItemChanged(index)
                }
            },
            onReplyLikeClicked = { reply ->
                viewModel.toggleLikeComment(reply.id) {
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
                viewModel.removeCommentLocally(commentId)
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
                    viewModel.loadMoreComments()
                }
            }
        })
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

    private fun observeComments() {
        viewModel.comments.observe(viewLifecycleOwner) { newComments ->
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
}