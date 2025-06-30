package com.example.picturemarker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.bumptech.glide.Glide
import com.example.picturemarker.databinding.ActivityWatermarkBinding
import com.example.picturemarker.utils.WatermarkUtils
import java.io.File

class WatermarkActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWatermarkBinding
    private lateinit var imageFiles: List<File>
    private var currentPosition = 0
    private var watermarkPosition = WatermarkUtils.WatermarkPosition.BOTTOM_RIGHT
    private lateinit var gestureDetector: GestureDetectorCompat
    private val watermarkedUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatermarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageFiles = intent.getSerializableExtra("image_files") as List<File>
        setupGestureDetector()
        setupNavigationButtons()
        showCurrentImage()

        binding.btnQuickWatermark.setOnClickListener {
            applyWatermarkToAll()
        }

        binding.btnCustomWatermark.setOnClickListener {
            showPositionSelectionDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                if (Math.abs(diffX) > 100) { // 滑动阈值
                    if (diffX > 0) {
                        showPreviousImage()
                    } else {
                        showNextImage()
                    }
                    return true
                }
                return false
            }
        })

        binding.ivPreview.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupNavigationButtons() {
        binding.btnPrevious.setOnClickListener { showPreviousImage() }
        binding.btnNext.setOnClickListener { showNextImage() }
        updateNavButtonVisibility()
    }

    private fun showPreviousImage() {
        if (currentPosition > 0) {
            currentPosition--
            showCurrentImage()
            updateNavButtonVisibility()
        }
    }

    private fun showNextImage() {
        if (currentPosition < imageFiles.size - 1) {
            currentPosition++
            showCurrentImage()
            updateNavButtonVisibility()
        }
    }

    private fun updateNavButtonVisibility() {
        binding.btnPrevious.visibility = if (currentPosition > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if (currentPosition < imageFiles.size - 1) View.VISIBLE else View.INVISIBLE
    }

    private fun showPositionSelectionDialog() {
        val positions = WatermarkUtils.WatermarkPosition.values()
        val positionNames = positions.map {
            when(it) {
                WatermarkUtils.WatermarkPosition.TOP_LEFT -> "左上角"
                WatermarkUtils.WatermarkPosition.TOP_RIGHT -> "右上角"
                WatermarkUtils.WatermarkPosition.BOTTOM_LEFT -> "左下角"
                WatermarkUtils.WatermarkPosition.BOTTOM_RIGHT -> "右下角"
                WatermarkUtils.WatermarkPosition.CENTER -> "居中"
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择水印位置")
            .setItems(positionNames) { dialog, which ->
                watermarkPosition = positions[which]
                applyWatermarkToAll()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCurrentImage() {
        if (watermarkedUris.isNotEmpty() && currentPosition < watermarkedUris.size) {
            // 显示已加水印的图片
            Glide.with(this)
                .load(watermarkedUris[currentPosition])
                .into(binding.ivPreview)
        } else {
            // 显示原始图片
            val currentFile = imageFiles[currentPosition]
            Glide.with(this)
                .load(currentFile)
                .into(binding.ivPreview)
        }
    }

    private fun applyWatermarkToAll() {
        watermarkedUris.clear()
        imageFiles.forEach { file ->
            val uri = WatermarkUtils.addWatermark(this, file, file, watermarkPosition)
            uri?.let { watermarkedUris.add(it) }
        }

        if (watermarkedUris.isNotEmpty()) {
            Toast.makeText(this, "已添加水印到 ${watermarkedUris.size} 张图片", Toast.LENGTH_SHORT).show()
            showCurrentImage()
        } else {
            Toast.makeText(this, "水印添加失败", Toast.LENGTH_SHORT).show()
        }
    }
}