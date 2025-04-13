package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.Comment
import com.google.android.material.imageview.ShapeableImageView

class ReplyAdapter(
    private val replies: List<Comment>,
    private val currentUserId: String,
    private val onReplyClicked: (Comment) -> Unit,
    private val onLikeClicked: (Comment) -> Unit,
    private val depth: Int
) : RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder>() {

    inner class ReplyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar=itemView.findViewById<ShapeableImageView>(R.id.ivReplyUserAvatar)
        val username = view.findViewById<TextView>(R.id.tvReplyUsername)
        val time = view.findViewById<TextView>(R.id.tvReplyTime)
        val content = view.findViewById<TextView>(R.id.tvReplyContent)
        val likeCount = view.findViewById<TextView>(R.id.tvReplyLikeCount)
        val btnLike = view.findViewById<TextView>(R.id.btnReplyLike)
        val btnReply = view.findViewById<TextView>(R.id.btnReplyToReply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment_reply, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replies[position]
        val isLiked = reply.likes.contains(currentUserId)
        val iconRes = if (isLiked) R.drawable.smallheartedicon else R.drawable.smallhearticon
        holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        holder.username.text = reply.username
        holder.content.text = reply.content
        holder.likeCount.text = reply.likes.size.toString()
        holder.time.text = getTimeAgo(reply.timestamp)
        holder.btnLike.setOnClickListener { onLikeClicked(reply) }
        holder.btnReply.visibility = View.VISIBLE
        holder.btnReply.setOnClickListener { onReplyClicked(reply) }
        if (reply.avatarurl.isNotEmpty()) {
            Glide.with(holder.avatar.context)
                .load(reply.avatarurl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(holder.avatar)
        }
    }

    override fun getItemCount(): Int = replies.size

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        return if (hours > 0) "$hours giờ trước" else "$minutes phút trước"
    }
}