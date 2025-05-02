package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentEditCommentBinding
import com.example.socialmediaproject.ui.comment.CommentViewModel
import com.google.firebase.firestore.FirebaseFirestore

class EditCommentFragment : Fragment() {
    private lateinit var binding: FragmentEditCommentBinding
    private lateinit var commentId: String
    private lateinit var avatarUrl: String
    private lateinit var time: String
    private lateinit var content: String
    private lateinit var oldContent: String
    private lateinit var username: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentEditCommentBinding.inflate(inflater,container,false)
        commentId = arguments?.getString("commentId") ?: ""
        avatarUrl=arguments?.getString("avatarUrl") ?: ""
        time=arguments?.getString("time") ?: ""
        content=arguments?.getString("content") ?: ""
        username=arguments?.getString("username") ?: ""
        oldContent=content
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etCommentContent.setText(content)
        binding.tvUsername.setText(username)
        Glide.with(requireContext())
            .load(avatarUrl)
            .error(R.drawable.avataricon)
            .placeholder(R.drawable.avataricon)
            .into(binding.ivUserAvatar)
        binding.tvCommentTimestamp.setText(time)
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnSaveComment.setOnClickListener {
            val newContent = binding.etCommentContent.text.toString()
            if (newContent==oldContent) {
                Toast.makeText(requireContext(), "Nội dung không thay đổi", Toast.LENGTH_SHORT).show()
            }
            else {
                FirebaseFirestore.getInstance().collection("comments").document(commentId).update("content", newContent).addOnSuccessListener {
                    parentFragmentManager.setFragmentResult(
                        "editCommentRequest",
                        bundleOf(
                            "commentId" to commentId,
                            "newContent" to newContent
                        )
                    )
                    Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }
}