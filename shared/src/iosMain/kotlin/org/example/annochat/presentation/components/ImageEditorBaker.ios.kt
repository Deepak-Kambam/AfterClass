package org.example.annochat.presentation.components

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

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
        val srcImage = Image.makeFromEncoded(originalBytes)
        val srcW = srcImage.width
        val srcH = srcImage.height

        // Determine cropped boundaries
        val left = if (cropRect != null) (cropRect.left.coerceIn(0f, 1f) * srcW).toInt() else 0
        val top = if (cropRect != null) (cropRect.top.coerceIn(0f, 1f) * srcH).toInt() else 0
        val right = if (cropRect != null) (cropRect.right.coerceIn(0f, 1f) * srcW).toInt() else srcW
        val bottom = if (cropRect != null) (cropRect.bottom.coerceIn(0f, 1f) * srcH).toInt() else srcH

        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        // Rotation dimension calculation (swap if 90 or 270)
        val isQuarterRotated = (rotationDegrees.toInt() % 180 != 0)
        val targetW = if (isQuarterRotated) cropH else cropW
        val targetH = if (isQuarterRotated) cropW else cropH

        val surface = Surface.makeRasterN32Premul(targetW, targetH) ?: return originalBytes
        val canvas = surface.canvas

        // Apply transformations
        canvas.save()
        canvas.translate(targetW / 2f, targetH / 2f)
        if (isFlippedHorizontal) {
            canvas.scale(-1f, 1f)
        }
        canvas.rotate(rotationDegrees)
        canvas.translate(-cropW / 2f, -cropH / 2f)

        val basePaint = Paint()
        if (aiStyle.matrix != null) {
            val m = aiStyle.matrix.values
            basePaint.colorFilter = ColorFilter.makeMatrix(org.jetbrains.skia.ColorMatrix(m))
        }

        // Draw cropped source image into (0, 0, cropW, cropH)
        val srcRect = Rect.makeLTRB(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        val dstRect = Rect.makeWH(cropW.toFloat(), cropH.toFloat())
        canvas.drawImageRect(srcImage, srcRect, dstRect, basePaint)

        // Scaling factor from UI canvas to crop dimensions
        val scaleX = if (canvasWidth > 0f) cropW.toFloat() / canvasWidth else 1f
        val scaleY = if (canvasHeight > 0f) cropH.toFloat() / canvasHeight else 1f

        // Draw doodles
        doodleStrokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                val strokePaint = Paint().apply {
                    color = ((stroke.color.alpha * 255).toInt() shl 24) or
                            ((stroke.color.red * 255).toInt() shl 16) or
                            ((stroke.color.green * 255).toInt() shl 8) or
                            (stroke.color.blue * 255).toInt()
                    mode = org.jetbrains.skia.PaintMode.STROKE
                    strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
                    strokeCap = org.jetbrains.skia.PaintStrokeCap.ROUND
                    strokeJoin = org.jetbrains.skia.PaintStrokeJoin.ROUND
                }
                for (i in 0 until stroke.points.size - 1) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i + 1]
                    canvas.drawLine(
                        p1.x * scaleX,
                        p1.y * scaleY,
                        p2.x * scaleX,
                        p2.y * scaleY,
                        strokePaint
                    )
                }
            }
        }

        canvas.restore()

        val finalImage = surface.makeImageSnapshot()
        val data = finalImage.encodeToData(EncodedImageFormat.JPEG, 90)
        data?.bytes ?: originalBytes
    } catch (_: Throwable) {
        originalBytes
    }
}
