package com.example.socialmediaproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmediaproject.R

class InterestAdapter(private val interests: List<Pair<String, String>>,
                      private val onEdit: (String, String) -> Unit,
                      private val onDelete: (String) -> Unit): RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    inner class InterestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtInterest: TextView = itemView.findViewById(R.id.txtInterest)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interest, parent, false)
        return InterestViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val (id, name) = interests[position]
        holder.txtInterest.text = name
        holder.btnEdit.setOnClickListener { onEdit(id, name) }
        holder.btnDelete.setOnClickListener { onDelete(id) }
    }

    override fun getItemCount(): Int = interests.size
}