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