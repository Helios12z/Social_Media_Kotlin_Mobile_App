package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.ui.comment.FriendInfo

class MentionSuggestionAdapter(
    private val onUserSelected: (String, String) -> Unit
): RecyclerView.Adapter<MentionSuggestionAdapter.ViewHolder>() {

    private val items = mutableListOf<FriendInfo>()

    fun submitList(list: List<FriendInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.tvUsername)
        private val avatar = view.findViewById<ImageView>(R.id.ivAvatar)

        fun bind(item: FriendInfo) {
            name.text = item.name
            Glide.with(itemView.context)
                .load(item.avatarUrl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(avatar)

            itemView.setOnClickListener {
                onUserSelected(item.id, item.name)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}