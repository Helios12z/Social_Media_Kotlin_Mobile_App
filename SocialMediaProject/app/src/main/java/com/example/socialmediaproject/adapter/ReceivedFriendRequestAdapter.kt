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
import com.example.socialmediaproject.dataclass.FriendRecommendation
import com.google.android.material.imageview.ShapeableImageView

class ReceivedFriendRequestAdapter(private val onRejectClick: (String, (Boolean)->Unit)->Unit,
                                   private val onAcceptClick: (String, (Boolean)->Unit)->Unit): ListAdapter<FriendRecommendation, ReceivedFriendRequestAdapter.ViewHolder>(ReceivedFriendRequestAdapter.FriendDiffCallback())
{
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewAvatar: ShapeableImageView = view.findViewById(R.id.imageViewAvatar)
        val textViewName: TextView = view.findViewById(R.id.textViewName)
        val textViewMutualFriends: TextView = view.findViewById(R.id.textViewMutualFriends)
        val buttonAcceptFriendRequest: Button = view.findViewById(R.id.buttonAcceptFriendRequest)
        val buttonRejectFriendRequest: Button = view.findViewById(R.id.buttonRejectRequest)
        val rejectNotification: TextView = view.findViewById(R.id.RejectNotification)
        val acceptNotification: TextView = view.findViewById(R.id.AcceptNotification)
        private val context = view.context

        fun bind(friend: FriendRecommendation,
                 onRejectClick: (String, (Boolean)->Unit)->Unit,
                 onAcceptClick: (String, (Boolean) -> Unit) -> Unit) {
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
            buttonRejectFriendRequest.setOnClickListener {
                onRejectClick(friend.userId) {
                    success->if (success) {
                        buttonRejectFriendRequest.visibility= View.GONE
                        buttonAcceptFriendRequest.visibility= View.GONE
                        rejectNotification.visibility= View.VISIBLE
                    }
                }
            }
            buttonAcceptFriendRequest.setOnClickListener {
                onAcceptClick(friend.userId) {
                    success->if (success) {
                        buttonRejectFriendRequest.visibility= View.GONE
                        buttonAcceptFriendRequest.visibility= View.GONE
                        acceptNotification.visibility= View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_received_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = getItem(position)
        holder.bind(friend, onRejectClick, onAcceptClick)
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