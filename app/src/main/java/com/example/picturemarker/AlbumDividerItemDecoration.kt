package com.example.picturemarker

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class AlbumDividerItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = spacing // 只设置底部间距
    }
}