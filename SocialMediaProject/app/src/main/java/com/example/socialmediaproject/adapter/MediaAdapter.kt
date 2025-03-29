package com.example.socialmediaproject.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.R

class MediaAdapter(private val imageList: MutableList<Uri>, private val onRemoveClick: (Int) -> Unit): RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.img_media)
        val removeButton: ImageButton = itemView.findViewById(R.id.btn_remove_media)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.setImageURI(imageList[position])
        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount(): Int = imageList.size

    fun addImage(uri: Uri) {
        imageList.add(uri)
        notifyItemInserted(imageList.size - 1)
    }
}