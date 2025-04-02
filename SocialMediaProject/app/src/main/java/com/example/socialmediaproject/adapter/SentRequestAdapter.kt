package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.adapter.FriendRecommendAdapter.ViewHolder
import com.example.socialmediaproject.dataclass.FriendRecommendation
import com.google.android.material.imageview.ShapeableImageView

class SentRequestAdapter(private val onResendClick: (String, (Boolean)->Unit)->Unit): ListAdapter<FriendRecommendation, SentRequestAdapter.ViewHolder>(FriendDiffCallback())
{
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewAvatar: ShapeableImageView = view.findViewById(R.id.imageViewAvatar)
        val textViewName: TextView = view.findViewById(R.id.textViewName)
        val textViewMutualFriends: TextView = view.findViewById(R.id.textViewMutualFriends)
        val buttonResendRequest: Button = view.findViewById(R.id.buttonResendRequest)
        val resendNotification: TextView = view.findViewById(R.id.resendcomplete)
        private val context = view.context

        fun bind(friend: FriendRecommendation,
                 onResendClick: (String, (Boolean)->Unit)->Unit) {
            textViewName.text = friend.name
            textViewMutualFriends.text = if (friend.mutualFriendsCount > 0) {
                "${friend.mutualFriendsCount} bạn chung"
            } else {
                "0 bạn chung"
            }
            Glide.with(itemView.context)
                .load(friend.avatarurl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(imageViewAvatar)
            buttonResendRequest.setOnClickListener {
                onResendClick(friend.userId) {
                    success->if (success) {
                        buttonResendRequest.visibility=View.GONE
                        resendNotification.visibility=View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sent_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend, onResendClick)
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<FriendRecommendation>() {
        override fun areItemsTheSame(oldItem: FriendRecommendation, newItem: FriendRecommendation): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: FriendRecommendation, newItem: FriendRecommendation): Boolean {
            return oldItem == newItem
        }
    }
}