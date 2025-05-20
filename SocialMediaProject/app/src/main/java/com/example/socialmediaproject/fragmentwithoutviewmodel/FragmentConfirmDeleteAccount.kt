package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.socialmediaproject.activity.LoginActivity
import com.example.socialmediaproject.databinding.FragmentConfirmDeleteAccountBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class FragmentConfirmDeleteAccount : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentConfirmDeleteAccountBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentConfirmDeleteAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.deleteButton.setOnClickListener {
            val passwordInput=binding.passwordInput
            val passwordLayout=binding.passwordLayout
            val password = passwordInput.text.toString().trim()
            if (password.isEmpty()) {
                passwordLayout.error = "Mật khẩu không được để trống"
                return@setOnClickListener
            }
            val user=FirebaseAuth.getInstance().currentUser
            if (user!=null && user.email!=null)
            {
                val credential=EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).addOnCompleteListener {
                    task->if (task.isSuccessful) showConfirmDialog()
                    else passwordLayout.error="Mật khẩu không đúng"
                }
            }
            passwordLayout.error = null
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản? Hành động này không thể hoàn tác.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                deleteAccount()
            }
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseAuth.getInstance().signOut()
                    requireActivity().finishAffinity()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show()
                }
            }
    }
}