package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingFragment : Fragment() {
    private lateinit var binding: FragmentSettingBinding
    private lateinit var userId: String
    private val db=FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentSettingBinding.inflate(layoutInflater, container, false)
        userId=arguments?.getString("userId") ?: ""
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode==Configuration.UI_MODE_NIGHT_YES) {
            binding.darkLightIcon.setImageResource(R.drawable.night_icon)
            binding.textViewThemeStatus.setText("Tối")
        }
        else {
            binding.darkLightIcon.setImageResource(R.drawable.light_icon)
            binding.textViewThemeStatus.setText("Sáng")
        }
        db.collection("Users").document(userId).get().addOnSuccessListener {
            result->if (result.exists())
            {
                if (result.getString("role")=="Admin")
                {
                    binding.cardEditInterests.visibility=View.VISIBLE
                    binding.cardEditGenders.visibility=View.VISIBLE
                    binding.cardUserManagement.visibility=View.VISIBLE
                }
            }
            else
            {
                Toast.makeText(requireContext(), "Không tìm thấy người dùng", Toast.LENGTH_SHORT)
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Không có kết nối ", Toast.LENGTH_SHORT)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardOSChoice.setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
        binding.cardChangePassword.setOnClickListener {
            findNavController().navigate(R.id.navigation_update_password)
        }
        binding.cardForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.navigation_forgot_password)
        }
    }
}