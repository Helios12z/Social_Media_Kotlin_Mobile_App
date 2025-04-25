package com.example.socialmediaproject.ui.notification

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.NotificationAdapter
import com.example.socialmediaproject.databinding.FragmentNotificationBinding
import com.example.socialmediaproject.dataclass.Notification
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class NotificationFragment : Fragment() {
    private lateinit var viewModel: NotificationViewModel
    private lateinit var binding: FragmentNotificationBinding
    private lateinit var adapter: NotificationAdapter
    private var allNotifications: List<Notification> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel= ViewModelProvider(requireActivity())[NotificationViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = NotificationAdapter (
            onClick = {notification ->  handleNotificationClick(notification)},
            onDeleteClick = {notification -> handleDeleteNotificationClick(notification)}
        )
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
        viewModel.fetchNotifications()
        if (viewModel.notificationsLiveData.value?.isEmpty()?:true) {
            binding.emptyState.visibility = View.VISIBLE
            binding.notificationsRecyclerView.visibility = View.GONE
        }
        else {
            binding.emptyState.visibility = View.GONE
            binding.notificationsRecyclerView.visibility = View.VISIBLE
        }
        observeNotifications()
    }

    private fun handleNotificationClick(notification: Notification) {
        viewModel.markAsRead(notification.id)
        when (notification.type) {
            "friend_request" -> {
                val bundle=Bundle()
                bundle.putString("wall_user_id", notification.senderId)
                findNavController().navigate(R.id.navigation_mainpage, bundle)
            }
            "mention" -> {
                val bundle=Bundle()
                bundle.putString("post_id", notification.relatedPostId)
                bundle.putString("comment_id", notification.relatedCommentId)
                findNavController().navigate(R.id.navigation_postWithComment, bundle)
            }
        }
    }

    private fun handleDeleteNotificationClick(notification: Notification) {
        FirebaseFirestore.getInstance().collection("notifications").document(notification.id).delete()
    }

    private fun filterNotifications(checkedId: Int) {
        val filteredList = when (checkedId) {
            R.id.chip_all -> allNotifications
            R.id.chip_mentions -> allNotifications.filter { it.type == "mention" }
            R.id.chip_friend_requests -> allNotifications.filter { it.type == "friend_request" }
            else -> allNotifications
        }
        if (filteredList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.notificationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.notificationsRecyclerView.visibility = View.VISIBLE
        }
        adapter.submitList(filteredList)
    }

    private fun observeNotifications() {
        viewModel.notificationsLiveData.observe(viewLifecycleOwner) { notifications ->
            allNotifications = notifications
            if (notifications.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.notificationsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.notificationsRecyclerView.visibility = View.VISIBLE
            }
            filterNotifications(binding.filterChips.checkedChipId)
        }
        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedId = checkedIds.firstOrNull()
            if (selectedId != null) {
                filterNotifications(selectedId)
            }
        }
    }
}