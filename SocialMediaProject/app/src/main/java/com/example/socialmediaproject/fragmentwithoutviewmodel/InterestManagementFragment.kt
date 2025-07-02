package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.InterestAdapter
import com.example.socialmediaproject.databinding.FragmentInterestManagementBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class InterestManagementFragment : Fragment() {
    private lateinit var binding: FragmentInterestManagementBinding
    private lateinit var db: FirebaseFirestore
    private val interestList = mutableListOf<Pair<String, String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentInterestManagementBinding.inflate(layoutInflater, container, false)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.animate().translationY(bottomnavbar.height.toFloat()).setDuration(200).start()
        bottomnavbar.visibility=View.GONE
        db=FirebaseFirestore.getInstance()
        binding.recyclerViewInterest.layoutManager = LinearLayoutManager(requireContext())
        binding.btnAddInterest.setOnClickListener { showAddDialog() }
        loadInterests()
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

    private fun loadInterests() {
        db.collection("Categories").get().addOnSuccessListener { snapshot ->
            interestList.clear()
            for (doc in snapshot) {
                val name = doc.getString("name") ?: continue
                interestList.add(doc.id to name)
            }
            binding.recyclerViewInterest.adapter = InterestAdapter(interestList,
                onEdit = { id, name -> showEditDialog(id, name) },
                onDelete = { id -> deleteInterest(id) }
            )
        }
    }

    private fun showAddDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Thêm sở thích")
            .setView(input)
            .setPositiveButton("Thêm") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    db.collection("Categories").add(mapOf("name" to name)).addOnSuccessListener {
                        loadInterests()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDialog(id: String, oldName: String) {
        val input = EditText(requireContext())
        input.setText(oldName)
        AlertDialog.Builder(requireContext())
            .setTitle("Sửa sở thích")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    db.collection("Categories").document(id).update("name", newName).addOnSuccessListener {
                        loadInterests()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteInterest(id: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa sở thích")
            .setMessage("Bạn có chắc muốn xóa?")
            .setPositiveButton("Xóa") { _, _ ->
                db.collection("Categories").document(id).delete().addOnSuccessListener {
                    loadInterests()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}