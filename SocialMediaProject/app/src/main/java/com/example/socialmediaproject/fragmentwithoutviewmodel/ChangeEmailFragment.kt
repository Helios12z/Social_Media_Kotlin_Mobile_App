package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentChangeEmailBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangeEmailFragment : Fragment() {
    private lateinit var binding: FragmentChangeEmailBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentChangeEmailBinding.inflate(layoutInflater, container, false)
        hideNavbar()
        auth=FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideNavbar()
        binding.currentEmail.text="Email hiện tại: " + auth.currentUser?.email
        binding.btnUpdateEmail.setOnClickListener {
            val newEmail = binding.edtNewEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (newEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null && user.email != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, password)

                val loading = LoadingDialogFragment()
                loading.show(parentFragmentManager, "loading")

                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.verifyBeforeUpdateEmail(newEmail)
                            .addOnSuccessListener {
                                loading.dismiss()
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Xác minh email")
                                    .setMessage("Đã gửi email xác minh đến:\n\n$newEmail\n\nVui lòng kiểm tra hộp thư và xác nhận để hoàn tất thay đổi.")
                                    .setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                        parentFragmentManager.popBackStack()
                                    }
                                    .setCancelable(false)
                                    .show()
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener {
                                loading.dismiss()
                                Toast.makeText(requireContext(), "Gửi xác minh thất bại: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        loading.dismiss()
                        Toast.makeText(requireContext(), "Xác thực thất bại", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showNavbarBack()
    }

    private fun hideNavbar() {
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
    }

    private fun showNavbarBack() {
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }
}