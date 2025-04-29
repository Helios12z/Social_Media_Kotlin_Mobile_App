package com.example.socialmediaproject.ui.comment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        postId = arguments?.getString("post_id") ?: return
        viewModel.postId = postId
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
            expandedCommentIds = expandedCommentIds
        )
        binding.rvComments.adapter = adapter
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
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
            adapter.updateFullComments(newComments)
        }
    }

    override fun onPause() {
        super.onPause()
        recyclerViewState = binding.rvComments.layoutManager?.onSaveInstanceState()
    }
}