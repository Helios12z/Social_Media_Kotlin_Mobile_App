package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.databinding.ItemSearchPostBinding
import com.example.socialmediaproject.dataclass.PostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDiff : DiffUtil.ItemCallback<PostViewModel>() {
    override fun areItemsTheSame(a: PostViewModel, b: PostViewModel) = a.id == b.id
    override fun areContentsTheSame(a: PostViewModel, b: PostViewModel) = a == b
}

class SearchPostAdapter(private val onDetailClicked: (String)->Unit,
                        private val onImageClicked: (String)->Unit): ListAdapter<PostViewModel, SearchPostAdapter.PostVH>(PostDiff()) {
    inner class PostVH(val binding: ItemSearchPostBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(p: PostViewModel) {
            binding.tvAuthorName.text = p.userName
            binding.tvPostTime.text = getTimeAgo(p.timestamp)
            binding.tvPostContent.text = p.content
            Glide.with(binding.ivAuthorAvatar.context)
                .load(p.userAvatarUrl)
                .placeholder(R.drawable.avataricon)
                .error(R.drawable.avataricon)
                .into(binding.ivAuthorAvatar)
            binding.btnViewDetails.setOnClickListener {
                onDetailClicked(p.id)
            }
            if (p.imageUrls.isNotEmpty()) {
                binding.ivPostImage.visibility = View.VISIBLE
                setupImagesRecyclerView(binding.ivPostImage, p.imageUrls)
            } else {
                binding.ivPostImage.visibility = View.GONE
            }
            if (p.privacy=="Công khai") binding.postPrivacy.setImageResource(R.drawable.icon_global)
            else if (p.privacy=="Riêng tư") binding.postPrivacy.setImageResource(R.drawable.icon_private)
            else binding.postPrivacy.setImageResource(R.drawable.iconfriends)
        }
    }

    private fun setupImagesRecyclerView(recyclerView: RecyclerView, imageUrls: List<String>) {
        val imageAdapter = ImagePostAdapter(imageUrls) { imagePosition ->
            onImageClicked(imageUrls[imagePosition])
        }
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.setHasFixedSize(true)
        recyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())
        recyclerView.adapter = imageAdapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PostVH(ItemSearchPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PostVH, pos: Int) = holder.bind(getItem(pos))

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