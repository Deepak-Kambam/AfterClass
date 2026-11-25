package org.example.annochat.presentation.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AnonChatShapes {
    // Generous, organic rounded geometry for modern mobile aesthetics
    val radiusSmall = 10.dp
    val radiusMedium = 16.dp
    val radiusCard = 20.dp
    val radiusContainer = 24.dp
    val radiusHero = 28.dp
    val radiusSheet = 32.dp

    val badge = RoundedCornerShape(radiusSmall)
    val small = RoundedCornerShape(radiusSmall)
    val medium = RoundedCornerShape(radiusMedium)
    val card = RoundedCornerShape(radiusCard)
    val container = RoundedCornerShape(radiusContainer)
    val large = RoundedCornerShape(radiusHero)
    val sheet = RoundedCornerShape(topStart = radiusSheet, topEnd = radiusSheet)
    val composer = RoundedCornerShape(radiusHero)
    val pill = RoundedCornerShape(percent = 50) // True stadium pill shape

    // Asymmetric bubble corners for modern messaging
    val bubbleOwn = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    val bubbleOwnContinuation = RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomStart = 18.dp, bottomEnd = 8.dp)

    val bubblePeer = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    val bubblePeerContinuation = RoundedCornerShape(topStart = 8.dp, topEnd = 18.dp, bottomStart = 8.dp, bottomEnd = 18.dp)
}
