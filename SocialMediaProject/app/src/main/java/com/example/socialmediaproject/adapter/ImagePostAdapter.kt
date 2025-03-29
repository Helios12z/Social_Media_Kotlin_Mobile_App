package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R

class ImagePostAdapter(
    private val imageUrls: List<String>,
    private val onImageClickListener: (Int) -> Unit
) : RecyclerView.Adapter<ImagePostAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_feed_item_layout, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        Glide.with(holder.imageViewPost)
            .load(imageUrl)
            .centerCrop()
            .into(holder.imageViewPost)

        holder.imageViewPost.setOnClickListener {
            onImageClickListener(position)
        }
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPost: ImageView = itemView.findViewById(R.id.imageViewPost)
    }
}