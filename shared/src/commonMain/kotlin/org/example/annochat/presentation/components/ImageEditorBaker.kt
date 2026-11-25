package org.example.annochat.presentation.components

/**
 * Normalized crop rectangle coordinates (0f..1f).
 */
data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

/**
 * Bakes all client-side visual edits (crop, rotation, AI color filter, doodles, stickers, and text)
 * directly into a newly encoded JPEG byte array.
 * If no edits were made, returns originalBytes directly.
 */
expect fun bakeEditedImage(
    originalBytes: ByteArray,
    rotationDegrees: Float,
    cropRect: NormalizedCropRect?,
    isFlippedHorizontal: Boolean = false,
    aiStyle: AiRestyleStyle,
    doodleStrokes: List<EditorDoodleStroke>,
    placedStickers: List<EditorPlacedSticker>,
    placedTexts: List<EditorPlacedText>,
    canvasWidth: Float,
    canvasHeight: Float
): ByteArray
