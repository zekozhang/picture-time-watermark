// 新建文件 ImageAdapter.kt
package com.example.picturemarker

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class ImageAdapter(
    private val imageFiles: List<File>,
    private val onItemSelected: (File, Boolean) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val checkBox: CheckBox = itemView.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Log.d("Adapter", "Binding position $position")
        val file = imageFiles[position]

        // 使用Glide加载图片
        Glide.with(holder.itemView.context)
            .load(file)
            .into(holder.imageView)

        holder.checkBox.isChecked = selectedItems.contains(position)

        holder.itemView.setOnClickListener {
            Log.d("Adapter", "1. Click detected on position $position")
            val isSelected = !selectedItems.contains(position)
            if (isSelected) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
            }
            holder.checkBox.isChecked = isSelected
            onItemSelected(file, isSelected)
        }

        /*holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("Adapter", "2. Click detected on position $position")
            if (isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
            }
            onItemSelected(file, isChecked)
        }*/
    }

    override fun getItemCount() = imageFiles.size
}