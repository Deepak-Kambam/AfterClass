package org.example.annochat.presentation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
object AnonChatTypography {
    // 29-32sp: Major Hero Titles
    val HeroTitle = TextStyle(
        fontSize = 29.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        lineHeight = 35.sp,
        color = AnonChatColors.TextPrimary
    )

    // 25sp: Screen Headings
    val ScreenTitle = TextStyle(
        fontSize = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
        lineHeight = 31.sp,
        color = AnonChatColors.TextPrimary
    )

    // 19sp: Important Labels & Brand Titles
    val BrandTitle = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = AnonChatColors.TextPrimary
    )

    val Label = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = AnonChatColors.TextPrimary
    )

    // 16sp: Body & Chat Messages
    val Body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 23.sp,
        letterSpacing = 0.1.sp,
        color = AnonChatColors.TextPrimary
    )

    val MessageBody = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 23.sp,
        letterSpacing = 0.1.sp,
        color = AnonChatColors.TextPrimary
    )

    // 15sp: Secondary Content & Action Buttons
    val BodySecondary = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        color = AnonChatColors.TextSecondary
    )

    val RoomTitle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = AnonChatColors.TextPrimary
    )

    val RoomMeta = TextStyle(
        fontSize = 14.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        color = AnonChatColors.TextSecondary
    )

    val Button = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        color = AnonChatColors.OnPrimary
    )

    // 12-13sp: Metadata, Section Headers, Badges, Timestamps
    val SectionHeader = TextStyle(
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = AnonChatColors.TextMuted
    )

    val Caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
        color = AnonChatColors.TextSecondary
    )

    val MessageAuthor = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        color = AnonChatColors.Primary
    )

    val MessageAuthorOwn = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        color = AnonChatColors.PrimaryVariant
    )

    val Timestamp = TextStyle(
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Normal,
        color = AnonChatColors.TextMuted
    )

    val Badge = TextStyle(
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        color = AnonChatColors.TextSecondary
    )

    val Code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        color = AnonChatColors.TextMuted
    )
}
