package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentDetailInformationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DetailInformationFragment : Fragment() {
    private lateinit var binding: FragmentDetailInformationBinding
    private lateinit var userId: String
    private val db=FirebaseFirestore.getInstance()
    private val auth=FirebaseAuth.getInstance()
    private var isBlocked = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentDetailInformationBinding.inflate(inflater,container,false)
        userId=arguments?.getString("userId")?:""
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

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(0f).setDuration(200).start()
        bottomnavbar.visibility=View.VISIBLE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db.collection("Users").document(userId).get().addOnSuccessListener {
            result->if (result.exists()) {
                binding.tvFullName.setText(result.getString("fullname")?:"")
                binding.tvNickname.setText(result.getString("name")?:"")
                Glide.with(requireContext())
                    .load(result.getString("avatarurl"))
                    .placeholder(R.drawable.avataricon)
                    .error(R.drawable.avataricon)
                    .into(binding.imgUserAvatar)
                binding.tvBirthday.setText(result.getString("birthday")?:"")
                binding.tvGender.setText(result.getString("gender")?:"")
                binding.tvPhone.setText(result.getString("phonenumber")?:"")
                binding.tvAddress.setText(result.getString("address")?:"")
                val currentUserId = auth.currentUser?.uid ?:""
                val blockDocRef = db.collection("Users")
                    .document(currentUserId)
                    .collection("BlockedUsers")
                    .document(userId)
                blockDocRef.get().addOnSuccessListener { doc ->
                    isBlocked = doc.exists()
                    updateBlockButtonText()
                }
                if (currentUserId==userId) binding.btnBlock.visibility=View.GONE
                binding.btnBlock.setOnClickListener {
                    val title = if (!isBlocked) "Chặn người dùng" else "Bỏ chặn người dùng"
                    val message = if (!isBlocked) "Bạn có chắc chắn muốn chặn người này không?"
                    else "Bạn có chắc chắn muốn bỏ chặn người này không?"
                    AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Đồng ý") { _, _ ->
                        if (!isBlocked) blockUser(currentUserId, blockDocRef)
                        else unblockUser(blockDocRef)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
                }
            }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Không có Internet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBlockButtonText() {
        binding.btnBlock.text = if (isBlocked) "Bỏ chặn" else "Chặn"
    }

    private fun blockUser(
        currentUserId: String,
        blockDocRef: DocumentReference
    ) {
        val currentUserRef = db.collection("Users").document(currentUserId)
        val targetUserRef = db.collection("Users").document(userId)
        val batch = db.batch()
        batch.update(currentUserRef, "friends", FieldValue.arrayRemove(userId))
        batch.update(targetUserRef, "friends", FieldValue.arrayRemove(currentUserId))
        batch.set(blockDocRef, mapOf(
            "blockedAt" to FieldValue.serverTimestamp(),
            "blockedUserId" to userId
        ))
        batch.commit()
        .addOnSuccessListener {
            isBlocked = true
            updateBlockButtonText()
            Toast.makeText(requireContext(), "Đã chặn người dùng", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
            Toast.makeText(requireContext(), "Lỗi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unblockUser(blockDocRef: DocumentReference) {
        blockDocRef.delete().addOnSuccessListener {
            isBlocked = false
            updateBlockButtonText()
            Toast.makeText(requireContext(), "Đã bỏ chặn người dùng", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            e->e.printStackTrace()
            Toast.makeText(requireContext(), "Lỗi", Toast.LENGTH_SHORT).show()
        }
    }
}