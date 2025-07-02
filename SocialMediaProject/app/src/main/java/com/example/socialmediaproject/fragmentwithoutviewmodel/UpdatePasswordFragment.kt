package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentUpdatePasswordBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.socialmediaproject.activity.MainActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UpdatePasswordFragment : Fragment() {
    private lateinit var binding: FragmentUpdatePasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentUpdatePasswordBinding.inflate(layoutInflater, container, false)
        auth=FirebaseAuth.getInstance()
        db=FirebaseFirestore.getInstance()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user=auth.currentUser
        val email=user?.email
        binding.etNewPassword.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //do nothing
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().length>=8)
                {
                    binding.tvLength.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tickicon, 0, 0, 0)
                }
                else
                {
                    binding.tvLength.setCompoundDrawablesWithIntrinsicBounds(R.drawable.warning_icon, 0, 0, 0)
                }
                if (s.toString().matches(".*[A-Z].*".toRegex()))
                {
                    binding.tvUppercase.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tickicon, 0, 0, 0)
                }
                else
                {
                    binding.tvUppercase.setCompoundDrawablesWithIntrinsicBounds(R.drawable.warning_icon, 0, 0, 0)
                }
                if (s.toString().matches(".*\\d.*".toRegex()))
                {
                    binding.tvNumber.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tickicon, 0, 0, 0)
                }
                else
                {
                    binding.tvNumber.setCompoundDrawablesWithIntrinsicBounds(R.drawable.warning_icon, 0, 0, 0)
                }
                if (s.toString().matches(".*[!@#\$%^&*()_+=\\[\\]{};':\",.<>/?`~|-].*".toRegex()))
                {
                    binding.tvSpecial.setCompoundDrawablesWithIntrinsicBounds(R.drawable.tickicon, 0, 0, 0)
                }
                else
                {
                    binding.tvSpecial.setCompoundDrawablesWithIntrinsicBounds(R.drawable.warning_icon, 0, 0, 0)
                }
            }
        })
        binding.etConfirmPassword.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //do nothing
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString()!=binding.etNewPassword.text.toString())
                {
                    binding.etConfirmPassword.error="Mật khẩu nhập lại không khớp"
                }
            }
        })
        binding.btnUpdatePassword.setOnClickListener {
            binding.btnUpdatePassword.isEnabled=false
            binding.progressBar.visibility=View.VISIBLE
            if (binding.etCurrentPassword.text.toString().isNullOrEmpty() || binding.etNewPassword.text.toString().isNullOrEmpty() || binding.etConfirmPassword.text.toString().isNullOrEmpty())
            {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                binding.btnUpdatePassword.isEnabled=true
                binding.progressBar.visibility=View.GONE
                return@setOnClickListener
            }
            val credential=EmailAuthProvider.getCredential(email!!, binding.etCurrentPassword.text.toString())
            user.reauthenticate(credential).addOnCompleteListener {
                task->if (task.isSuccessful) {
                    if (binding.etNewPassword.text.toString()==binding.etConfirmPassword.text.toString()) {
                        user.updatePassword(binding.etNewPassword.text.toString()).addOnCompleteListener {
                            result->if (result.isSuccessful) {
                                Toast.makeText(requireContext(), "Cập nhật mật khẩu mới thành công", Toast.LENGTH_SHORT).show()
                                binding.etCurrentPassword.setText("")
                                binding.etNewPassword.setText("")
                                binding.etConfirmPassword.setText("")
                                binding.btnUpdatePassword.isEnabled=true
                                binding.progressBar.visibility=View.GONE
                            }
                            else
                            {
                                Toast.makeText(requireContext(), "Cập nhật mật khẩu mới không thành công", Toast.LENGTH_SHORT).show()
                                binding.btnUpdatePassword.isEnabled=true
                                binding.progressBar.visibility=View.GONE
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(requireContext(), "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show()
                        binding.btnUpdatePassword.isEnabled=true
                        binding.progressBar.visibility=View.GONE
                    }
                }
                else {
                    Toast.makeText(requireContext(), "Mật khẩu cũ nhập không đúng", Toast.LENGTH_SHORT).show()
                    binding.btnUpdatePassword.isEnabled=true
                    binding.progressBar.visibility=View.GONE
                    return@addOnCompleteListener
                }
            }
        }
    }
}