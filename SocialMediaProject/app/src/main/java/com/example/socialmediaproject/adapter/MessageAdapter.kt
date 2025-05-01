package com.example.socialmediaproject.adapter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.Constant
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.Message
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(private val currentUserId: String,
                     private val senderAvatarUrl: String?,
                     private val onMessageLongClick: (Message) -> Unit,
                     private val onLinkClick: (postId: String, commentId: String?, messageContent: String?) -> Unit): ListAdapter<Message, MessageAdapter.MessageViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem == newItem
        }
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutSent = view.findViewById<View>(R.id.layoutSentMessage)
        val layoutReceived = view.findViewById<View>(R.id.layoutReceivedMessage)
        val tvSentMessage = view.findViewById<TextView>(R.id.tvSentMessage)
        val tvSentTime = view.findViewById<TextView>(R.id.tvSentTime)
        val ivMessageStatus = view.findViewById<ImageView>(R.id.ivMessageStatus)
        val tvReceivedMessage = view.findViewById<TextView>(R.id.tvReceivedMessage)
        val tvReceivedTime = view.findViewById<TextView>(R.id.tvReceivedTime)
        val ivSenderAvatar = view.findViewById<ImageView>(R.id.ivSenderAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        val isSentByCurrentUser = message.senderId == currentUserId
        val isFromAI = message.senderId == "Ordinary_VectorAI"
        val timeFormat = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        val timeText = timeFormat.format(message.timestamp.toDate())
        if (isSentByCurrentUser) {
            holder.layoutSent.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
            holder.tvSentMessage.text = message.text
            holder.tvSentTime.text = timeText
            if (message.receiverId!=Constant.ChatConstants.VECTOR_AI_ID) {
                val tickIcon = if (message.read)
                    R.drawable.tickicon_double
                else
                    R.drawable.tickicon
                holder.ivMessageStatus.setImageResource(tickIcon)
            }
            else holder.ivMessageStatus.visibility=View.GONE
        }
        else if (isFromAI) {
            holder.layoutSent.visibility = View.GONE
            holder.layoutReceived.visibility = View.VISIBLE
            holder.tvReceivedMessage.text = message.text
            holder.tvReceivedTime.text = timeText
            Glide.with(holder.itemView.context)
                .load(R.drawable.vectorai)
                .placeholder(R.drawable.vectorai)
                .into(holder.ivSenderAvatar)
        }
        else {
            holder.layoutSent.visibility = View.GONE
            holder.layoutReceived.visibility = View.VISIBLE
            holder.tvReceivedMessage.text = message.text
            holder.tvReceivedTime.text = timeText
            Glide.with(holder.itemView.context).load(senderAvatarUrl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(holder.ivSenderAvatar)
        }
        holder.itemView.setOnLongClickListener {
            if (message.senderId==currentUserId) onMessageLongClick.invoke(message)
            true
        }
        if (message.removed) {
            if (isSentByCurrentUser) {
                holder.tvSentMessage.text = "Tin nhắn đã được thu hồi"
                holder.tvSentMessage.setTypeface(null, Typeface.ITALIC)
                holder.tvSentMessage.setTextColor(Color.GRAY)
            } else {
                holder.tvReceivedMessage.text = "Tin nhắn đã được thu hồi"
                holder.tvReceivedMessage.setTypeface(null, Typeface.ITALIC)
                holder.tvReceivedMessage.setTextColor(Color.GRAY)
            }
            holder.itemView.isClickable = false
            holder.itemView.isLongClickable = false
        } else {
            if (isSentByCurrentUser) {
                holder.tvSentMessage.setTypeface(null, Typeface.NORMAL)
                val typedValue = TypedValue()
                val theme = holder.itemView.context.theme
                theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                val color = ContextCompat.getColor(holder.itemView.context, typedValue.resourceId)
                holder.tvSentMessage.setTextColor(color)
            } else {
                holder.tvReceivedMessage.setTypeface(null, Typeface.NORMAL)
                val typedValue = TypedValue()
                val theme = holder.itemView.context.theme
                theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                val color = ContextCompat.getColor(holder.itemView.context, typedValue.resourceId)
                holder.tvReceivedMessage.setTextColor(color)
            }
            holder.itemView.isClickable = true
            holder.itemView.isLongClickable = true
        }
        if (message.link) {
            if (isSentByCurrentUser) {
                holder.tvSentMessage.setTextColor(Color.BLUE)
                holder.tvSentMessage.paintFlags = holder.tvSentMessage.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                holder.tvSentMessage.setOnClickListener {
                    onLinkClick(message.postId, "", "")
                }
            }
            else {
                holder.tvReceivedMessage.setTextColor(Color.BLUE)
                holder.tvReceivedMessage.paintFlags = holder.tvReceivedMessage.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                holder.tvReceivedMessage.setOnClickListener {
                    onLinkClick(message.postId, "", message.text)
                }
            }
        }
        else {
            if (isSentByCurrentUser) {
                holder.tvSentMessage.setTypeface(null, Typeface.NORMAL)
                val typedValue = TypedValue()
                val theme = holder.itemView.context.theme
                theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                val color = ContextCompat.getColor(holder.itemView.context, typedValue.resourceId)
                holder.tvSentMessage.setTextColor(color)
            } else {
                holder.tvReceivedMessage.setTypeface(null, Typeface.NORMAL)
                val typedValue = TypedValue()
                val theme = holder.itemView.context.theme
                theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                val color = ContextCompat.getColor(holder.itemView.context, typedValue.resourceId)
                holder.tvReceivedMessage.setTextColor(color)
            }
        }
    }
}