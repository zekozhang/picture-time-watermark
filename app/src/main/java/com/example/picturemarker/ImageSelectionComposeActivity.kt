package com.example.picturemarker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.picturemarker.ui.theme.PictureMarkerTheme
import java.io.File
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectionComposeActivity : ComponentActivity() {
    private val REQUEST_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PictureMarkerTheme {
                ImageSelectionScreen(
                    onBack = { finish() },
                    onImagesSelected = { selectedFiles ->
                        if (selectedFiles.isNotEmpty()) {
                            startActivity(
                                Intent(this, WatermarkActivity::class.java).apply {
                                    putExtra("image_files", ArrayList(selectedFiles))
                                }
                            )
                        }
                    }
                )
            }
        }
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
                // Permission already granted
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, recomposition will handle the rest
        } else {
            Toast.makeText(this, "需要权限才能访问照片", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectionScreen(
    onBack: () -> Unit,
    onImagesSelected: (List<File>) -> Unit
) {
    val context = LocalContext.current
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var selectedAlbumIndex by remember { mutableIntStateOf(0) }
    var selectedImages by remember { mutableStateOf<Set<File>>(emptySet()) }
    var isRefreshing by remember { mutableStateOf(false) } // 新增：刷新状态


    // 新增：旋转动画的角度
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_transition")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )

    val finalRotation = if (isRefreshing) rotationAngle else 0f


    // 修改刷新函数
    fun refreshAlbums() {
        isRefreshing = true
        selectedImages = emptySet()
        albums = emptyList() // 清空当前显示

        // 使用协程模拟异步加载
        CoroutineScope(Dispatchers.IO).launch {
            val newAlbums = getAlbumsFromStorage(context)
            withContext(Dispatchers.Main) {
                albums = newAlbums
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAlbums()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.select_images)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshAlbums() },
                        enabled = !isRefreshing // 刷新时禁用按钮
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            modifier = Modifier
                                .rotate(finalRotation)
                                .size(24.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedImages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onImagesSelected(selectedImages.toList()) },
                    icon = { Icon(Icons.Default.Done, contentDescription = null) },
                    text = { Text(stringResource(R.string.confirm_selection)) }
                )
            }
        }
    ) { padding ->
        // 新增：加载指示器
        if (isRefreshing && albums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Side Navigation Rail
                NavigationRail {
                    albums.forEachIndexed { index, album ->
                        NavigationRailItem(
                            selected = selectedAlbumIndex == index,
                            onClick = { selectedAlbumIndex = index },
                            icon = {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = album.name
                                )
                            },
                            label = { Text(album.name) }
                        )
                    }
                }

                // Image Grid
                if (albums.isNotEmpty()) {
                    val currentAlbum = albums[selectedAlbumIndex]
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(currentAlbum.imageFiles) { file ->
                            ImageGridItem(
                                file = file,
                                isSelected = selectedImages.contains(file),
                                onSelectedChange = { isSelected ->
                                    selectedImages = if (isSelected) {
                                        selectedImages + file
                                    } else {
                                        selectedImages - file
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("没有找到图片")
                    }
                }
            }
        }
    }
}

@Composable
fun ImageGridItem(
    file: File,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable { onSelectedChange(!isSelected) }
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .size(coil.size.Size.ORIGINAL)
                    .build()
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )
        }

        // 自定义复选框容器
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clickable { onSelectedChange(!isSelected) }
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        width = if (isSelected) 0.dp else 1.dp, // 选中时无边框
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .background(
                        if (isSelected) Color(0xFF276FF8) else Color.Transparent // 选中时浅蓝色
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun getAlbumsFromStorage(context: android.content.Context): List<Album> {
    val albumsMap = mutableMapOf<String, MutableList<File>>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.DATE_MODIFIED
    )

    val sortOrder = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC"

    context.contentResolver.query(
        getMediaStoreUri(),
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
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