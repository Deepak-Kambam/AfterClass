package org.example.annochat.presentation.navigation

import org.example.annochat.data.model.PublicRoomDto

/**
 * Process-scoped singleton that bridges the enter and exit radial expansion animations
 * across screen transitions.
 *
 * When the user completes the slider gesture:
 *   1. RoomsScreen stores [lastEnteredRoom] here before calling onRoomClick
 *   2. App.kt navigates to ChatScreen with an instant (0ms) transition
 *
 * When the user presses Back from ChatScreen:
 *   1. App.kt navigates back to RoomsScreen with an instant (0ms) transition
 *   2. RoomsScreen reads [lastEnteredRoom] and plays the reverse radial contraction
 *   3. After contraction completes, [lastEnteredRoom] is cleared
 */
object RoomTransitionState {
    /** The room whose slider was just completed — drives both enter and exit animations. */
    var lastEnteredRoom: PublicRoomDto? = null

    /** True while the reverse (exit) animation is running in RoomsScreen. */
    var isPlayingExitAnimation: Boolean = false
}
