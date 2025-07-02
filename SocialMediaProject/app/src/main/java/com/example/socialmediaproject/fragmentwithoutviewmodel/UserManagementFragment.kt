package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.UserManagementAdapter
import com.example.socialmediaproject.databinding.FragmentUserManagementBinding
import com.example.socialmediaproject.activity.MainActivity
import com.example.socialmediaproject.dataclass.Friend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class UserManagementFragment : Fragment() {
    private lateinit var binding: FragmentUserManagementBinding
    private lateinit var db: FirebaseFirestore
    private var users= mutableListOf<Friend>()
    private lateinit var friendAdapter: UserManagementAdapter
    private var selectedFriend: Friend? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentUserManagementBinding.inflate(layoutInflater, container, false)
        db = FirebaseFirestore.getInstance()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).hideNavigationWithBlur()
        loadUserStats()
        loadUserSignupsByMonth()
        setupRoleManagement()
        setUpLiveSearch()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }

    private fun loadUserStats() {
        db.collection("Users").get().addOnSuccessListener { snap ->
            val total = snap.size()
            var active = 0
            val now = System.currentTimeMillis()

            for (doc in snap) {
                val ts = doc.getTimestamp("lastActive")?.toDate()?.time ?: continue
                if (now - ts < 60_000) active++
            }

            binding.totalUsersText.text = "Tổng người dùng: $total"
            binding.activeUsersText.text = "Đang hoạt động: $active"
        }
    }

    private fun loadUserSignupsByMonth() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Users").get().addOnSuccessListener { snap ->
            val calendar = Calendar.getInstance()
            val monthCount = IntArray(12)

            for (doc in snap) {
                val ts = doc.getTimestamp("createdAt")?.toDate() ?: continue
                calendar.time = ts
                val month = calendar.get(Calendar.MONTH)
                monthCount[month]++
            }

            val entries = monthCount.mapIndexed { i, value ->
                BarEntry(i.toFloat(), value.toFloat())
            }

            val barDataSet = BarDataSet(entries, "Người dùng mới theo tháng")
            val data = BarData(barDataSet)
            binding.userChart.data = data
            binding.userChart.invalidate()

            binding.userChart.xAxis.textColor=R.color.text_color
            binding.userChart.axisLeft.textColor=R.color.text_color
            binding.userChart.axisRight.textColor=R.color.text_color
            binding.userChart.legend.textColor=R.color.text_color
            binding.userChart.description.textColor=R.color.text_color
        }
    }

    private fun setupRoleManagement() {
        binding.updateRoleBtn.setOnClickListener {
            val username = binding.searchUsername.text.toString().trim()
            val selectedRole = binding.roleSpinner.selectedItem.toString()

            db.collection("Users")
                .whereEqualTo("name", username)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) {
                        val doc = snap.documents[0]
                        db.collection("Users").document(doc.id)
                            .update("role", selectedRole).addOnSuccessListener {
                                Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Cập nhật không thành công", Toast.LENGTH_SHORT).show()
                            }
                    }
                    else {
                        Toast.makeText(requireContext(), "Cập nhật không thành công", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Cập nhật không thành công", Toast.LENGTH_SHORT).show()
                }
        }

        binding.deleteUserBtn.setOnClickListener {
            val username = binding.searchUsername.text.toString().trim()
            db.collection("Users")
                .whereEqualTo("name", username)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) {
                        val doc = snap.documents[0]
                        val userRef=db.collection("Users").document(doc.id)
                        val currentStatus = doc.getBoolean("isBanned") ?: false
                        userRef.update("isBanned", !currentStatus)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Cấm tài khoản thành công",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Cấm tài khoản không thành công",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    else {
                        Toast.makeText(
                            requireContext(),
                            "Cấm tài khoản không thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Cấm tài khoản không thành công",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun setUpLiveSearch() {
        friendAdapter = UserManagementAdapter(users) { friend ->
            selectedFriend = friend
            binding.searchUsername.setText(friend.displayName)
            binding.userSearchRecycler.visibility = View.GONE
            db.collection("Users")
                .document(friend.id)
                .get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: "user"
                    val roleIndex = when (role.lowercase()) {
                        "admin" -> 1
                        "user" -> 0
                        else -> 0
                    }
                    binding.roleSpinner.setSelection(roleIndex)
                    val isBanned=doc.getBoolean("isBanned") ?: false
                    if (isBanned) binding.deleteUserBtn.text="Khôi phục tài khoản"
                    else binding.deleteUserBtn.text="Cấm tài khoản"

                    binding.roleSpinner.visibility = View.VISIBLE
                    binding.updateRoleBtn.visibility = View.VISIBLE
                    binding.deleteUserBtn.visibility = View.VISIBLE
                }
        }

        binding.userSearchRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.userSearchRecycler.adapter = friendAdapter

        binding.searchUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim()
                if (keyword.isEmpty()) {
                    users.clear()
                    friendAdapter.notifyDataSetChanged()
                    binding.userSearchRecycler.visibility = View.GONE
                    return
                }

                db.collection("Users")
                    .orderBy("name")
                    .startAt(keyword)
                    .endAt(keyword + "\uf8ff")
                    .limit(10)
                    .get()
                    .addOnSuccessListener { snap ->
                        users.clear()
                        for (doc in snap.documents) {
                            val friend = Friend(
                                id = doc.id,
                                displayName = doc.getString("name") ?: "",
                                fullName = doc.getString("fullname") ?: "",
                                avatarUrl = doc.getString("avatarurl") ?: "",
                                mutualFriendCount = 1,
                                isFriend = false
                            )
                            users.add(friend)
                        }
                        friendAdapter.notifyDataSetChanged()
                        binding.userSearchRecycler.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
                    }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}