package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ItemFriendBinding
import com.example.socialmediaproject.dataclass.Friend
import android.graphics.Typeface
import android.view.View
import com.google.firebase.auth.FirebaseAuth

class FriendAdapter(private val onClick: (Friend) -> Unit): ListAdapter<Friend, FriendAdapter.FriendViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Friend>() {
            override fun areItemsTheSame(a: Friend, b: Friend) = a.id == b.id
            override fun areContentsTheSame(a: Friend, b: Friend) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(private val b: ItemFriendBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(f: Friend) = with(b) {
            textViewName.text = f.displayName
            textViewMutualFriends.text = "${f.mutualFriendCount} bạn chung"
            if (f.id==FirebaseAuth.getInstance().currentUser?.uid?:"") {
                friendStatus.visibility= View.GONE
                textViewMutualFriends.visibility=View.GONE
            }
            else {
                friendStatus.apply {
                    text = if (f.isFriend) "Bạn bè" else "Người lạ"
                    setTypeface(null, Typeface.BOLD)
                }
            }
            Glide.with(root)
                .load(f.avatarUrl)
                .placeholder(R.drawable.avataricon)
                .into(imageViewAvatar)
            root.setOnClickListener { onClick(f) }
        }
    }
}