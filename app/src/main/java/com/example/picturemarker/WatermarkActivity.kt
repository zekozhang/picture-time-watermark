package com.example.picturemarker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.picturemarker.databinding.ActivityWatermarkBinding
import com.example.picturemarker.utils.WatermarkUtils
import java.io.File
import java.io.FileOutputStream

class WatermarkActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWatermarkBinding
    private lateinit var imageFiles: List<File>
    private var currentPosition = 0
    private var watermarkPosition = WatermarkUtils.WatermarkPosition.BOTTOM_RIGHT

    private val watermarkedUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatermarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageFiles = intent.getSerializableExtra("image_files") as List<File>
        showCurrentImage()

        binding.btnQuickWatermark.setOnClickListener {
            applyWatermarkToAll()
            showCurrentWatermarkedImage()
        }

        binding.btnCustomWatermark.setOnClickListener {
            showPositionSelectionDialog()
        }

        binding.btnBack.setOnClickListener {
            // 方式1：直接结束当前Activity
            finish()

            // 方式2：如果需要特定跳转
            // startActivity(Intent(this, MainActivity::class.java))
            // finish()
        }

        /*binding.btnSave.setOnClickListener {
            saveImages(true)
        }*/
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

    private fun showCurrentWatermarkedImage() {
        if (watermarkedUris.isNotEmpty()) {
            Glide.with(this)
                .load(watermarkedUris[currentPosition])
                .into(binding.ivPreview)
        }
    }

    private fun showCurrentImage() {
        val currentFile = imageFiles[currentPosition]
        val bitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
        binding.ivPreview.setImageBitmap(bitmap)
        return

        /*if (uri == null) {
            // 回退到文件路径方式
            val currentFile = imageFiles[currentPosition]
            val bitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
            binding.ivPreview.setImageBitmap(bitmap)
            return
        }

        // Preview image with uri
        try {
            val bitmap = contentResolver.loadThumbnail(
                uri,
                Size(binding.ivPreview.width, binding.ivPreview.height),
                null
            )
            binding.ivPreview.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // 备用方案：使用 Glide/Picasso
            Glide.with(this)
                .load(uri)
                .into(binding.ivPreview)
        }*/
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

        // Toast.makeText(this, "水印已添加", Toast.LENGTH_SHORT).show()
    }

    private fun saveImages(saveAsNew: Boolean) {
        imageFiles.forEach { originalFile ->
            val outputFile = if (saveAsNew) {
                // 创建新文件名，在原文件名后添加"_watermarked"
                val newFile = File(originalFile.parent, "${originalFile.nameWithoutExtension}_watermarked.jpg")
                originalFile.copyTo(newFile)
            } else {
                // 覆盖原文件
                originalFile
            }

            /*FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }*/
        }

        Toast.makeText(
            this,
            if (saveAsNew) "已保存为新文件" else "已覆盖原文件",
            Toast.LENGTH_SHORT
        ).show()
    }
}
