package com.example.picturemarker

import java.io.File

data class Album(
    val name: String,
    val coverPath: String,
    val imageFiles: List<File>,
    val count: Int
)