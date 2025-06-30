package com.example.picturemarker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.picturemarker.databinding.ActivityImageSelectionBinding
import java.io.File

class ImageSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageSelectionBinding
    private val REQUEST_PERMISSION = 1001
    private val selectedImages = mutableSetOf<String>() // 使用文件路径作为唯一标识

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        checkPermissions()
    }

    private fun setupRecyclerViews() {
        // 左侧相册列表 (8dp间距)
        binding.rvAlbums.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ImageSelectionActivity)
            addItemDecoration(AlbumDividerItemDecoration(resources.getDimensionPixelSize(R.dimen.album_spacing)))
        }

        // 右侧图片列表 (4dp间距)
        binding.rvImages.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@ImageSelectionActivity, 3)
            addItemDecoration(ImageGridItemDecoration(resources.getDimensionPixelSize(R.dimen.grid_spacing)))
        }
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                loadAlbums()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                showPermissionExplanationDialog(permission)
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_PERMISSION
                )
            }
        }
    }

    private fun showPermissionExplanationDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("此功能需要访问您的照片，请授予存储权限")
            .setPositiveButton("确定") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_PERMISSION
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadAlbums() {
        val albums = getAlbumsFromStorage()

        binding.rvAlbums.adapter = AlbumAdapter(albums) { album ->
            // 切换相册时更新图片列表，保持已选状态
            binding.rvImages.adapter = createImageAdapter(album.imageFiles)
        }

        // 默认加载第一个相册
        if (albums.isNotEmpty()) {
            binding.rvImages.adapter = createImageAdapter(albums[0].imageFiles)
        }

        setupConfirmButton()
    }

    private fun createImageAdapter(files: List<File>): ImageAdapter {
        return ImageAdapter(
            files,
            onItemSelected = { file, isSelected ->
                if (isSelected) {
                    selectedImages.add(file.absolutePath)
                } else {
                    selectedImages.remove(file.absolutePath)
                }
            },
            isItemSelected = { file ->
                selectedImages.contains(file.absolutePath)
            }
        )
    }

    private fun setupConfirmButton() {
        binding.btnConfirm.setOnClickListener {
            val selectedFiles = getAlbumsFromStorage()
                .flatMap { it.imageFiles }
                .filter { selectedImages.contains(it.absolutePath) }
                .takeIf { it.isNotEmpty() }
                ?.let { files ->
                    Intent(this, WatermarkActivity::class.java).apply {
                        putExtra("image_files", ArrayList(files))
                        startActivity(this)
                    }
                } ?: Toast.makeText(this, "请至少选择一张图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAlbumsFromStorage(): List<Album> {
        val albumsMap = mutableMapOf<String, MutableList<File>>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        contentResolver.query(
            getMediaStoreUri(),
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            Log.d("AlbumQuery", "Found ${cursor.count} media items")

            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketNameColumn) ?: continue
                val path = cursor.getString(dataColumn) ?: continue

                File(path).takeIf { it.exists() }?.let { file ->
                    albumsMap.getOrPut(bucketName) { mutableListOf() }.add(file)
                }
            }
        }

        return albumsMap.map { (name, files) ->
            Album(
                name = name,
                coverPath = files.maxByOrNull { it.lastModified() }?.absolutePath ?: "",
                imageFiles = files.sortedByDescending { it.lastModified() },
                count = files.size
            )
        }.sortedBy { it.name }
    }

    private fun getMediaStoreUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAlbums()
        } else {
            Toast.makeText(this, "需要权限才能访问照片", Toast.LENGTH_SHORT).show()
        }
    }
}