package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.databinding.ItemSearchUserBinding
import com.example.socialmediaproject.dataclass.User

class UserDiff : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(a: User, b: User) = a.userid == b.userid
    override fun areContentsTheSame(a: User, b: User) = a == b
}

class SearchUserAdapter(private val onDetailClicked: (String)->Unit): ListAdapter<User, SearchUserAdapter.UserVH>(UserDiff()) {
    inner class UserVH(val binding: ItemSearchUserBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(u: User) {
            binding.tvUserName.text = u.name
            binding.tvUserHandle.text = "${u.fullName}"
            binding.tvUserBio.text = u.bio
            Glide.with(binding.ivUserAvatar)
                .load(u.avatarurl)
                .into(binding.ivUserAvatar)
            binding.btnDetail.setOnClickListener {
                onDetailClicked(u.userid)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserVH(ItemSearchUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: UserVH, pos: Int) = holder.bind(getItem(pos))
}