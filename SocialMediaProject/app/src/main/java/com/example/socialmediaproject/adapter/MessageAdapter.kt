package com.example.socialmediaproject.adapter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
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
                     private val onLinkClick: (postId: String, commentId: String?, messageContent: String?) -> Unit,
                     private val onPictureClick: (imageUrl: String) -> Unit): ListAdapter<Message, MessageAdapter.MessageViewHolder>(DIFF_CALLBACK) {
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
        val ivSentImage = view.findViewById<ImageView>(R.id.ivSentImage)
        val ivReceivedImage = view.findViewById<ImageView>(R.id.ivReceivedImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        val isSent = message.senderId == currentUserId
        val isFromAI = message.senderId == Constant.ChatConstants.VECTOR_AI_ID
        val timeText = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            .format(message.timestamp.toDate())
        listOf(holder.tvSentMessage, holder.tvReceivedMessage).forEach { tv ->
            tv.paintFlags = tv.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            tv.setOnClickListener(null)
            val tvp = TypedValue().apply {
                holder.itemView.context.theme.resolveAttribute(
                    android.R.attr.textColorPrimary, this, true
                )
            }
            val baseColor = if (tvp.resourceId != 0)
                ContextCompat.getColor(holder.itemView.context, tvp.resourceId)
            else tvp.data
            tv.setTextColor(baseColor)

            tv.setTypeface(null, Typeface.NORMAL)
            tv.isClickable = false
            tv.isLongClickable = false
        }
        if (isSent) {
            holder.layoutSent.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE
            holder.tvSentMessage.text = message.text
            holder.tvSentTime.text = timeText
            holder.ivMessageStatus.apply {
                visibility = if (message.receiverId != Constant.ChatConstants.VECTOR_AI_ID) View.VISIBLE else View.GONE
                setImageResource(if (message.read) R.drawable.tickicon_double else R.drawable.tickicon)
            }
        } else {
            holder.layoutSent.visibility = View.GONE
            holder.layoutReceived.visibility = View.VISIBLE
            holder.tvReceivedMessage.text = message.text
            holder.tvReceivedTime.text = timeText
            val avatarSrc = if (isFromAI) R.drawable.vectorai else senderAvatarUrl
            Glide.with(holder.itemView.context)
                .load(avatarSrc).placeholder(R.drawable.avataricon).into(holder.ivSenderAvatar)
        }
        holder.itemView.apply {
            isLongClickable = true
            setOnLongClickListener {
                if (!message.removed) {
                    onMessageLongClick(message)
                    true
                } else false
            }
        }
        if (message.removed) {
            val tv = if (isSent) holder.tvSentMessage else holder.tvReceivedMessage
            tv.apply {
                text = "Tin nhắn đã được thu hồi"
                setTypeface(null, Typeface.ITALIC)
                setTextColor(Color.GRAY)
            }
            return
        }
        if (message.link) {
            val tv = if (isSent) holder.tvSentMessage else holder.tvReceivedMessage
            tv.apply {
                isClickable = true
                isLongClickable = true
                setTextColor(Color.YELLOW)
                paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                setOnClickListener {
                    if (!message.postId.isNullOrEmpty()) {
                        onLinkClick(message.postId, "", "")
                    } else {
                        onLinkClick("", "", message.text)
                    }
                }
                setOnLongClickListener {
                    onMessageLongClick(message)
                    true
                }
            }
            return
        }
        if (message.picture) {
            val tv = if (isSent) holder.tvSentMessage else holder.tvReceivedMessage
            tv.apply {
                isClickable=true
                isLongClickable=true
                setOnLongClickListener {
                    onMessageLongClick(message)
                    true
                }
                setOnClickListener {
                    onPictureClick(message.imageUrl)
                }
                tv.visibility=View.GONE
            }
            val iv = if (isSent) holder.ivSentImage else holder.ivReceivedImage
            iv.visibility=View.VISIBLE
            Log.d("IMAGE URL: ", message.imageUrl)
            Glide.with(holder.itemView.context).load(message.imageUrl)
                .placeholder(R.drawable.imageicon)
                .error(R.drawable.imageicon)
                .into(iv)
        }
        else {
            holder.tvSentMessage.visibility=View.VISIBLE
            holder.tvReceivedMessage.visibility=View.VISIBLE
            holder.ivSentImage.visibility=View.GONE
            holder.ivReceivedImage.visibility=View.GONE
        }
    }
}