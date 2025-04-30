package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ItemFriendShareBinding
import com.example.socialmediaproject.dataclass.Friend

class FriendShareAdapter(private val friends: MutableList<Friend>, private val onClick: (Friend) -> Unit): RecyclerView.Adapter<FriendShareAdapter.FriendViewHolder>()  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.bind(friend)
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(private val binding: ItemFriendShareBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(friend: Friend) {
            binding.apply {
                textViewName.text = friend.displayName
                fullName.text = friend.fullName
                Glide.with(root.context)
                    .load(friend.avatarUrl)
                    .placeholder(R.drawable.avataricon)
                    .into(imageViewAvatar)
                root.setOnClickListener { onClick(friend) }
            }
        }
    }
}