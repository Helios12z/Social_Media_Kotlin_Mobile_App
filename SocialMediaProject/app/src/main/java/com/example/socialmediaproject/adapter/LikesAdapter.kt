package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.dataclass.User

class LikesAdapter(private val users: List<User>) :
    RecyclerView.Adapter<LikesAdapter.LikeViewHolder>() {

    inner class LikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvBio: TextView = itemView.findViewById(R.id.tvUserBio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_like, parent, false)
        return LikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: LikeViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.name
        holder.tvBio.text = user.email
        if (user.avatarurl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.avatarurl)
                .placeholder(R.drawable.avataricon)
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.avataricon)
        }
    }

    override fun getItemCount(): Int = users.size
}
