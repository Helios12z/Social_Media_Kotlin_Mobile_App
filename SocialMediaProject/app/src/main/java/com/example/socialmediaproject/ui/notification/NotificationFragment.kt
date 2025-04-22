package com.example.socialmediaproject.ui.notification

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.NotificationAdapter
import com.example.socialmediaproject.databinding.FragmentNotificationBinding
import com.example.socialmediaproject.dataclass.Notification

class NotificationFragment : Fragment() {
    private lateinit var viewModel: NotificationViewModel
    private lateinit var binding: FragmentNotificationBinding
    private lateinit var adapter: NotificationAdapter

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
        viewModel.fetchNotifications()
        viewModel.notificationsLiveData.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.notificationsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.notificationsRecyclerView.visibility = View.VISIBLE
                adapter = NotificationAdapter(notifications) { notification ->
                    handleNotificationClick(notification)
                }
                binding.notificationsRecyclerView.adapter = adapter
            }
        }
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
                findNavController().navigate(R.id.navigation_postWithComment, bundle)
            }
        }
    }
}