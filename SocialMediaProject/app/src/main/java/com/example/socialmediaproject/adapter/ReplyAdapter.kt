package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.Comment
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReplyAdapter(
    private val replies: List<Comment>,
    private val currentUserId: String,
    private val onReplyClicked: (Comment) -> Unit,
    private val onLikeClicked: (Comment) -> Unit,
    private val onCommentClicked: (String) -> Unit,
    private val level: Int = 0,
    private val maxLevel: Int = 3,
    private val highlightReplyId: String?
) : RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder>() {

    inner class ReplyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar = itemView.findViewById<ShapeableImageView>(R.id.ivReplyUserAvatar)
        val username = view.findViewById<TextView>(R.id.tvReplyUsername)
        val time = view.findViewById<TextView>(R.id.tvReplyTime)
        val content = view.findViewById<TextView>(R.id.tvReplyContent)
        val likeCount = view.findViewById<TextView>(R.id.tvReplyLikeCount)
        val btnLike = view.findViewById<TextView>(R.id.btnReplyLike)
        val btnReply = view.findViewById<TextView>(R.id.btnReplyToReply)
        val nestedRepliesContainer = view.findViewById<LinearLayout>(R.id.nestedRepliesContainer)
        val cardReply=view.findViewById<CardView>(R.id.cardReplyComment)
        val editReply=view.findViewById<TextView>(R.id.editReply)
        val deleteReply=view.findViewById<TextView>(R.id.deleteReply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment_reply, parent, false)
        return ReplyViewHolder(view)
    }

    private val db=FirebaseFirestore.getInstance()

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replies[position]
        db.collection("Users").document(currentUserId).get().addOnSuccessListener { result ->
            if (result.exists()) {
                if (result.getString("role") == "admin") {
                    holder.deleteReply.visibility = View.VISIBLE
                    holder.editReply.visibility = View.GONE
                }
            }
        }
        if (reply.userId==currentUserId) {
            holder.deleteReply.visibility = View.VISIBLE
            holder.editReply.visibility = View.VISIBLE
        }
        if (reply.id == highlightReplyId) {
            holder.cardReply.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.highlight_color)
            )
            holder.cardReply.startAnimation(
                AnimationUtils.loadAnimation(holder.itemView.context, R.anim.pulse)
            )
        } else {
            holder.cardReply.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.normal_comment_background)
            )
        }
        val isLiked = reply.likes.contains(currentUserId)
        val iconRes = if (isLiked) R.drawable.smallheartedicon else R.drawable.smallhearticon
        holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        holder.username.text = reply.username
        holder.content.text = reply.content
        holder.likeCount.text = reply.likes.size.toString()
        holder.time.text = getTimeAgo(reply.timestamp)
        holder.btnLike.setOnClickListener { onLikeClicked(reply) }
        holder.btnReply.setOnClickListener { onReplyClicked(reply) }
        if (reply.avatarurl.isNotEmpty()) {
            Glide.with(holder.avatar.context)
                .load(reply.avatarurl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(holder.avatar)
        }
        holder.avatar.setOnClickListener {
            onCommentClicked(reply.userId)
        }
        holder.nestedRepliesContainer.removeAllViews()
        if (reply.replies.isNotEmpty() && level < maxLevel) {
            val nestedRecyclerView = RecyclerView(holder.itemView.context)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            nestedRecyclerView.layoutParams = layoutParams
            val nestedAdapter = ReplyAdapter(
                replies = reply.replies,
                currentUserId = currentUserId,
                onReplyClicked = onReplyClicked,
                onLikeClicked = onLikeClicked,
                onCommentClicked = onCommentClicked,
                level = level + 1,
                maxLevel = maxLevel,
                highlightReplyId=highlightReplyId
            )
            nestedRecyclerView.adapter = nestedAdapter
            nestedRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            nestedRecyclerView.isNestedScrollingEnabled = false
            holder.nestedRepliesContainer.addView(nestedRecyclerView)
        }
    }

    override fun getItemCount(): Int = replies.size

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