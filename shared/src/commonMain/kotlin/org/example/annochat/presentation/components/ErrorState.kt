package org.example.annochat.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import org.example.annochat.presentation.components.AnonChatPrimaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Monochromatic ErrorState component matching the No Internet Connection design.
 */
@Composable
fun ErrorState(
    title: String = "No internet connection!",
    message: String = "We couldn't reach the server.\nPlease check your connection\nand try again.",
    modifier: Modifier = Modifier,
    retryLabel: String = "Try again",
    isLoading: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    MonochromaticNetworkErrorScreen(
        onRetry = { onRetry?.invoke() },
        isRetrying = isLoading,
        title = title,
        message = message,
        modifier = modifier
    )
}

@Composable
private fun LargeWifiGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val originX = w * 0.08f
        val originY = h * 0.48f + 16.dp.toPx()

        val waveBrush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFFE4E4E7),
                Color(0xFFA1A1AA),
                Color(0xFF71717A),
                Color(0xFF3F3F46),
                Color(0xFF18181B),
                Color.Transparent
            ),
            center = Offset(originX, originY),
            radius = w * 1.60f
        )

        drawCircle(
            brush = waveBrush,
            radius = 34.dp.toPx(),
            center = Offset(originX, originY)
        )

        val r1 = 85.dp.toPx()
        val s1 = 34.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -86f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(originX - r1, originY - r1),
            size = Size(r1 * 2, r1 * 2),
            style = Stroke(width = s1, cap = StrokeCap.Round)
        )

        val r2 = 160.dp.toPx()
        val s2 = 40.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -88f,
            sweepAngle = 84f,
            useCenter = false,
            topLeft = Offset(originX - r2, originY - r2),
            size = Size(r2 * 2, r2 * 2),
            style = Stroke(width = s2, cap = StrokeCap.Round)
        )

        val r3 = 245.dp.toPx()
        val s3 = 46.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -90f,
            sweepAngle = 86f,
            useCenter = false,
            topLeft = Offset(originX - r3, originY - r3),
            size = Size(r3 * 2, r3 * 2),
            style = Stroke(width = s3, cap = StrokeCap.Round)
        )

        val r4 = 340.dp.toPx()
        val s4 = 52.dp.toPx()
        drawArc(
            brush = waveBrush,
            startAngle = -90f,
            sweepAngle = 88f,
            useCenter = false,
            topLeft = Offset(originX - r4, originY - r4),
            size = Size(r4 * 2, r4 * 2),
            style = Stroke(width = s4, cap = StrokeCap.Round)
        )
    }
}

