package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.activity.LoginActivity
import com.example.socialmediaproject.databinding.FragmentChoiceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChoiceFragment : Fragment() {
    private lateinit var binding: FragmentChoiceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentChoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db: FirebaseFirestore=FirebaseFirestore.getInstance()
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val userid=auth.currentUser?.uid ?: ""
        binding.choiceprogressbar.visibility=View.VISIBLE
        binding.tvUserName.visibility=View.INVISIBLE
        binding.tvUserEmail.visibility=View.INVISIBLE
        db.collection("Users").document(userid).get().addOnSuccessListener { result->
            binding.choiceprogressbar.visibility=View.GONE
            binding.tvUserName.visibility=View.VISIBLE
            binding.tvUserEmail.visibility=View.VISIBLE
            binding.tvUserName.text=result.getString("name")
            binding.tvUserEmail.text=result.getString("email")
            if (result.getString("avatarurl")!=null || result.getString("avatarurl")!="") {
                Glide.with(requireContext())
                    .load(result.getString("avatarurl"))
                    .placeholder(R.drawable.avataricon)
                    .error(R.drawable.avataricon)
                    .into(binding.ivUserAvatar)
            }
            binding.cardEditAccount.setOnClickListener {
                navigateToAccountDetailFragment()
            }
            binding.cardLogout.setOnClickListener {
                userLogOut()
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        }
    }

    private fun userLogOut() {
        WorkManager.getInstance(requireContext()).cancelAllWorkByTag("FriendRequestWorker")
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToAccountDetailFragment() {
        findNavController().navigate(R.id.action_choiceFragment_to_accountDetailFragment)
    }
}