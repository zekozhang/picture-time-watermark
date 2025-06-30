package com.example.picturemarker

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.picturemarker.ui.theme.PictureMarkerTheme
import com.example.picturemarker.utils.WatermarkUtils
import java.io.File
import kotlin.compareTo
import kotlin.dec
import kotlin.inc
import kotlin.text.compareTo

class WatermarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageFiles = intent.getSerializableExtra("image_files") as? List<File> ?: emptyList()

        setContent {
            PictureMarkerTheme {
                WatermarkScreen(
                    imageFiles = imageFiles,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    imageFiles: List<File>,
    onBack: () -> Unit
) {
    var currentPosition by remember { mutableIntStateOf(0) }
    var watermarkedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    fun showPreviousImage() {
        if (currentPosition > 0) currentPosition--
    }

    fun showNextImage() {
        if (currentPosition < imageFiles.size - 1) currentPosition++
    }

    // 处理位置选择
    var selectedPosition by remember {
        mutableStateOf(WatermarkUtils.WatermarkPosition.BOTTOM_RIGHT)
    }

    // 显示对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择水印位置") },
            text = {
                Column {
                    WatermarkUtils.WatermarkPosition.values().forEachIndexed { index, position ->
                        TextButton(
                            onClick = {
                                selectedPosition = position
                                watermarkedUris = imageFiles.mapNotNull { file ->
                                    WatermarkUtils.addWatermark(context, file, file, position)
                                }
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when(position) {
                                    WatermarkUtils.WatermarkPosition.TOP_LEFT -> "左上角"
                                    WatermarkUtils.WatermarkPosition.TOP_RIGHT -> "右上角"
                                    WatermarkUtils.WatermarkPosition.BOTTOM_LEFT -> "左下角"
                                    WatermarkUtils.WatermarkPosition.BOTTOM_RIGHT -> "右下角"
                                    WatermarkUtils.WatermarkPosition.CENTER -> "居中"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("添加水印") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (imageFiles.isNotEmpty()) {
                val currentFile = imageFiles[currentPosition]
                val currentUri = watermarkedUris.getOrNull(currentPosition) ?: Uri.fromFile(currentFile)

                // 图片预览（整体上移）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 105.dp) // 为底部按钮留出空间
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(currentUri)
                                    .build()
                            ),
                            contentDescription = "图片预览",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        when {
                                            dragAmount > 10 -> showPreviousImage()
                                            dragAmount < -10 -> showNextImage()
                                        }
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )

                        // 页码指示器（右上角）
                        Text(
                            text = "${currentPosition + 1}/${imageFiles.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.3f))
                        )

                        // 导航按钮
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // 上一张按钮
                            if (currentPosition > 0) {
                                IconButton(
                                    onClick = { showPreviousImage() },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "上一张",
                                        tint = Color.White
                                    )
                                }
                            }

                            // 下一张按钮
                            if (currentPosition < imageFiles.size - 1) {
                                IconButton(
                                    onClick = { showNextImage() },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "下一张",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 底部按钮组（居中显示）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    watermarkedUris = imageFiles.mapNotNull { file ->
                                        WatermarkUtils.addWatermark(
                                            context,
                                            file,
                                            file,
                                            WatermarkUtils.WatermarkPosition.BOTTOM_RIGHT
                                        )
                                    }
                                },
                                icon = { Icon(Icons.Default.PlayArrow, "快速添加水印") },
                                text = { Text("快速添加水印") },
                                modifier = Modifier.widthIn(min = 150.dp)
                            )

                            ExtendedFloatingActionButton(
                                onClick = { showDialog = true },
                                icon = { Icon(Icons.Default.Settings, "自定义添加水印") },
                                text = { Text("自定义添加水印") },
                                modifier = Modifier.widthIn(min = 150.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有可用的图片")
                }
            }
        }
    }
}