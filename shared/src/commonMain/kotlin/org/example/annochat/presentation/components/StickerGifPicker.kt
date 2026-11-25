package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.data.model.GifItemDto
import org.example.annochat.data.model.StickerItemDto
import org.example.annochat.data.model.StickersAndGifsResponse
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

data class CuratedSticker(
    val id: String,
    val name: String,
    val emojiOrVisual: String,
    val category: String,
    val path: String
)

data class CuratedGif(
    val id: String,
    val name: String,
    val visual: String,
    val path: String
)

private val BUILTIN_STICKERS = listOf(
    // Animals
    CuratedSticker("s_fox", "Fox", "🦊", "Animals", "sticker_fox"),
    CuratedSticker("s_wolf", "Wolf", "🐺", "Animals", "sticker_wolf"),
    CuratedSticker("s_panda", "Panda", "🐼", "Animals", "sticker_panda"),
    CuratedSticker("s_lion", "Lion", "🦁", "Animals", "sticker_lion"),
    CuratedSticker("s_tiger", "Tiger", "🐯", "Animals", "sticker_tiger"),
    CuratedSticker("s_koala", "Koala", "🐨", "Animals", "sticker_koala"),
    CuratedSticker("s_raccoon", "Raccoon", "🦝", "Animals", "sticker_raccoon"),
    CuratedSticker("s_bear", "Bear", "🐻", "Animals", "sticker_bear"),
    CuratedSticker("s_rabbit", "Rabbit", "🐰", "Animals", "sticker_rabbit"),
    CuratedSticker("s_frog", "Frog", "🐸", "Animals", "sticker_frog"),
    CuratedSticker("s_penguin", "Penguin", "🐧", "Animals", "sticker_penguin"),
    CuratedSticker("s_owl", "Owl", "🦉", "Animals", "sticker_owl"),
    CuratedSticker("s_llama", "Llama", "🦙", "Animals", "sticker_llama"),
    CuratedSticker("s_unicorn", "Unicorn", "🦄", "Animals", "sticker_unicorn"),
    CuratedSticker("s_dragon", "Dragon", "🐲", "Animals", "sticker_dragon"),
    CuratedSticker("s_dolphin", "Dolphin", "🐬", "Animals", "sticker_dolphin"),

    // Reactions
    CuratedSticker("s_fire", "Fire", "🔥", "Reactions", "sticker_fire"),
    CuratedSticker("s_rocket", "Rocket", "🚀", "Reactions", "sticker_rocket"),
    CuratedSticker("s_100", "Hundred", "💯", "Reactions", "sticker_100"),
    CuratedSticker("s_sparkles", "Sparkles", "✨", "Reactions", "sticker_sparkles"),
    CuratedSticker("s_party", "Party", "🎉", "Reactions", "sticker_party"),
    CuratedSticker("s_boom", "Boom", "💥", "Reactions", "sticker_boom"),
    CuratedSticker("s_claps", "Claps", "👏", "Reactions", "sticker_claps"),
    CuratedSticker("s_chill", "Chill", "🤙", "Reactions", "sticker_chill"),
    CuratedSticker("s_skull", "Skull", "💀", "Reactions", "sticker_skull"),
    CuratedSticker("s_bot", "Bot", "🤖", "Reactions", "sticker_bot"),
    CuratedSticker("s_cool", "Cool", "🕶️", "Reactions", "sticker_cool"),
    CuratedSticker("s_bolt", "Bolt", "⚡", "Reactions", "sticker_bolt"),
    CuratedSticker("s_heart", "Heart", "🖤", "Reactions", "sticker_heart"),
    CuratedSticker("s_target", "Target", "🎯", "Reactions", "sticker_target"),
    CuratedSticker("s_coffee", "Coffee", "☕", "Reactions", "sticker_coffee"),
    CuratedSticker("s_shield", "Shield", "🛡️", "Reactions", "sticker_shield")
)

private val BUILTIN_GIFS = listOf(
    CuratedGif("g_mindblown", "Mind Blown", "🤯", "gif_mindblown"),
    CuratedGif("g_hacker", "Hacker Mode", "💻", "gif_hacker"),
    CuratedGif("g_catvibes", "Cat Vibing", "🐱", "gif_catvibes"),
    CuratedGif("g_dancing", "Dance Move", "🕺", "gif_dancing"),
    CuratedGif("g_cheers", "Cheers", "🥂", "gif_cheers"),
    CuratedGif("g_facepalm", "Facepalm", "🤦", "gif_facepalm"),
    CuratedGif("g_rollsafe", "Smart Think", "🧠", "gif_rollsafe"),
    CuratedGif("g_popcorn", "Watching Drama", "🍿", "gif_popcorn"),
    CuratedGif("g_loading", "Buffering...", "⏳", "gif_loading"),
    CuratedGif("g_micdrop", "Mic Drop", "🎤", "gif_micdrop"),
    CuratedGif("g_wave", "Bye Wave", "👋", "gif_wave"),
    CuratedGif("g_celebrate", "Victory", "🏆", "gif_celebrate")
)

