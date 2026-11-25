package org.example.annochat.presentation.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Coherent, professional vector iconography for AnonChat.
 * Designed based on the Flaticon UIcons Rounded design system:
 * - 24x24 dp viewport
 * - Consistent stroke width (~2.2 - 2.5dp) & corner radiuses
 * - Round stroke caps and line joins (StrokeCap.Round, StrokeJoin.Round)
 * - Pure monochrome aesthetic matching AnonChat's black & white cinematic theme
 *
 * Attribution:
 * Icons designed with reference to Flaticon UIcons (Freepik Company) under Free License with Attribution.
 * https://www.flaticon.com/uicons
 */
object AnonChatIcons {

    // ── Navigation & Directional (Special back chevron matching user reference) ──

    /**
     * Back navigation chevron matching the user reference screenshot.
     * Sleek rounded '<' with rounded stroke caps and smooth rounded vertex.
     */
    val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.5f, 6.5f)
                lineTo(9f, 12f)
                lineTo(14.5f, 17.5f)
            }
        }.build()
    }

    val ArrowBack: ImageVector by lazy { ChevronLeft }
    val ArrowLeft: ImageVector by lazy { ChevronLeft }

    /**
     * Forward navigation chevron '>'.
     */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.5f, 6.5f)
                lineTo(15f, 12f)
                lineTo(9.5f, 17.5f)
            }
        }.build()
    }

    /**
     * Downward navigation chevron 'v'.
     */
    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 9.5f)
                lineTo(12f, 15f)
                lineTo(17.5f, 9.5f)
            }
        }.build()
    }

    /**
     * Rightward directional action arrow (for SliderToEnter and Action Buttons).
     */
    val ArrowRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Horizontal stem
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 12f)
                lineTo(18.5f, 12f)
            }
            // Arrow head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 6.5f)
                lineTo(18.5f, 12f)
                lineTo(13f, 17.5f)
            }
        }.build()
    }

    // ── Messaging & Chat Actions ─────────────────────────────────────────────

    /**
     * Flaticon UIcons Rounded Paper Plane for message dispatch.
     */
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.5f, 2.5f)
                lineTo(3f, 10.5f)
                lineTo(10.5f, 13.5f)
                lineTo(13.5f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 13.5f)
                lineTo(21.5f, 2.5f)
            }
        }.build()
    }

    /**
     * Sleek lightning bolt icon for Match / instant pairing.
     */
    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.5f, 2.5f)
                lineTo(5.5f, 13f)
                lineTo(11.5f, 13f)
                lineTo(10.5f, 21.5f)
                lineTo(18.5f, 11f)
                lineTo(12.5f, 11f)
                close()
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Paperclip for media/image attachments.
     */
    val Attachment: ImageVector by lazy {
        ImageVector.Builder(
            name = "Attachment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 7.5f)
                lineTo(8.5f, 15.5f)
                curveTo(7f, 17f, 7f, 19.5f, 8.5f, 21f)
                curveTo(10f, 22.5f, 12.5f, 22.5f, 14f, 21f)
                lineTo(20f, 15f)
                curveTo(22.2f, 12.8f, 22.2f, 9.2f, 20f, 7f)
                curveTo(17.8f, 4.8f, 14.2f, 4.8f, 12f, 7f)
                lineTo(6f, 13f)
                curveTo(4.8f, 14.2f, 4.8f, 16.2f, 6f, 17.4f)
                curveTo(7.2f, 18.6f, 9.2f, 18.6f, 10.4f, 17.4f)
                lineTo(16f, 11.8f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Smiling face for Stickers and GIFs.
     */
    val StickerSmile: ImageVector by lazy {
        ImageVector.Builder(
            name = "StickerSmile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Face perimeter
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                close()
            }
            // Left eye dot
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 9f)
                curveTo(8.45f, 9f, 8f, 9.45f, 8f, 10f)
                curveTo(8f, 10.55f, 8.45f, 11f, 9f, 11f)
                curveTo(9.55f, 11f, 10f, 10.55f, 10f, 10f)
                curveTo(10f, 9.45f, 9.55f, 9f, 9f, 9f)
                close()
            }
            // Right eye dot
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 9f)
                curveTo(14.45f, 9f, 14f, 9.45f, 14f, 10f)
                curveTo(14f, 10.55f, 14.45f, 11f, 15f, 11f)
                curveTo(15.55f, 11f, 16f, 10.55f, 16f, 10f)
                curveTo(16f, 9.45f, 15.55f, 9f, 15f, 9f)
                close()
            }
            // Smile curve
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8.5f, 14.5f)
                curveTo(9.5f, 16.5f, 14.5f, 16.5f, 15.5f, 14.5f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Keyboard for switching back to text mode.
     */
    val Keyboard: ImageVector by lazy {
        ImageVector.Builder(
            name = "Keyboard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 6f)
                lineTo(18.5f, 6f)
                curveTo(20.16f, 6f, 21.5f, 7.34f, 21.5f, 9f)
                lineTo(21.5f, 15f)
                curveTo(21.5f, 16.66f, 20.16f, 18f, 18.5f, 18f)
                lineTo(5.5f, 18f)
                curveTo(3.84f, 18f, 2.5f, 16.66f, 2.5f, 15f)
                lineTo(2.5f, 9f)
                curveTo(2.5f, 7.34f, 3.84f, 6f, 5.5f, 6f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.5f, 9.5f); lineTo(6.51f, 9.5f)
                moveTo(10.2f, 9.5f); lineTo(10.21f, 9.5f)
                moveTo(13.8f, 9.5f); lineTo(13.81f, 9.5f)
                moveTo(17.5f, 9.5f); lineTo(17.51f, 9.5f)
                moveTo(6.5f, 12.5f); lineTo(6.51f, 12.5f)
                moveTo(10.2f, 12.5f); lineTo(10.21f, 12.5f)
                moveTo(13.8f, 12.5f); lineTo(13.81f, 12.5f)
                moveTo(17.5f, 12.5f); lineTo(17.51f, 12.5f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8.5f, 15.2f)
                lineTo(15.5f, 15.2f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons 3-dot vertical menu.
     */
    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top dot
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 4.5f)
                curveTo(10.9f, 4.5f, 10f, 5.4f, 10f, 6.5f)
                curveTo(10f, 7.6f, 10.9f, 8.5f, 12f, 8.5f)
                curveTo(13.1f, 8.5f, 14f, 7.6f, 14f, 6.5f)
                curveTo(14f, 5.4f, 13.1f, 4.5f, 12f, 4.5f)
                close()
            }
            // Middle dot
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                close()
            }
            // Bottom dot
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 15.5f)
                curveTo(10.9f, 15.5f, 10f, 16.4f, 10f, 17.5f)
                curveTo(10f, 18.6f, 10.9f, 19.5f, 12f, 19.5f)
                curveTo(13.1f, 19.5f, 14f, 18.6f, 14f, 17.5f)
                curveTo(14f, 16.4f, 13.1f, 15.5f, 12f, 15.5f)
                close()
            }
        }.build()
    }

    /**
     * 3-dot horizontal menu icon.
     */
    val MoreHoriz: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left dot
            path(fill = SolidColor(Color.White)) {
                moveTo(6.5f, 10f)
                curveTo(5.4f, 10f, 4.5f, 10.9f, 4.5f, 12f)
                curveTo(4.5f, 13.1f, 5.4f, 14f, 6.5f, 14f)
                curveTo(7.6f, 14f, 8.5f, 13.1f, 8.5f, 12f)
                curveTo(8.5f, 10.9f, 7.6f, 10f, 6.5f, 10f)
                close()
            }
            // Middle dot
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                close()
            }
            // Right dot
            path(fill = SolidColor(Color.White)) {
                moveTo(17.5f, 10f)
                curveTo(16.4f, 10f, 15.5f, 10.9f, 15.5f, 12f)
                curveTo(15.5f, 13.1f, 16.4f, 14f, 17.5f, 14f)
                curveTo(18.6f, 14f, 19.5f, 13.1f, 19.5f, 12f)
                curveTo(19.5f, 10.9f, 18.6f, 10f, 17.5f, 10f)
                close()
            }
        }.build()
    }

    /**
     * Telephone handset icon.
     */
    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.62f, 10.79f)
                curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
                lineTo(15.41f, 15.18f)
                curveTo(15.69f, 14.9f, 16.08f, 14.82f, 16.43f, 14.94f)
                curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
                curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
                lineTo(21f, 20f)
                curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
                curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
                curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
                lineTo(7.5f, 3f)
                curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
                curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
                curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
                lineTo(6.62f, 10.79f)
                close()
            }
        }.build()
    }

    /**
     * Half-filled contrast / liquid glass icon (circle partitioned half-filled).
     */
    val LiquidGlass: ImageVector by lazy {
        ImageVector.Builder(
            name = "LiquidGlass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outline circle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
            }
            // Left filled half
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                close()
            }
        }.build()
    }

    /**
     * Artist Palette icon for Appearance & Personalization.
     */
    val Palette: ImageVector by lazy {
        ImageVector.Builder(
            name = "Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, false, false, 3f, 12f)
                arcTo(9f, 9f, 0f, false, false, 7.5f, 19.8f)
                curveTo(8.2f, 20.4f, 9.2f, 20f, 9.2f, 19.1f)
                curveTo(9.2f, 18.2f, 9.9f, 17.5f, 10.8f, 17.5f)
                lineTo(12.5f, 17.5f)
                curveTo(16.6f, 17.5f, 21f, 14.1f, 21f, 10f)
                curveTo(21f, 6.1f, 17f, 3f, 12f, 3f)
                close()
            }
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(7.5f, 9.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 7.5f, 7.1f)
                arcTo(1.2f, 1.2f, 0f, false, true, 7.5f, 9.5f)

                moveTo(12f, 7.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 12f, 5.1f)
                arcTo(1.2f, 1.2f, 0f, false, true, 12f, 7.5f)

                moveTo(16.5f, 9.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 16.5f, 7.1f)
                arcTo(1.2f, 1.2f, 0f, false, true, 16.5f, 9.5f)

                moveTo(17.5f, 13.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 17.5f, 11.1f)
                arcTo(1.2f, 1.2f, 0f, false, true, 17.5f, 13.5f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded 'X' Close cross.
     */
    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Checkmark.
     */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 12.5f)
                lineTo(9.5f, 17.5f)
                lineTo(19.5f, 6.5f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Dual Document Copy icon.
     */
    val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Front sheet
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7f)
                horizontalLineTo(15f)
                curveTo(16.1f, 7f, 17f, 7.9f, 17f, 9f)
                verticalLineTo(19f)
                curveTo(17f, 20.1f, 16.1f, 21f, 15f, 21f)
                horizontalLineTo(7f)
                curveTo(5.9f, 21f, 5f, 20.1f, 5f, 19f)
                verticalLineTo(9f)
                curveTo(5f, 7.9f, 5.9f, 7f, 7f, 7f)
                close()
            }
            // Back sheet edge
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 3.5f)
                horizontalLineTo(17f)
                curveTo(18.1f, 3.5f, 19f, 4.4f, 19f, 5.5f)
                verticalLineTo(16f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Leftward Curved Reply arrow.
     */
    val Reply: ImageVector by lazy {
        ImageVector.Builder(
            name = "Reply",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Arrow head
                moveTo(8.5f, 5f)
                lineTo(3.5f, 10f)
                lineTo(8.5f, 15f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Body curve
                moveTo(4f, 10f)
                horizontalLineTo(13f)
                curveTo(17f, 10f, 20.5f, 13f, 20.5f, 17.5f)
                curveTo(20.5f, 18.5f, 20f, 19.5f, 19.5f, 20f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded User Plus (Add peer to chat).
     */
    val UserPlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "UserPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 11f)
                curveTo(6.8f, 11f, 5f, 9.2f, 5f, 7f)
                curveTo(5f, 4.8f, 6.8f, 3f, 9f, 3f)
                curveTo(11.2f, 3f, 13f, 4.8f, 13f, 7f)
                curveTo(13f, 9.2f, 11.2f, 11f, 9f, 11f)
                close()
            }
            // Torso
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.5f, 20f)
                curveTo(2.5f, 16.5f, 5.5f, 14.5f, 9f, 14.5f)
                curveTo(11f, 14.5f, 13f, 15.3f, 14f, 16.8f)
            }
            // Plus symbol
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18f, 13f)
                lineTo(18f, 19f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15f, 16f)
                lineTo(21f, 16f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded LogOut / Leave room.
     */
    val LogOut: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogOut",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Door / bracket
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 4f)
                horizontalLineTo(5f)
                curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
                verticalLineTo(18f)
                curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
                horizontalLineTo(10f)
            }
            // Exit stem
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 12f)
                lineTo(21f, 12f)
            }
            // Exit arrow head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 7.5f)
                lineTo(21f, 12f)
                lineTo(16.5f, 16.5f)
            }
        }.build()
    }

    /**
     * Thumbtack / Pin icon for pinning a group to the top.
     */
    val Pin: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Pin head (diamond)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                lineTo(17f, 8f)
                curveTo(17f, 8f, 15.5f, 10f, 15.5f, 12f)
                lineTo(11f, 13f)
                lineTo(9.5f, 16f)
                lineTo(8f, 14.5f)
                lineTo(9f, 13f)
                lineTo(7f, 8.5f)
                curveTo(9f, 8.5f, 11f, 7f, 11f, 7f)
                close()
            }
            // Pin stem
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 14.5f)
                lineTo(4.5f, 18f)
            }
        }.build()
    }

    /**
     * Trash / Delete bin icon for removing / leaving a group.
     */
    val Trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Lid
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 7f)
                lineTo(19.5f, 7f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 3.5f)
                horizontalLineTo(14f)
            }
            // Body
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 7f)
                lineTo(6.5f, 19f)
                curveTo(6.5f, 19.83f, 7.17f, 20.5f, 8f, 20.5f)
                horizontalLineTo(16f)
                curveTo(16.83f, 20.5f, 17.5f, 19.83f, 17.5f, 19f)
                lineTo(18.5f, 7f)
            }
            // Inner lines
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10f, 11f)
                lineTo(10f, 17f)
                moveTo(14f, 11f)
                lineTo(14f, 17f)
            }
        }.build()
    }

    // ── Bottom Navigation Tabs (Icon-Only Capsule) ───────────────────────────

    /**
     * Dual rounded speech bubbles for CHATS tab.
     */
    val ChatBubbles: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChatBubbles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Front primary speech bubble
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 15f)
                lineTo(3f, 18.5f)
                lineTo(6.5f, 17.5f)
                curveTo(7.5f, 18f, 8.7f, 18.3f, 10f, 18.3f)
                curveTo(14.5f, 18.3f, 18f, 15.2f, 18f, 11.5f)
                curveTo(18f, 7.8f, 14.5f, 4.7f, 10f, 4.7f)
                curveTo(5.5f, 4.7f, 2f, 7.8f, 2f, 11.5f)
                curveTo(2f, 12.8f, 2.5f, 14f, 3.3f, 15f)
            }
            // Second offset bubble in background
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 6.5f)
                curveTo(19.5f, 7.5f, 22f, 10f, 22f, 12.5f)
                curveTo(22f, 14.5f, 20.5f, 16.2f, 18.5f, 17.2f)
                lineTo(19f, 20f)
                lineTo(16f, 19f)
            }
        }.build()
    }

    /**
     * Rounded peer group for CONTACTS tab.
     */
    val Users: ImageVector by lazy {
        ImageVector.Builder(
            name = "Users",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Front user head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 11f)
                curveTo(6.8f, 11f, 5f, 9.2f, 5f, 7f)
                curveTo(5f, 4.8f, 6.8f, 3f, 9f, 3f)
                curveTo(11.2f, 3f, 13f, 4.8f, 13f, 7f)
                curveTo(13f, 9.2f, 11.2f, 11f, 9f, 11f)
                close()
            }
            // Front user shoulders
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.5f, 20f)
                curveTo(2.5f, 16.2f, 5.8f, 14.5f, 9f, 14.5f)
                curveTo(12.2f, 14.5f, 15.5f, 16.2f, 15.5f, 20f)
            }
            // Back peer head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.5f, 4f)
                curveTo(17.2f, 4.3f, 18.5f, 5.8f, 18.5f, 7.5f)
                curveTo(18.5f, 9.2f, 17.2f, 10.7f, 15.5f, 11f)
            }
            // Back peer shoulder
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(17f, 15f)
                curveTo(19.5f, 15.8f, 21.5f, 17.5f, 21.5f, 20f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Settings Gear / Sliders for SETTINGS tab.
     */
    val SettingsGear: ImageVector by lazy {
        ImageVector.Builder(
            name = "SettingsGear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Center axle circle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 15f)
                curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
                curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
                curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
                curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
                close()
            }
            // Symmetrical clean rounded cog teeth outline
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 3.5f)
                horizontalLineTo(14f)
                lineTo(14.5f, 5.8f)
                lineTo(16.5f, 6.6f)
                lineTo(18.5f, 5.2f)
                lineTo(20.5f, 7.2f)
                lineTo(19.1f, 9.2f)
                lineTo(19.9f, 11.2f)
                lineTo(22.2f, 11.7f)
                verticalLineTo(14.3f)
                lineTo(19.9f, 14.8f)
                lineTo(19.1f, 16.8f)
                lineTo(20.5f, 18.8f)
                lineTo(18.5f, 20.8f)
                lineTo(16.5f, 19.4f)
                lineTo(14.5f, 20.2f)
                lineTo(14f, 22.5f)
                horizontalLineTo(10f)
                lineTo(9.5f, 20.2f)
                lineTo(7.5f, 19.4f)
                lineTo(5.5f, 20.8f)
                lineTo(3.5f, 18.8f)
                lineTo(4.9f, 16.8f)
                lineTo(4.1f, 14.8f)
                lineTo(1.8f, 14.3f)
                verticalLineTo(11.7f)
                lineTo(4.1f, 11.2f)
                lineTo(4.9f, 9.2f)
                lineTo(3.5f, 7.2f)
                lineTo(5.5f, 5.2f)
                lineTo(7.5f, 6.6f)
                lineTo(9.5f, 5.8f)
                close()
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded User in Circle for PROFILE tab.
     */
    val PersonCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "PersonCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer circle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 21.5f)
                curveTo(6.75f, 21.5f, 2.5f, 17.25f, 2.5f, 12f)
                curveTo(2.5f, 6.75f, 6.75f, 2.5f, 12f, 2.5f)
                curveTo(17.25f, 2.5f, 21.5f, 6.75f, 21.5f, 12f)
                curveTo(21.5f, 17.25f, 17.25f, 21.5f, 12f, 21.5f)
                close()
            }
            // Inner user head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 11.5f)
                curveTo(10.35f, 11.5f, 9f, 10.15f, 9f, 8.5f)
                curveTo(9f, 6.85f, 10.35f, 5.5f, 12f, 5.5f)
                curveTo(13.65f, 5.5f, 15f, 6.85f, 15f, 8.5f)
                curveTo(15f, 10.15f, 13.65f, 11.5f, 12f, 11.5f)
                close()
            }
            // Inner user shoulders arc
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.8f, 18.5f)
                curveTo(7.8f, 15.5f, 9.8f, 14.5f, 12f, 14.5f)
                curveTo(14.2f, 14.5f, 16.2f, 15.5f, 17.2f, 18.5f)
            }
        }.build()
    }

    /**
     * Clean single person silhouette.
     */
    val Person: ImageVector by lazy {
        ImageVector.Builder(
            name = "Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 11f)
                curveTo(9.8f, 11f, 8f, 9.2f, 8f, 7f)
                curveTo(8f, 4.8f, 9.8f, 3f, 12f, 3f)
                curveTo(14.2f, 3f, 16f, 4.8f, 16f, 7f)
                curveTo(16f, 9.2f, 14.2f, 11f, 12f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.5f, 21f)
                curveTo(4.5f, 16.5f, 8f, 14.5f, 12f, 14.5f)
                curveTo(16f, 14.5f, 19.5f, 16.5f, 19.5f, 21f)
            }
        }.build()
    }

    // ── Semantic Room Carousel Icons (Section 7) ─────────────────────────────

    /**
     * Public Room: Flaticon UIcons Rounded Globe / World.
     */
    val Globe: ImageVector by lazy {
        ImageVector.Builder(
            name = "Globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Globe perimeter
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                close()
            }
            // Equator line
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 12f)
                lineTo(21f, 12f)
            }
            // Meridian curve
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 3f)
                curveTo(8.5f, 6.5f, 6.8f, 17.5f, 12f, 21f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 3f)
                curveTo(15.5f, 6.5f, 17.2f, 17.5f, 12f, 21f)
            }
        }.build()
    }

    /**
     * CSE Stream: Flaticon UIcons Rounded Code / Terminal brackets `< / >`.
     */
    val Terminal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Terminal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Left bracket '<'
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.5f, 7.5f)
                lineTo(3f, 12f)
                lineTo(7.5f, 16.5f)
            }
            // Slash '/'
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14f, 5.5f)
                lineTo(10f, 18.5f)
            }
            // Right bracket '>'
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 7.5f)
                lineTo(21f, 12f)
                lineTo(16.5f, 16.5f)
            }
        }.build()
    }

    /**
     * Electronics / ECE Room: Flaticon UIcons Rounded Microchip / CPU.
     */
    val Cpu: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cpu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Chip body (rounded square)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 5f)
                horizontalLineTo(17f)
                curveTo(18.1f, 5f, 19f, 5.9f, 19f, 7f)
                verticalLineTo(17f)
                curveTo(19f, 18.1f, 18.1f, 19f, 17f, 19f)
                horizontalLineTo(7f)
                curveTo(5.9f, 19f, 5f, 18.1f, 5f, 17f)
                verticalLineTo(7f)
                curveTo(5f, 5.9f, 5.9f, 5f, 7f, 5f)
                close()
            }
            // Inner core square
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.5f, 9.5f)
                horizontalLineTo(14.5f)
                verticalLineTo(14.5f)
                horizontalLineTo(9.5f)
                close()
            }
            // Top pins
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 2f); lineTo(9f, 5f)
                moveTo(15f, 2f); lineTo(15f, 5f)
            }
            // Bottom pins
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 19f); lineTo(9f, 22f)
                moveTo(15f, 19f); lineTo(15f, 22f)
            }
            // Left pins
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2f, 9f); lineTo(5f, 9f)
                moveTo(2f, 15f); lineTo(5f, 15f)
            }
            // Right pins
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(19f, 9f); lineTo(22f, 9f)
                moveTo(19f, 15f); lineTo(22f, 15f)
            }
        }.build()
    }

    /**
     * Mechanical Room: Flaticon UIcons Rounded Engineering Wrench.
     */
    val Wrench: ImageVector by lazy {
        ImageVector.Builder(
            name = "Wrench",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.5f, 6.5f)
                curveTo(13.8f, 5.8f, 13.5f, 4.8f, 13.7f, 3.8f)
                curveTo(14.8f, 2.7f, 16.5f, 2.7f, 17.6f, 3.8f)
                lineTo(19f, 5.2f)
                curveTo(19.8f, 6f, 20f, 7.2f, 19.5f, 8.2f)
                curveTo(20.3f, 9.4f, 20.3f, 11f, 19.3f, 12f)
                lineTo(10.5f, 20.8f)
                curveTo(9.5f, 21.8f, 8f, 21.8f, 7f, 20.8f)
                lineTo(3.2f, 17f)
                curveTo(2.2f, 16f, 2.2f, 14.5f, 3.2f, 13.5f)
                lineTo(12f, 4.7f)
                curveTo(13f, 3.7f, 14.6f, 3.7f, 15.8f, 4.5f)
            }
            // Small ring opening in bottom handle
            path(fill = SolidColor(Color.White)) {
                moveTo(6.5f, 17.5f)
                curveTo(5.95f, 17.5f, 5.5f, 17.95f, 5.5f, 18.5f)
                curveTo(5.5f, 19.05f, 5.95f, 19.5f, 6.5f, 19.5f)
                curveTo(7.05f, 19.5f, 7.5f, 19.05f, 7.5f, 18.5f)
                curveTo(7.5f, 17.95f, 7.05f, 17.5f, 6.5f, 17.5f)
                close()
            }
        }.build()
    }

    /**
     * Civil Room: Flaticon UIcons Rounded Architecture / Building.
     */
    val Building: ImageVector by lazy {
        ImageVector.Builder(
            name = "Building",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer building structure
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 21.5f)
                horizontalLineTo(20f)
                moveTo(6f, 21.5f)
                verticalLineTo(4.5f)
                curveTo(6f, 3.4f, 6.9f, 2.5f, 8f, 2.5f)
                horizontalLineTo(16f)
                curveTo(17.1f, 2.5f, 18f, 3.4f, 18f, 4.5f)
                verticalLineTo(21.5f)
            }
            // Windows grid
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Row 1
                moveTo(9.5f, 6.5f); lineTo(10.5f, 6.5f)
                moveTo(13.5f, 6.5f); lineTo(14.5f, 6.5f)
                // Row 2
                moveTo(9.5f, 10.5f); lineTo(10.5f, 10.5f)
                moveTo(13.5f, 10.5f); lineTo(14.5f, 10.5f)
                // Row 3
                moveTo(9.5f, 14.5f); lineTo(10.5f, 14.5f)
                moveTo(13.5f, 14.5f); lineTo(14.5f, 14.5f)
            }
            // Entrance door
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 21.5f)
                verticalLineTo(18.5f)
                horizontalLineTo(13.5f)
                verticalLineTo(21.5f)
            }
        }.build()
    }

    /**
     * Faculty Room: Flaticon UIcons Rounded Graduation Cap / Education.
     */
    val GraduationCap: ImageVector by lazy {
        ImageVector.Builder(
            name = "GraduationCap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Mortarboard diamond top
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.5f)
                lineTo(22f, 8.5f)
                lineTo(12f, 13.5f)
                lineTo(2f, 8.5f)
                close()
            }
            // Skull cap arc underneath
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 10.8f)
                verticalLineTo(15.5f)
                curveTo(6f, 18.5f, 18f, 18.5f, 18f, 15.5f)
                verticalLineTo(10.8f)
            }
            // Hanging tassel
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20f, 10f)
                verticalLineTo(16.5f)
            }
        }.build()
    }

    /**
     * Hostel Room: Flaticon UIcons Rounded Dormitory / Bed.
     */
    val Bed: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Headboard
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 6.5f)
                verticalLineTo(20.5f)
            }
            // Footboard
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(21f, 11f)
                verticalLineTo(20.5f)
            }
            // Mattress & blanket line
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 16.5f)
                horizontalLineTo(21f)
            }
            // Pillow rounded rectangle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 11f)
                horizontalLineTo(9.5f)
                curveTo(10.3f, 11f, 11f, 11.7f, 11f, 12.5f)
                verticalLineTo(14f)
                horizontalLineTo(5f)
                verticalLineTo(12.5f)
                curveTo(5f, 11.7f, 5.7f, 11f, 6.5f, 11f)
                close()
            }
        }.build()
    }

    // ── Security, Identity & Status Icons ────────────────────────────────────

    /**
     * Flaticon UIcons Rounded Padlock.
     */
    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Shackle arch
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.5f, 10f)
                verticalLineTo(6.5f)
                curveTo(7.5f, 4f, 9.5f, 2.5f, 12f, 2.5f)
                curveTo(14.5f, 2.5f, 16.5f, 4f, 16.5f, 6.5f)
                verticalLineTo(10f)
            }
            // Lock body
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 10f)
                horizontalLineTo(18.5f)
                curveTo(19.6f, 10f, 20.5f, 10.9f, 20.5f, 12f)
                verticalLineTo(19.5f)
                curveTo(20.5f, 20.6f, 19.6f, 21.5f, 18.5f, 21.5f)
                horizontalLineTo(5.5f)
                curveTo(4.4f, 21.5f, 3.5f, 20.6f, 3.5f, 19.5f)
                verticalLineTo(12f)
                curveTo(3.5f, 10.9f, 4.4f, 10f, 5.5f, 10f)
                close()
            }
            // Keyhole
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 14f)
                lineTo(12f, 17.5f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Shield.
     */
    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(4.5f, 5.5f)
                verticalLineTo(11.5f)
                curveTo(4.5f, 16.8f, 7.8f, 21.5f, 12f, 22.5f)
                curveTo(16.2f, 21.5f, 19.5f, 16.8f, 19.5f, 11.5f)
                verticalLineTo(5.5f)
                close()
            }
            // Subtle internal check for security verification
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Sparkles / Magic Stars.
     */
    val Sparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sparkles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Primary sparkle (4-point rounded star)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 2.5f)
                curveTo(10f, 7.5f, 14f, 11.5f, 19f, 11.5f)
                curveTo(14f, 11.5f, 10f, 15.5f, 10f, 20.5f)
                curveTo(10f, 15.5f, 6f, 11.5f, 1f, 11.5f)
                curveTo(6f, 11.5f, 10f, 7.5f, 10f, 2.5f)
                close()
            }
            // Secondary mini sparkle in upper right
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 3f)
                curveTo(18.5f, 5f, 20f, 6.5f, 22f, 6.5f)
                curveTo(20f, 6.5f, 18.5f, 8f, 18.5f, 10f)
                curveTo(18.5f, 8f, 17f, 6.5f, 15f, 6.5f)
                curveTo(17f, 6.5f, 18.5f, 5f, 18.5f, 3f)
                close()
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Radar / Matchmaking Scanner icon.
     * Depicts concentric radar scanning pulses, center blip, and directional sweep beam
     * perfectly tailored for finding random peers online.
     */
    val Radar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Radar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer radar perimeter circle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.5f, 12f)
                arcTo(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2.5f, 12f)
                arcTo(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 21.5f, 12f)
            }
            // Inner concentric range circle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 12f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.5f, 12f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17.5f, 12f)
            }
            // Center scanning radar blip
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(13.5f, 12f)
                arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.5f, 12f)
                arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13.5f, 12f)
            }
            // Radar scanner sweep line from center to upper-right boundary
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12f)
                lineTo(18.7f, 5.3f)
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Animal Paw print.
     */
    val Paw: ImageVector by lazy {
        ImageVector.Builder(
            name = "Paw",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Main center pad
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 13f)
                curveTo(9.6f, 13f, 7.5f, 14.8f, 7.5f, 17.2f)
                curveTo(7.5f, 19.5f, 9.4f, 21.5f, 12f, 21.5f)
                curveTo(14.6f, 21.5f, 16.5f, 19.5f, 16.5f, 17.2f)
                curveTo(16.5f, 14.8f, 14.4f, 13f, 12f, 13f)
                close()
            }
            // Far left toe
            path(fill = SolidColor(Color.White)) {
                moveTo(4.5f, 10.5f)
                curveTo(3.4f, 10.5f, 2.5f, 11.4f, 2.5f, 12.5f)
                curveTo(2.5f, 13.6f, 3.4f, 14.5f, 4.5f, 14.5f)
                curveTo(5.6f, 14.5f, 6.5f, 13.6f, 6.5f, 12.5f)
                curveTo(6.5f, 11.4f, 5.6f, 10.5f, 4.5f, 10.5f)
                close()
            }
            // Mid left toe
            path(fill = SolidColor(Color.White)) {
                moveTo(8.5f, 6.5f)
                curveTo(7.4f, 6.5f, 6.5f, 7.4f, 6.5f, 8.5f)
                curveTo(6.5f, 9.6f, 7.4f, 10.5f, 8.5f, 10.5f)
                curveTo(9.6f, 10.5f, 10.5f, 9.6f, 10.5f, 8.5f)
                curveTo(10.5f, 7.4f, 9.6f, 6.5f, 8.5f, 6.5f)
                close()
            }
            // Mid right toe
            path(fill = SolidColor(Color.White)) {
                moveTo(15.5f, 6.5f)
                curveTo(14.4f, 6.5f, 13.5f, 7.4f, 13.5f, 8.5f)
                curveTo(13.5f, 9.6f, 14.4f, 10.5f, 15.5f, 10.5f)
                curveTo(16.6f, 10.5f, 17.5f, 9.6f, 17.5f, 8.5f)
                curveTo(17.5f, 7.4f, 16.6f, 6.5f, 15.5f, 6.5f)
                close()
            }
            // Far right toe
            path(fill = SolidColor(Color.White)) {
                moveTo(19.5f, 10.5f)
                curveTo(18.4f, 10.5f, 17.5f, 11.4f, 17.5f, 12.5f)
                curveTo(17.5f, 13.6f, 18.4f, 14.5f, 19.5f, 14.5f)
                curveTo(20.6f, 14.5f, 21.5f, 13.6f, 21.5f, 12.5f)
                curveTo(21.5f, 11.4f, 20.6f, 10.5f, 19.5f, 10.5f)
                close()
            }
        }.build()
    }

    /**
     * Flaticon UIcons Rounded Refresh / Sync arrows.
     */
    val Refresh: ImageVector by lazy {
        ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Circular arc
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.5f, 12f)
                curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f)
                curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f)
                curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f)
                curveTo(15.2f, 3.5f, 18f, 5.3f, 19.5f, 8f)
            }
            // Arrow head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 8f)
                lineTo(20f, 8f)
                lineTo(20f, 3.5f)
            }
        }.build()
    }

    val RotateLeft: ImageVector by lazy { Refresh }


    /**
     * Flaticon UIcons Rounded Wifi Offline slash.
     */
    val WifiOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "WifiOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer wifi arc segment
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.5f, 7.5f)
                curveTo(8.5f, 4.5f, 15.5f, 4.5f, 19.5f, 7.5f)
            }
            // Middle wifi arc segment
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.5f, 11.5f)
                curveTo(10f, 9.5f, 14f, 9.5f, 16.5f, 11.5f)
            }
            // Central dot
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 17f)
                curveTo(11.2f, 17f, 10.5f, 17.7f, 10.5f, 18.5f)
                curveTo(10.5f, 19.3f, 11.2f, 20f, 12f, 20f)
                curveTo(12.8f, 20f, 13.5f, 19.3f, 13.5f, 18.5f)
                curveTo(13.5f, 17.7f, 12.8f, 17f, 12f, 17f)
                close()
            }
            // Diagonal strike-through slash
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }.build()
    }

    /**
     * Single message bubble (default/fallback room icon).
     */
    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 11.5f)
                curveTo(20f, 6.8f, 16.4f, 3f, 12f, 3f)
                curveTo(7.6f, 3f, 4f, 6.8f, 4f, 11.5f)
                curveTo(4f, 13.3f, 4.6f, 15f, 5.7f, 16.4f)
                lineTo(4.5f, 20.5f)
                lineTo(8.8f, 19.2f)
                curveTo(9.8f, 19.7f, 10.9f, 20f, 12f, 20f)
                curveTo(16.4f, 20f, 20f, 16.2f, 20f, 11.5f)
                close()
            }
        }.build()
    }

    /**
     * Grid tile icon.
     */
    val Grid: ImageVector by lazy {
        ImageVector.Builder(
            name = "Grid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Top-left
                moveTo(5f, 4f); horizontalLineTo(9.5f); verticalLineTo(8.5f); horizontalLineTo(5f); close()
                // Top-right
                moveTo(14.5f, 4f); horizontalLineTo(19f); verticalLineTo(8.5f); horizontalLineTo(14.5f); close()
                // Bottom-left
                moveTo(5f, 13.5f); horizontalLineTo(9.5f); verticalLineTo(18f); horizontalLineTo(5f); close()
                // Bottom-right
                moveTo(14.5f, 13.5f); horizontalLineTo(19f); verticalLineTo(18f); horizontalLineTo(14.5f); close()
            }
        }.build()
    }

    /**
     * Download / Save icon (downward arrow pointing to tray).
     */
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Downward arrow stem and head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.5f)
                lineTo(12f, 14.5f)
                moveTo(7.5f, 10.5f)
                lineTo(12f, 15f)
                lineTo(16.5f, 10.5f)
            }
            // Base tray
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 15.5f)
                lineTo(4.5f, 19.5f)
                lineTo(19.5f, 19.5f)
                lineTo(19.5f, 15.5f)
            }
        }.build()
    }

    /**
     * Crop and rotate tool icon.
     */
    val CropRotate: ImageVector by lazy {
        ImageVector.Builder(
            name = "CropRotate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Crop square boundary lines
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 3f); lineTo(7f, 17f); lineTo(21f, 17f)
                moveTo(17f, 21f); lineTo(17f, 7f); lineTo(3f, 7f)
            }
            // Rotate circular arrow indication
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.5f, 12f)
                arcTo(8f, 8f, 0f, false, true, 12f, 20f)
            }
        }.build()
    }

    /**
     * Text "Aa" annotation icon.
     */
    val TextAa: ImageVector by lazy {
        ImageVector.Builder(
            name = "TextAa",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Large 'A'
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 18f)
                lineTo(8.5f, 5.5f)
                lineTo(13.5f, 18f)
                moveTo(5.5f, 14f)
                lineTo(11.5f, 14f)
            }
            // Small 'a'
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.5f, 11.5f)
                lineTo(19.5f, 18f)
                moveTo(19.5f, 14.5f)
                arcTo(3f, 3f, 0f, true, true, 16.5f, 11.5f)
                lineTo(19.5f, 11.5f)
            }
        }.build()
    }

    /**
     * Pencil / Doodle drawing tool icon.
     */
    val Pencil: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pencil",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 3f)
                lineTo(21f, 5.5f)
                lineTo(7.5f, 19f)
                lineTo(3f, 21f)
                lineTo(5f, 16.5f)
                close()
                moveTo(16f, 5.5f)
                lineTo(18.5f, 8f)
            }
        }.build()
    }

    /**
     * Photo / Gallery landscape icon.
     */
    val Photo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Photo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Frame
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 5f)
                lineTo(19.5f, 5f)
                arcTo(2f, 2f, 0f, false, true, 21.5f, 7f)
                lineTo(21.5f, 17f)
                arcTo(2f, 2f, 0f, false, true, 19.5f, 19f)
                lineTo(4.5f, 19f)
                arcTo(2f, 2f, 0f, false, true, 2.5f, 17f)
                lineTo(2.5f, 7f)
                arcTo(2f, 2f, 0f, false, true, 4.5f, 5f)
                close()
            }
            // Mountain / sun
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 16f)
                lineTo(8f, 11f)
                lineTo(13f, 16f)
                moveTo(12f, 15f)
                lineTo(15f, 12f)
                lineTo(21f, 18f)
            }
        }.build()
    }

    /**
     * Upward chevron `^` icon.
     */
    val ChevronUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 14.5f)
                lineTo(12f, 9f)
                lineTo(17.5f, 14.5f)
            }
        }.build()
    }

    /**
     * Rotate Clockwise (90 deg CW).
     */
    val RotateCw: ImageVector by lazy {
        ImageVector.Builder(
            name = "RotateCw",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 4f)
                lineTo(21f, 9f)
                lineTo(16f, 9f)
                moveTo(20.5f, 13f)
                arcTo(8.5f, 8.5f, 0f, true, true, 18f, 7.5f)
                lineTo(21f, 9f)
            }
        }.build()
    }

    /**
     * Rotate Counter-Clockwise (90 deg CCW).
     */
    val RotateCcw: ImageVector by lazy {
        ImageVector.Builder(
            name = "RotateCcw",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 4f)
                lineTo(3f, 9f)
                lineTo(8f, 9f)
                moveTo(3.5f, 13f)
                arcTo(8.5f, 8.5f, 0f, true, false, 6f, 7.5f)
                lineTo(3f, 9f)
            }
        }.build()
    }

    /**
     * Undo curved back-arrow.
     */
    val Undo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Undo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 10f)
                lineTo(3f, 5f)
                lineTo(8f, 5f)
                moveTo(3f, 9f)
                arcTo(9f, 9f, 0f, true, true, 5.5f, 16.5f)
            }
        }.build()
    }

    /**
     * Reset / Restore icon.
     */
    val Reset: ImageVector by lazy {
        ImageVector.Builder(
            name = "Reset",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 4f)
                lineTo(1f, 10f)
                lineTo(7f, 10f)
                moveTo(23f, 20f)
                lineTo(23f, 14f)
                lineTo(17f, 14f)
                moveTo(20.5f, 9f)
                arcTo(9f, 9f, 0f, false, false, 3.5f, 9.5f)
                moveTo(3.5f, 15f)
                arcTo(9f, 9f, 0f, false, false, 20.5f, 14.5f)
            }
        }.build()
    }


    /**
     * Flip Horizontal icon matching Screenshot 1: vertical center line with mirrored triangles.
     */
    val FlipHorizontal: ImageVector by lazy {
        ImageVector.Builder(
            name = "FlipHorizontal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Center dividing line
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 3f)
                lineTo(12f, 21f)
            }
            // Left triangle
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(9f, 6f)
                lineTo(3f, 12f)
                lineTo(9f, 18f)
                close()
            }
            // Right triangle
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 6f)
                lineTo(21f, 12f)
                lineTo(15f, 18f)
                close()
            }
        }.build()
    }

    /**
     * Freeform Crop Icon: 4 corner brackets.
     */
    val CropFree: ImageVector by lazy {
        ImageVector.Builder(
            name = "CropFree",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Top-Left
                moveTo(8f, 4f)
                lineTo(4f, 4f)
                lineTo(4f, 8f)
                // Top-Right
                moveTo(16f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 8f)
                // Bottom-Left
                moveTo(4f, 16f)
                lineTo(4f, 20f)
                lineTo(8f, 20f)
                // Bottom-Right
                moveTo(20f, 16f)
                lineTo(20f, 20f)
                lineTo(16f, 20f)
            }
        }.build()
    }

    /**
     * Original Crop Icon: film/photo frame with perforations matching Screenshot 1.
     */
    val CropOriginal: ImageVector by lazy {
        ImageVector.Builder(
            name = "CropOriginal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                arcTo(2f, 2f, 0f, false, true, 22f, 6f)
                lineTo(22f, 18f)
                arcTo(2f, 2f, 0f, false, true, 20f, 20f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, false, true, 2f, 18f)
                lineTo(2f, 6f)
                arcTo(2f, 2f, 0f, false, true, 4f, 4f)
                close()
            }
            // Inner perforations / frame lines
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 8f)
                lineTo(18f, 8f)
                moveTo(6f, 16f)
                lineTo(18f, 16f)
            }
        }.build()
    }

    /**
     * Rotate 90 Crop Icon: rectangle with rotation indicator matching Screenshot 1.
     */
    val Rotate90Crop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Rotate90Crop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 6f)
                lineTo(18f, 6f)
                arcTo(2f, 2f, 0f, false, true, 20f, 8f)
                lineTo(20f, 18f)
                arcTo(2f, 2f, 0f, false, true, 18f, 20f)
                lineTo(8f, 20f)
                arcTo(2f, 2f, 0f, false, true, 6f, 18f)
                lineTo(6f, 8f)
                arcTo(2f, 2f, 0f, false, true, 8f, 6f)
                close()
                // Rotation arrow
                moveTo(4f, 10f)
                arcTo(7f, 7f, 0f, false, true, 12f, 3f)
                lineTo(14f, 3f)
            }
        }.build()
    }

    /**
     * Minimal Hourglass icon for disappearing/ephemeral chat states.
     */
    val Hourglass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Hourglass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 4f)
                lineTo(18f, 4f)
                moveTo(6f, 20f)
                lineTo(18f, 20f)
                moveTo(7f, 4f)
                lineTo(11f, 11f)
                arcTo(1.5f, 1.5f, 0f, false, true, 11f, 13f)
                lineTo(7f, 20f)
                moveTo(17f, 4f)
                lineTo(13f, 11f)
                arcTo(1.5f, 1.5f, 0f, false, false, 13f, 13f)
                lineTo(17f, 20f)
            }
        }.build()
    }

    /**
     * Notification Bell icon.
     */
    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Bell body
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                arcTo(6f, 6f, 0f, false, false, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                lineTo(21f, 17f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                close()
            }
            // Clapper
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.3f, 20f)
                arcTo(2f, 2f, 0f, false, false, 13.7f, 20f)
            }
        }.build()
    }

    /**
     * Plus / Add icon.
     */
    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    /**
     * Broadcast Beacon / Radio Mast icon for Campus Broadcasts.
     */
    val BroadcastBeacon: ImageVector by lazy {
        ImageVector.Builder(
            name = "BroadcastBeacon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                lineTo(12f, 16f)
                moveTo(7f, 22f)
                lineTo(12f, 16f)
                lineTo(17f, 22f)
                moveTo(9.5f, 19f)
                lineTo(14.5f, 19f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8.5f, 5.5f)
                arcTo(5f, 5f, 0f, false, false, 8.5f, 12.5f)
                moveTo(15.5f, 5.5f)
                arcTo(5f, 5f, 0f, false, true, 15.5f, 12.5f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.5f, 3.5f)
                arcTo(9f, 9f, 0f, false, false, 5.5f, 14.5f)
                moveTo(18.5f, 3.5f)
                arcTo(9f, 9f, 0f, false, true, 18.5f, 14.5f)
            }
        }.build()
    }

    /**
     * Megaphone / Announcement icon.
     */
    val Megaphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Megaphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 11f)
                lineTo(3f, 7f)
                arcTo(2f, 2f, 0f, false, true, 5f, 5f)
                lineTo(14f, 2f)
                lineTo(14f, 16f)
                lineTo(5f, 13f)
                arcTo(2f, 2f, 0f, false, true, 3f, 11f)
                close()
                moveTo(14f, 6f)
                lineTo(19f, 4f)
                arcTo(1.5f, 1.5f, 0f, false, true, 21f, 5.5f)
                lineTo(21f, 12.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 19f, 14f)
                lineTo(14f, 12f)
                moveTo(8f, 14f)
                lineTo(8f, 20f)
                lineTo(11f, 20f)
                lineTo(11f, 15f)
            }
        }.build()
    }

    /**
     * Edit / Rename icon (aliases Pencil).
     */
    val Edit: ImageVector by lazy { Pencil }

    /**
     * Group info / About circular icon.
     */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                arcTo(10f, 10f, 0f, true, true, 12f, 2f)
                arcTo(10f, 10f, 0f, false, true, 12f, 22f)
                close()
                moveTo(12f, 16f)
                lineTo(12f, 12f)
                moveTo(12f, 8f)
                lineTo(12.01f, 8f)
            }
        }.build()
    }

    /**
     * Invite link icon.
     */
    val Link: ImageVector by lazy {
        ImageVector.Builder(
            name = "Link",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 13f)
                arcTo(5f, 5f, 0f, false, false, 17f, 13f)
                lineTo(19f, 11f)
                arcTo(5f, 5f, 0f, false, false, 12f, 4f)
                lineTo(10.5f, 5.5f)
                moveTo(14f, 11f)
                arcTo(5f, 5f, 0f, false, false, 7f, 11f)
                lineTo(5f, 13f)
                arcTo(5f, 5f, 0f, false, false, 12f, 20f)
                lineTo(13.5f, 18.5f)
            }
        }.build()
    }

    /**
     * Share sheet / Nodes connection icon.
     */
    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 5f)
                arcTo(3f, 3f, 0f, true, true, 18f, 11f)
                arcTo(3f, 3f, 0f, false, true, 18f, 5f)
                close()
                moveTo(6f, 9f)
                arcTo(3f, 3f, 0f, true, true, 6f, 15f)
                arcTo(3f, 3f, 0f, false, true, 6f, 9f)
                close()
                moveTo(18f, 13f)
                arcTo(3f, 3f, 0f, true, true, 18f, 19f)
                arcTo(3f, 3f, 0f, false, true, 18f, 13f)
                close()
                moveTo(8.59f, 13.51f)
                lineTo(15.42f, 17.49f)
                moveTo(15.41f, 6.51f)
                lineTo(8.59f, 10.49f)
            }
        }.build()
    }

    /**
     * Lightbulb / Suggestion / Idea icon.
     */
    val Lightbulb: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lightbulb",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                lineTo(15f, 18f)
                moveTo(10f, 21f)
                lineTo(14f, 21f)
                moveTo(15.09f, 14f)
                curveTo(16.89f, 12.65f, 18f, 10.47f, 18f, 8f)
                curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
                curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
                curveTo(6f, 10.47f, 7.11f, 12.65f, 8.91f, 14f)
                lineTo(9f, 18f)
                lineTo(15f, 18f)
                lineTo(15.09f, 14f)
                close()
            }
        }.build()
    }
}


