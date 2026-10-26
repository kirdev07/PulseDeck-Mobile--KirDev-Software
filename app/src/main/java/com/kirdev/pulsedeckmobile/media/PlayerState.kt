package com.kirdev.pulsedeckmobile.media

import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.model.MediaSession
import com.kirdev.pulsedeckmobile.data.model.PlayStatus

/**
 * What the music module does in each state (see docs/PULSEDESK_ARCHITECTURE.md):
 *
 * - PLAYING: mini player, media card, progress ticks locally, wave moves, phone volume keys → PC.
 * - PAUSED: same card with «paused»; no progress ticks, no wave, keys stay on the phone.
 * - IDLE: nothing plays on the PC (the PC sends `session: null` for stopped players too):
 *   mini player hidden, media card and session removed, service stopped.
 * - DISCONNECTED: no link to the PC: everything removed, no commands sent.
 *
 * There is no polling anywhere: the PC pushes `media` over the WebSocket when it changes.
 */
enum class PlayerState { PLAYING, PAUSED, IDLE, DISCONNECTED }

fun playerState(connection: ConnectionState, session: MediaSession?): PlayerState = when {
    connection !is ConnectionState.Connected -> PlayerState.DISCONNECTED
    session == null -> PlayerState.IDLE
    session.status == PlayStatus.PLAYING -> PlayerState.PLAYING
    else -> PlayerState.PAUSED
}

/** A media card and session exist only for a track: playing or paused. */
val PlayerState.hasTrack: Boolean get() = this == PlayerState.PLAYING || this == PlayerState.PAUSED
