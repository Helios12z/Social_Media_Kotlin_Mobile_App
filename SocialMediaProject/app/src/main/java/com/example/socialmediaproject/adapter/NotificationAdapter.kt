package com.example.socialmediaproject.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.databinding.ItemNotificationBinding
import com.example.socialmediaproject.dataclass.Notification

class NotificationAdapter(private val onClick: (Notification) -> Unit, private val onDeleteClick: (Notification) -> Unit): ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {
    inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.notificationText.text = notification.message
            binding.notificationTime.text = DateUtils.getRelativeTimeSpanString(notification.timestamp.toDate().time)
            binding.notificationCard.strokeWidth = if (!notification.read) 4 else 0
            binding.notificationAction.setOnClickListener {
                onClick(notification)
            }
            binding.notificationDelete.setOnClickListener {
                onDeleteClick(notification)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
    override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem == newItem
    }
}