package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.Comment
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val comments: List<Comment>,
    private val currentUserId: String,
    private val onReplyClicked: (Comment) -> Unit,
    private val onLikeClicked: (Comment) -> Unit,
    private val onReplyLikeClicked: (Comment) -> Unit,
    private val highlightCommentId: String? = null
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar = itemView.findViewById<ShapeableImageView>(R.id.ivUserAvatar)
        val username = itemView.findViewById<TextView>(R.id.tvUsername)
        val time = itemView.findViewById<TextView>(R.id.tvCommentTime)
        val content = itemView.findViewById<TextView>(R.id.tvCommentContent)
        val likeCount = itemView.findViewById<TextView>(R.id.tvLikeCount)
        val btnLike = itemView.findViewById<TextView>(R.id.btnLike)
        val btnReply = itemView.findViewById<TextView>(R.id.btnReply)
        val rvReplies = itemView.findViewById<RecyclerView>(R.id.rvReplies)
        val cardComment = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cardComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        if (comment.id == highlightCommentId) {
            holder.cardComment.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.highlight_color))
            holder.cardComment.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.pulse))
        } else {
            holder.cardComment.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.normal_comment_background))
        }
        holder.username.text = comment.username
        holder.content.text = comment.content
        holder.likeCount.text = comment.likes.size.toString()
        holder.time.text = getTimeAgo(comment.timestamp)
        if (comment.avatarurl.isNotEmpty()) {
            Glide.with(holder.avatar.context)
                .load(comment.avatarurl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(holder.avatar)
        }
        val isLiked = comment.likes.contains(currentUserId)
        val iconRes = if (isLiked) R.drawable.smallheartedicon else R.drawable.smallhearticon
        holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        holder.btnLike.setOnClickListener { onLikeClicked(comment) }
        holder.btnReply.setOnClickListener { onReplyClicked(comment) }
        val replyAdapter = ReplyAdapter(
            replies = comment.replies,
            currentUserId = currentUserId,
            onReplyClicked = onReplyClicked,
            onLikeClicked = onReplyLikeClicked,
            depth = 2
        )
        holder.rvReplies.adapter = replyAdapter
        holder.rvReplies.layoutManager = LinearLayoutManager(holder.itemView.context)
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 7 -> "$days ngày trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}