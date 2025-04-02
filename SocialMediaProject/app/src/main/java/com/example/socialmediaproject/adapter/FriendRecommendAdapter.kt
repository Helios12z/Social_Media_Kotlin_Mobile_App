package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.dataclass.FriendRecommendation
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.RequestStatus
import com.google.android.material.imageview.ShapeableImageView

class FriendRecommendAdapter(private val onAddFriendClick: (FriendRecommendation) -> Unit,
                             private val onResendClick: (String, (Boolean)->Unit)->Unit) :
    ListAdapter<FriendRecommendation, FriendRecommendAdapter.ViewHolder>(FriendDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewAvatar: ShapeableImageView = view.findViewById(R.id.imageViewAvatar)
        val textViewName: TextView = view.findViewById(R.id.textViewName)
        val textViewMutualFriends: TextView = view.findViewById(R.id.textViewMutualFriends)
        val buttonAddFriend: Button = view.findViewById(R.id.buttonAddFriend)
        val buttonResend: Button = view.findViewById(R.id.buttonresend)
        private val context = view.context

        fun bind(friend: FriendRecommendation,
                 onAddFriendClick: (FriendRecommendation) -> Unit,
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

            when (friend.requestStatus) {
                RequestStatus.SENDING -> {
                    buttonAddFriend.text = "Đang gửi..."
                    buttonAddFriend.isEnabled = false
                    buttonAddFriend.setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
                }
                RequestStatus.SENT -> {
                    buttonAddFriend.text = "Đã gửi"
                    buttonAddFriend.isEnabled = false
                    buttonResend.visibility=View.VISIBLE
                    buttonResend.setOnClickListener {
                        onResendClick(friend.userId) {
                            success->if (success) {
                            buttonResend.visibility = View.GONE
                            buttonAddFriend.isEnabled = true
                            buttonAddFriend.text = "Kết bạn"
                            buttonAddFriend.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_200))
                            }
                        }
                    }
                    buttonAddFriend.setBackgroundColor(ContextCompat.getColor(context, R.color.gray))
                }
                RequestStatus.ERROR -> {
                    buttonAddFriend.text = "Lỗi! Thử lại"
                    buttonAddFriend.isEnabled = true
                    buttonAddFriend.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_200))
                    buttonAddFriend.setOnClickListener {
                        onAddFriendClick(friend)
                    }
                }
                else -> {
                    buttonAddFriend.text = "Kết bạn"
                    buttonAddFriend.isEnabled = true
                    buttonAddFriend.setBackgroundColor(ContextCompat.getColor(context, R.color.purple_200))
                    buttonAddFriend.setOnClickListener {
                        onAddFriendClick(friend)
                        buttonAddFriend.text = "Đang gửi..."
                        buttonAddFriend.isEnabled = false
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend, onAddFriendClick, onResendClick)
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