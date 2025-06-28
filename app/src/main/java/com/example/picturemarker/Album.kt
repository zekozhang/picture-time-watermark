package com.example.picturemarker

import java.io.File

data class Album(
    val name: String,      // 相册名称（如"相机"、"截图"）
    val coverPath: String, // 封面图片路径
    val imageFiles: List<File>
)