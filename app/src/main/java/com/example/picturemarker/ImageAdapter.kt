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
        val overlay: View = itemView.findViewById(R.id.selectedOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val safePosition = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
            ?: return

        val file = imageFiles[safePosition]
        val isSelected = selectedItems.contains(safePosition)

        Glide.with(holder.itemView.context)
            .load(file)
            .thumbnail(0.1f)
            .into(holder.imageView)

        holder.checkBox.isChecked = isSelected
        holder.overlay.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val newSelected = !isSelected
            if (newSelected) {
                selectedItems.add(safePosition)
            } else {
                selectedItems.remove(safePosition)
            }
            notifyItemChanged(safePosition)
            onItemSelected(file, newSelected)
        }
    }

    override fun getItemCount() = imageFiles.size
}