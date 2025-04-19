package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.ChatUser

class ChatUserAdapter(private val users: List<ChatUser>, private val onItemClick: (ChatUser) -> Unit): RecyclerView.Adapter<ChatUserAdapter.ChatViewHolder>() {
    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.ivUserAvatar)
        val name: TextView = view.findViewById(R.id.tvUsername)
        val message: TextView = view.findViewById(R.id.tvLastMessage)
        val time: TextView = view.findViewById(R.id.tvTimestamp)
        val unread: TextView = view.findViewById(R.id.tvUnreadCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.username
        holder.message.text = user.lastMessage ?: "Chưa có tin nhắn"
        holder.time.text = ""
        holder.unread.visibility = if (user.unreadCount > 0) View.VISIBLE else View.GONE
        holder.unread.text = user.unreadCount.toString()
        Glide.with(holder.itemView.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.avataricon)
            .into(holder.avatar)
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}