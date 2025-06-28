package com.example.picturemarker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// AlbumAdapter.kt
// AlbumAdapter.kt
class AlbumAdapter(
    private val albums: List<Album>,
    private val onAlbumSelected: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    private var selectedPosition = 0

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivAlbumCover)
        val textView: TextView = itemView.findViewById(R.id.tvAlbumName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        // 获取当前安全的位置
        val safePosition = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
            ?: return // 如果位置无效则直接返回

        val album = albums[safePosition]
        holder.textView.text = album.name

        Glide.with(holder.itemView.context)
            .load(album.coverPath)
            .into(holder.imageView)

        holder.itemView.isSelected = safePosition == selectedPosition
        holder.itemView.setOnClickListener {
            selectedPosition = safePosition
            notifyDataSetChanged()
            onAlbumSelected(album)
        }
    }

    override fun getItemCount() = albums.size
}