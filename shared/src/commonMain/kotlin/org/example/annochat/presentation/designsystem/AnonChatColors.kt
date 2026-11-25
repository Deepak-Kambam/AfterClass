package org.example.annochat.presentation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * AnonChat Professional Black & White Design System
 *
 * Strictly monochromatic palette — no hues, no tints, no colour.
 *
 * Canvas:   #0A0A0A  (near-black background)
 * Surfaces: #111111 → #1A1A1A → #222222  (dark grey hierarchy)
 * Borders:  #2C2C2C → #3A3A3A            (subtle to visible)
 * Text:     #FFFFFF → #B0B0B0 → #666666  (white → mid → muted)
 * Accent:   #FFFFFF / #F0F0F0             (pure white only)
 * Semantic: greyscale analogues           (no red/green/amber)
 */
@Immutable
object AnonChatColors {
    // ──────────────────────────────────────────────
    // Palette Constants  (Monochrome Only)
    // ──────────────────────────────────────────────
    val PaletteWhite      = Color(0xFFFFFFFF)
    val PaletteLightGrey  = Color(0xFFB0B0B0)
    val PaletteMidGrey    = Color(0xFF666666)
    val PaletteDarkGrey   = Color(0xFF333333)
    val PaletteDeepGrey   = Color(0xFF1A1A1A)
    val PaletteBlack      = Color(0xFF0A0A0A)

    // ──────────────────────────────────────────────
    // Base Backgrounds  (Transparent to reveal global bg)
    // ──────────────────────────────────────────────
    val BaseBackground    = Color.Transparent    // Let global space bg show through
    val BaseBackgroundAlt = Color.Transparent    // Let global space bg show through

    // ──────────────────────────────────────────────
    // Solid Surface Hierarchy
    // ──────────────────────────────────────────────
    val SurfacePrimary    = Color(0xFF111111) // Solid dark surface (not frosted/translucent)
    val SurfaceSecondary  = Color(0xFF1A1A1A)    // Cards, item rows, composer input
    val SurfaceTertiary   = Color(0xFF222222)    // Pressed / elevated containers

    // Compatibility aliases
    val Background        = BaseBackground
    val Surface           = SurfacePrimary
    val SurfaceElevated   = SurfaceSecondary
    val SurfaceInteractive = SurfaceTertiary

    // ──────────────────────────────────────────────
    // Brand & Accent Tokens  (White only)
    // ──────────────────────────────────────────────
    val Primary           = Color(0xFFFFFFFF)    // Pure white — the sole accent
    val PrimaryVariant    = Color(0xFFE0E0E0)    // Slightly dimmed white
    val Secondary         = Color(0xFFB0B0B0)    // Mid-grey secondary accent
    val AccentCyan        = Color(0xFFD0D0D0)    // Light grey  (replaces teal)
    val PrimaryDark       = Color(0xFF888888)    // Dark-mode subdued primary
    val OnPrimary         = Color(0xFF0A0A0A)    // Black text on white surface
    val OnPrimaryText     = Color(0xFFFFFFFF)    // White text on dark surface

    // ──────────────────────────────────────────────
    // Text Hierarchy
    // ──────────────────────────────────────────────
    val TextPrimary       = Color(0xFFFFFFFF)    // Pure white — headlines, body
    val TextSecondary     = Color(0xFFB0B0B0)    // Mid grey — subtitles, hints
    val TextMuted         = Color(0xFF666666)    // Muted — timestamps, disabled
    val TextDisabled      = Color(0xFF444444)    // Fully disabled text

    // ──────────────────────────────────────────────
    // Semantic Status Colors  (Greyscale analogues)
    // ──────────────────────────────────────────────
    val Success           = Color(0xFFDDDDDD)    // Light grey — positive / online
    val Warning           = Color(0xFFAAAAAA)    // Mid grey — caution
    val Error             = Color(0xFF888888)    // Darker grey — error / offline
    val Offline           = Color(0xFF555555)    // Deep muted grey

    // ──────────────────────────────────────────────
    // Solid Semantic Badge / Tinted Containers
    // (Solid — NEVER transparent alpha!)
    // ──────────────────────────────────────────────
    val BadgePrimaryBg    = Color(0xFF1A1A1A)
    val BadgePrimaryBorder = Color(0xFF555555)

    val BadgeSuccessBg    = Color(0xFF1A1A1A)
    val BadgeSuccessBorder = Color(0xFF888888)

    val BadgeWarningBg    = Color(0xFF1A1A1A)
    val BadgeWarningBorder = Color(0xFF666666)

    val BadgeErrorBg      = Color(0xFF1A1A1A)
    val BadgeErrorBorder  = Color(0xFF777777)

    // ──────────────────────────────────────────────
    // Solid Borders / Dividers
    // ──────────────────────────────────────────────
    val Border            = Color(0xFF2C2C2C)    // Card / container borders
    val BorderSubtle      = Color(0xFF1E1E1E)    // Hairline separator
    val BorderHighlight   = Color(0xFFFFFFFF)    // Focused / active border (white)
    val Divider           = Color(0xFF181818)    // List dividers

    // Backward compatibility aliases
    val GlassSurface         = SurfacePrimary
    val GlassSurfaceLight    = SurfaceSecondary
    val GlassSurfaceSubtle   = SurfacePrimary
    val GlassBorder          = Border
    val GlassBorderSubtle    = BorderSubtle
    val GlassBorderHighlight = BorderHighlight

    // ──────────────────────────────────────────────
    // Chat Message Bubbles
    // ──────────────────────────────────────────────
    val BubbleOwn         = Color(0xFF2A2A2A)    // Own: light dark grey bubble
    val BubbleOwnBorder   = Color(0xFF555555)    // Own border — visible grey ring
    val BubblePeer        = Color(0xFF111111)    // Peer: near-black bubble
    val BubblePeerBorder  = Color(0xFF2C2C2C)    // Peer border — subtle

    // ──────────────────────────────────────────────
    // Skeleton Shimmer
    // ──────────────────────────────────────────────
    val SkeletonBase      = Color(0xFF1A1A1A)    // Resting skeleton colour
    val SkeletonHighlight = Color(0xFF2E2E2E)    // Pulse highlight
}
