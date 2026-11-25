package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Standardized Medium Top App Bar across the app adhering to
 * Material 3's MediumTopAppBar specifications and strict monochromatic palette.
 * Provides generous breathing room, status bar clearance, and balanced spacing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnonChatMediumTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AnonChatColors.SurfacePrimary)
            .statusBarsPadding()
    ) {
        MediumTopAppBar(
            title = {
                Column(
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                        color = AnonChatColors.TextPrimary,
                        maxLines = 1
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = AnonChatTypography.Caption,
                            color = AnonChatColors.TextSecondary,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                    }
                }
            },
            navigationIcon = navigationIcon,
            actions = {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AnonChatColors.SurfacePrimary,
                scrolledContainerColor = AnonChatColors.SurfacePrimary,
                navigationIconContentColor = AnonChatColors.TextPrimary,
                titleContentColor = AnonChatColors.TextPrimary,
                actionIconContentColor = AnonChatColors.TextPrimary
            ),
            scrollBehavior = scrollBehavior
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AnonChatColors.Divider)
        )
    }
}
