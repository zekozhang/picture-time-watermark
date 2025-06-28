package com.example.picturemarker.utils

import android.R.attr.delay
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object WatermarkUtils {
    fun addWatermark(
        context: Context,
        originalFile: File,
        outputFile: File,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        text: String? = null
    ): Uri? {
        return try {
            Log.i("Watermark", "输入文件路径: ${originalFile.absolutePath}")

            // 1. Get original bitmap
            /*val options = BitmapFactory.Options()*/
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            options.inJustDecodeBounds = false
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options)
            Log.i("Watermark", "原始Bitmap尺寸: ${originalBitmap.width}x${originalBitmap.height}") // 检查是否成功加载

            // 2. Create new bitmap with watermark
            val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)
            /*val paint = Paint().apply {
                color = Color.WHITE
                textSize = watermarkedBitmap.width * 0.03f
                isAntiAlias = true
                setShadowLayer(5f, 0f, 0f, Color.BLACK)
            }*/
            val paint = Paint().apply {
                color = Color.WHITE
                // textSize = 40f
                isAntiAlias = true
                setShadowLayer(5f, 0f, 0f, Color.BLACK) // 添加阴影增强对比度
                textSize = watermarkedBitmap.width * 0.04f
            }

            // 3. Get watermark text (use EXIF date if not provided)
            val watermarkText = text ?: getExifDateTime(originalFile) ?: 
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            // 4. Calculate position
            val (x, y) = calculatePosition(watermarkedBitmap, paint, watermarkText, position)
            Log.i("Watermark", "水印位置: ($x, $y)") // 检查水印坐标

            // 5. Draw watermark
            canvas.drawText(watermarkText, x, y, paint)


            try {
                // 6. Save to output file
                /*FileOutputStream(outputFile).use { out ->
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }*/

                // 6. Save to MediaStore
                saveBitmapToMediaStore(context, watermarkedBitmap, originalFile.name)
            } catch (e: IOException) {
                Log.d("Watermark", "Save image error: ${e.message}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Uri? {
        val resolver = context.contentResolver

        val baseName = displayName.substringBeforeLast(".")
        val extension = displayName.substringAfterLast(".", "png") // 默认png
        val timestamp = System.currentTimeMillis()
        val newDisplayName = "wm_${timestamp}_${baseName}.$extension"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, newDisplayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            // 3. 强制刷新 MediaStore
            triggerMediaStoreScan(context, uri)

            val fileSize = getMediaStoreFileSize(resolver, uri)
            Log.d("Watermark", "Saved to: $uri | File size: ${fileSize / 1024} KB")
            return uri
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
    }

    private fun triggerMediaStoreScan(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        context.sendBroadcast(intent)
    }

    fun createWatermarkedFileName(originalFile: File): File {
        // 获取文件名（不含扩展名）和扩展名
        val fileName = originalFile.nameWithoutExtension
        val fileExtension = originalFile.extension

        // 构建新文件名（添加 _wm 后缀）
        val newFileName = "${fileName}_wm.${fileExtension}"

        // 创建新文件对象（与原文件同一目录）
        return File(originalFile.parent, newFileName)
    }

    fun addWatermarkWithoutCompression(
        originalFile: File,
        outputFile: File,
        position: WatermarkPosition
    ): Boolean {
        return try {
            // 1. 加载原图（禁用压缩选项）
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888 // 保持最高质量
                inSampleSize = 1 // 禁止缩放
            }
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options)
                ?: return false

            // 2. 创建水印Bitmap
            val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = watermarkedBitmap.width * 0.03f
                isAntiAlias = true
            }

            // 3. 绘制水印
            val watermarkText = "2023-01-01"
            val (x, y) = calculatePosition(watermarkedBitmap, paint, watermarkText, position)
            canvas.drawText(watermarkText, x, y, paint)

            // 4. 保存为PNG（无损格式）
            val newOutputFile = createWatermarkedFileName(originalFile);
            FileOutputStream(newOutputFile).use { out ->
                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // PNG不压缩
                out.flush()
            }
            true
        } catch (e: Exception) {
            Log.d("Watermark", "Save image error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun getMediaStoreFileSize(resolver: ContentResolver, uri: Uri): Long {
        return try {
            val cursor = resolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.SIZE),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(0)
                } else 0
            } ?: 0
        } catch (e: Exception) {
            Log.e("Watermark", "Failed to get file size", e)
            0
        }
    }

    private fun getExifDateTime(file: File): String? {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
            date?.let { 
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    .parse(it)
                    ?.let { parsedDate -> 
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(parsedDate)
                    }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculatePosition(
        bitmap: Bitmap,
        paint: Paint,
        text: String,
        position: WatermarkPosition
    ): Pair<Float, Float> {
        val textWidth = paint.measureText(text)
        val textHeight = paint.textSize
        val margin = bitmap.width * 0.02f

        return when (position) {
            WatermarkPosition.TOP_LEFT -> Pair(margin, textHeight + margin)
            WatermarkPosition.TOP_RIGHT -> Pair(bitmap.width - textWidth - margin, textHeight + margin)
            WatermarkPosition.BOTTOM_LEFT -> Pair(margin, bitmap.height - margin)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(bitmap.width - textWidth - margin, bitmap.height - margin)
            WatermarkPosition.CENTER -> Pair(
                (bitmap.width - textWidth) / 2,
                (bitmap.height + textHeight) / 2
            )
        }
    }

    enum class WatermarkPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }
}
