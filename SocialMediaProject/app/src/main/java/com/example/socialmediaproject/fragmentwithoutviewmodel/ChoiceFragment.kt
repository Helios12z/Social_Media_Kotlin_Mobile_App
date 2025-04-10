package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.activity.LoginActivity
import com.example.socialmediaproject.databinding.FragmentChoiceBinding
import com.example.socialmediaproject.ui.mainpage.MainPageFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChoiceFragment : Fragment() {
    private lateinit var binding: FragmentChoiceBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var useremail: String
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentChoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db=FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
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
            useremail=result.getString("email").toString()
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
                val builder= AlertDialog.Builder(requireContext())
                builder.setTitle("Xác nhận")
                builder.setMessage("Bạn có muốn đăng xuất?")
                builder.setPositiveButton("Có") { dialog, which ->
                    userLogOut()
                }
                builder.setNegativeButton("Không") { dialog, which -> }
                builder.show()
            }
            binding.cardAccountHome.setOnClickListener {
                val gotofragment=MainPageFragment()
                val bundle=Bundle()
                bundle.putString("wall_user_id", userid)
                gotofragment.arguments=bundle
                findNavController().navigate(R.id.action_choiceFragment_to_mainPageFragment, bundle)
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
        }
    }

    private fun userLogOut() {
        db=FirebaseFirestore.getInstance()
        WorkManager.getInstance(requireContext()).cancelAllWorkByTag("FriendRequestWorker")
        auth.signOut()
        sharedPreferences = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToAccountDetailFragment() {
        findNavController().navigate(R.id.action_choiceFragment_to_accountDetailFragment)
    }
}