@Composable
fun StickerGifPicker(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectMedia: (imagePath: String, imageName: String, isGif: Boolean) -> Unit,
    onLoadRemoteMedia: ((StickersAndGifsResponse?) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var selectedTab by remember { mutableStateOf(0) } // 0 = Stickers, 1 = GIFs
    var selectedCategory by remember { mutableStateOf("All") }
    var remoteData by remember { mutableStateOf<StickersAndGifsResponse?>(null) }
    var isLoadingRemote by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        onLoadRemoteMedia { response ->
            remoteData = response
            isLoadingRemote = false
        }
    }

    val categories = remember { listOf("All", "Animals", "Reactions") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(AnonChatColors.SurfacePrimary)
            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        // Drag / Sheet Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(AnonChatColors.BorderHighlight)
        )

        Spacer(Modifier.height(10.dp))

        // Top Controls: Tabs & Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AnonChatSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Segmented Pill Tabs: Stickers / GIFs
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AnonChatColors.SurfaceSecondary)
                    .border(1.dp, AnonChatColors.Border, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Stickers", "GIFs").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AnonChatColors.Primary else AnonChatColors.SurfaceSecondary)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AnonChatColors.OnPrimary else AnonChatColors.TextSecondary
                        )
                    }
                }
            }

            // Close button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceSecondary)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AnonChatIcons.Close,
                    contentDescription = "Close sticker picker",
                    tint = AnonChatColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Category pills for Stickers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AnonChatSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCatSelected) AnonChatColors.SurfaceTertiary else AnonChatColors.SurfaceSecondary)
                            .border(
                                1.dp,
                                if (isCatSelected) AnonChatColors.Primary else AnonChatColors.Border,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.5.sp,
                            fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCatSelected) AnonChatColors.TextPrimary else AnonChatColors.TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sticker Grid
            val filteredStickers = remember(selectedCategory, remoteData) {
                val list = mutableListOf<CuratedSticker>()
                // Add built-ins
                list.addAll(
                    if (selectedCategory == "All") BUILTIN_STICKERS
                    else BUILTIN_STICKERS.filter { it.category.equals(selectedCategory, ignoreCase = true) }
                )
                // Add remote stickers if matching category
                remoteData?.stickers?.forEach { remote ->
                    val cat = remote.category ?: "Animals"
                    if (selectedCategory == "All" || cat.equals(selectedCategory, ignoreCase = true)) {
                        list.add(
                            CuratedSticker(
                                id = remote.path,
                                name = remote.name,
                                emojiOrVisual = "🎨",
                                category = cat,
                                path = remote.path
                            )
                        )
                    }
                }
                list
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(horizontal = AnonChatSpacing.md),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStickers, key = { it.id }) { sticker ->
                    StickerItemCard(
                        sticker = sticker,
                        onClick = {
                            onSelectMedia(sticker.path, sticker.name, false)
                            onDismiss()
                        }
                    )
                }
            }
        } else {
            // GIFs Grid
            val gifList = remember(remoteData) {
                val list = mutableListOf<CuratedGif>()
                list.addAll(BUILTIN_GIFS)
                remoteData?.gifs?.forEach { remote ->
                    list.add(
                        CuratedGif(
                            id = remote.path,
                            name = remote.name,
                            visual = "🎬",
                            path = remote.path
                        )
                    )
                }
                list
            }

            Spacer(Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = AnonChatSpacing.md),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(gifList, key = { it.id }) { gif ->
                    GifItemCard(
                        gif = gif,
                        onClick = {
                            onSelectMedia(gif.path, gif.name, true)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerItemCard(
    sticker: CuratedSticker,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = sticker.emojiOrVisual,
                fontSize = 30.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sticker.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AnonChatColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GifItemCard(
    gif: CuratedGif,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceTertiary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gif.visual,
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AnonChatColors.Primary)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "GIF",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnonChatColors.OnPrimary
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = gif.name,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AnonChatColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
