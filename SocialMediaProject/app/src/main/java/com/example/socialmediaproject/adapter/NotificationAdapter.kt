package com.example.socialmediaproject.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.databinding.ItemNotificationBinding
import com.example.socialmediaproject.dataclass.Notification

class NotificationAdapter(private val notifications: List<Notification>, private val onClick: (Notification) -> Unit): RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.notificationText.text = notification.message
            binding.notificationTime.text = DateUtils.getRelativeTimeSpanString(notification.timestamp.toDate().time)
            binding.notificationCard.strokeWidth = if (!notification.read) 4 else 0
            binding.notificationAction.setOnClickListener {
                onClick(notification)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}