package org.example.annochat.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import java.io.ByteArrayOutputStream

actual fun bakeEditedImage(
    originalBytes: ByteArray,
    rotationDegrees: Float,
    cropRect: NormalizedCropRect?,
    isFlippedHorizontal: Boolean,
    aiStyle: AiRestyleStyle,
    doodleStrokes: List<EditorDoodleStroke>,
    placedStickers: List<EditorPlacedSticker>,
    placedTexts: List<EditorPlacedText>,
    canvasWidth: Float,
    canvasHeight: Float
): ByteArray {
    if (originalBytes.isEmpty()) return originalBytes

    val hasEdits = (rotationDegrees % 360f != 0f) ||
        isFlippedHorizontal ||
        (cropRect != null && (cropRect.left > 0.01f || cropRect.top > 0.01f || cropRect.right < 0.99f || cropRect.bottom < 0.99f)) ||
        aiStyle != AiRestyleStyle.ORIGINAL ||
        doodleStrokes.isNotEmpty() ||
        placedStickers.isNotEmpty() ||
        placedTexts.isNotEmpty()

    if (!hasEdits) return originalBytes

    return try {
        val srcBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size) ?: return originalBytes

        // 1. Crop if specified
        var currentBitmap = srcBitmap
        if (cropRect != null) {
            val srcW = srcBitmap.width
            val srcH = srcBitmap.height
            val left = (cropRect.left.coerceIn(0f, 1f) * srcW).toInt().coerceIn(0, srcW - 1)
            val top = (cropRect.top.coerceIn(0f, 1f) * srcH).toInt().coerceIn(0, srcH - 1)
            val right = (cropRect.right.coerceIn(0f, 1f) * srcW).toInt().coerceIn(left + 1, srcW)
            val bottom = (cropRect.bottom.coerceIn(0f, 1f) * srcH).toInt().coerceIn(top + 1, srcH)
            val width = (right - left).coerceAtLeast(1)
            val height = (bottom - top).coerceAtLeast(1)
            currentBitmap = Bitmap.createBitmap(srcBitmap, left, top, width, height)
        }

        // 2. Rotate & Flip if specified
        if (rotationDegrees % 360f != 0f || isFlippedHorizontal) {
            val matrix = Matrix().apply {
                if (isFlippedHorizontal) postScale(-1f, 1f, currentBitmap.width / 2f, currentBitmap.height / 2f)
                if (rotationDegrees % 360f != 0f) postRotate(rotationDegrees)
            }
            currentBitmap = Bitmap.createBitmap(
                currentBitmap,
                0,
                0,
                currentBitmap.width,
                currentBitmap.height,
                matrix,
                true
            )
        }

        // 3. Create composite canvas
        val resultBitmap = Bitmap.createBitmap(currentBitmap.width, currentBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 4. Draw base bitmap with AI color filter if active
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (aiStyle.matrix != null) {
            basePaint.colorFilter = ColorMatrixColorFilter(aiStyle.matrix.values)
        }
        canvas.drawBitmap(currentBitmap, 0f, 0f, basePaint)

        // Calculate scaling from UI canvas coordinates to bitmap pixels
        val scaleX = if (canvasWidth > 0f) currentBitmap.width.toFloat() / canvasWidth else 1f
        val scaleY = if (canvasHeight > 0f) currentBitmap.height.toFloat() / canvasHeight else 1f
        val avgScale = (scaleX + scaleY) / 2f

        // 5. Draw doodle strokes
        doodleStrokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.color.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = stroke.strokeWidth * avgScale
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = android.graphics.Path()
                path.moveTo(stroke.points[0].x * scaleX, stroke.points[0].y * scaleY)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
                }
                canvas.drawPath(path, strokePaint)
            }
        }

        // 6. Draw stickers (emojis)
        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 42f * avgScale
            textAlign = Paint.Align.CENTER
        }
        placedStickers.forEach { sticker ->
            val x = (canvasWidth / 2f + sticker.offset.x) * scaleX
            val y = (canvasHeight / 2f + sticker.offset.y) * scaleY
            canvas.drawText(sticker.emoji, x, y, stickerPaint)
        }

        // 7. Draw text overlays
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f * avgScale
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        placedTexts.forEach { textItem ->
            textPaint.color = textItem.color.toArgb()
            val x = (canvasWidth / 2f + textItem.offset.x) * scaleX
            val y = (canvasHeight / 2f + textItem.offset.y) * scaleY
            val bounds = android.graphics.Rect()
            textPaint.getTextBounds(textItem.text, 0, textItem.text.length, bounds)
            val padX = 14f * scaleX
            val padY = 8f * scaleY
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(160, 0, 0, 0)
            }
            canvas.drawRoundRect(
                x - bounds.width() / 2f - padX,
                y - bounds.height() - padY,
                x + bounds.width() / 2f + padX,
                y + padY,
                10f * scaleX,
                10f * scaleY,
                bgPaint
            )
            canvas.drawText(textItem.text, x, y, textPaint)
        }

        val outStream = ByteArrayOutputStream()
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
        outStream.toByteArray()
    } catch (_: Throwable) {
        originalBytes
    }
}
