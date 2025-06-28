package com.example.picturemarker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.picturemarker.databinding.ActivityImageSelectionBinding
import java.io.File

class ImageSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageSelectionBinding
    private val REQUEST_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                //loadImages()
                loadAlbums()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                // 解释为什么需要权限
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

    // 在ImageSelectionActivity.kt中添加：
    private val selectedImages = mutableListOf<File>()

    private fun loadImages() {
        // 获取所有图片文件
        val imageFiles = getImageFilesFromStorage()
        // 设置RecyclerView适配器
        binding.rvImages.adapter = ImageAdapter(imageFiles) { file, isSelected ->
            if (isSelected) {
                selectedImages.add(file)
            } else {
                selectedImages.remove(file)
            }
        }

        binding.btnConfirm.setOnClickListener {
            if (selectedImages.isNotEmpty()) {
                val intent = Intent(this, WatermarkActivity::class.java).apply {
                    putExtra("image_files", ArrayList(selectedImages))
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "请至少选择一张图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAlbums() {
        val albums = getAlbumsFromStorage()

        binding.rvAlbums.adapter = AlbumAdapter(albums) { album ->
            // 当相册被选中时，更新图片列表
            binding.rvImages.adapter = ImageAdapter(album.imageFiles) { file, isSelected ->
                if (isSelected) {
                    selectedImages.add(file)
                } else {
                    selectedImages.remove(file)
                }
            }
        }

        // 默认加载第一个相册
        if (albums.isNotEmpty()) {
            binding.rvImages.adapter = ImageAdapter(albums[0].imageFiles, { file, isSelected ->
                if (isSelected) {
                    selectedImages.add(file)
                } else {
                    selectedImages.remove(file)
                }
            })
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

        // 添加排序条件
        val sortOrder = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            Log.d("AlbumQuery", "Cursor count: ${cursor.count}")
            
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketNameColumn) ?: continue
                val path = cursor.getString(dataColumn) ?: continue

                // 确保文件存在
                val file = File(path).takeIf { it.exists() } ?: continue
                albumsMap.getOrPut(bucketName) { mutableListOf() }.add(file)
            }
        }

        return albumsMap.map { (name, files) ->
            // 按修改时间排序，确保封面是最新图片
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            Album(name, sortedFiles.first().absolutePath, sortedFiles)
        }
    }
    private fun getImageFilesFromStorage(): List<File> {
        val imageFiles = mutableListOf<File>()

        // 查询MediaStore获取所有图片
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                File(path)?.takeIf { it.exists() }?.let { imageFiles.add(it) }
            }
        }

        return imageFiles
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty() 
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadImages()
        }
    }
}
