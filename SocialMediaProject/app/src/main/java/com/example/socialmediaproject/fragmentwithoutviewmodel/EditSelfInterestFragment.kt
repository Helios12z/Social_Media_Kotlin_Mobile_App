package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.socialmediaproject.LoadingDialogFragment
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.FragmentEditSelfInterestBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditSelfInterestFragment : Fragment() {
    private lateinit var binding: FragmentEditSelfInterestBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val interests = mutableListOf<String>()
    private val selectedInterests = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentEditSelfInterestBinding.inflate(layoutInflater, container, false)
        hideNavbar()
        db=FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideNavbar()
        loadUserInterests()
        binding.btnSave.setOnClickListener {
            saveUpdatedInterests()
        }
    }

    private fun loadUserInterests() {
        val uid = auth.currentUser?.uid ?: return

        val loading = LoadingDialogFragment()
        loading.show(parentFragmentManager, "loading")

        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            val userInterests = doc.get("interests") as? List<String> ?: emptyList()
            selectedInterests.clear()
            selectedInterests.addAll(userInterests)

            db.collection("Categories").get().addOnSuccessListener { snapshot ->
                interests.clear()
                for (item in snapshot) {
                    item.getString("name")?.let { interests.add(it) }
                }
                loading.dismiss()
                setupChips()
            }.addOnFailureListener {
                loading.dismiss()
                Toast.makeText(requireContext(), "Không tải được danh sách sở thích", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            loading.dismiss()
            Toast.makeText(requireContext(), "Lỗi tải thông tin người dùng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChips() {
        binding.chipGroup.removeAllViews()

        interests.forEach { interest ->
            val chip = Chip(requireContext()).apply {
                text = interest
                isCheckable = true
                isChecked = selectedInterests.contains(interest)
                setTextColor(android.graphics.Color.WHITE)
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_selector)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedInterests.add(interest)
                    else selectedInterests.remove(interest)
                    updateSaveButton()
                }
            }
            binding.chipGroup.addView(chip)
        }
        updateSaveButton()
    }

    private fun updateSaveButton() {
        val isEnabled = selectedInterests.size >= 2
        binding.btnSave.isEnabled = isEnabled
        val color = if (isEnabled) {
            MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.GRAY)
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        }
        binding.btnSave.setBackgroundColor(color)
    }

    private fun saveUpdatedInterests() {
        val uid = auth.currentUser?.uid ?: return
        val loading = LoadingDialogFragment()
        loading.show(parentFragmentManager, "loading")

        db.collection("Users").document(uid)
            .update("interests", selectedInterests.toList())
            .addOnSuccessListener {
                loading.dismiss()
                Toast.makeText(requireContext(), "Đã lưu thành công", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener {
                loading.dismiss()
                Toast.makeText(requireContext(), "Lỗi khi lưu", Toast.LENGTH_SHORT).show()
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