package com.kirdev.pulsedeckmobile

import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.model.MediaSession
import com.kirdev.pulsedeckmobile.data.model.PairedPc
import com.kirdev.pulsedeckmobile.data.model.PlayStatus
import com.kirdev.pulsedeckmobile.data.remote.Protocol
import com.kirdev.pulsedeckmobile.feature.monitoring.Names
import com.kirdev.pulsedeckmobile.media.PlayerState
import com.kirdev.pulsedeckmobile.media.hasTrack
import com.kirdev.pulsedeckmobile.media.playerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {
    private val pc = PairedPc.of("PC", "192.168.1.5", 8767, "AA".repeat(32))
    private val connected = ConnectionState.Connected(pc, "PC")
    private fun track(status: PlayStatus) = MediaSession("T", "A", "", status, 10.0, 200.0, "Spotify", "", true, true, 0)

    @Test fun playingAndPausedHaveATrack() {
        assertEquals(PlayerState.PLAYING, playerState(connected, track(PlayStatus.PLAYING)))
        assertEquals(PlayerState.PAUSED, playerState(connected, track(PlayStatus.PAUSED)))
        assertTrue(PlayerState.PLAYING.hasTrack)
        assertTrue(PlayerState.PAUSED.hasTrack)
    }

    @Test fun nothingPlayingMeansNoCardAndNoService() {
        assertEquals(PlayerState.IDLE, playerState(connected, null))
        assertFalse(PlayerState.IDLE.hasTrack)
    }

    @Test fun lostLinkHidesEvenAKnownTrack() {
        for (state in listOf(ConnectionState.Connecting(pc), ConnectionState.Reconnecting(pc, 5, "x"), ConnectionState.NotPaired)) {
            assertEquals(PlayerState.DISCONNECTED, playerState(state, track(PlayStatus.PLAYING)))
        }
        assertFalse(PlayerState.DISCONNECTED.hasTrack)
    }

    @Test fun pausedPositionDoesNotMove() {
        assertEquals(10.0, track(PlayStatus.PAUSED).positionAt(60_000), 0.0)
        assertEquals(70.0, track(PlayStatus.PLAYING).positionAt(60_000), 0.0)
    }

    @Test fun stoppedPlayerFromThePcIsIdle() {
        val message = Protocol.parse("""{"type":"media","session":{"title":"T","status":"stopped"}}""", 0)
        assertNull((message as Protocol.Message.MediaUpdate).session)
    }
}

class NewFieldsTest {
    @Test fun perCoreAndUptimeAreParsed() {
        val m = (Protocol.parse("""{"type":"metrics","cpu":{"load":10,"cores":2,"per_core":[5.5,14.5]},"uptime":3600,
            "gpu":{},"ram":{},"net":{},"disks":[]}""", 0) as Protocol.Message.MetricsUpdate).metrics
        assertEquals(listOf(5.5, 14.5), m.perCore)
        assertEquals(2, m.cpuCores)
        assertEquals(3600L, m.uptime)
    }

    @Test fun olderPcWithoutNewFieldsGivesUnknownNotZero() {
        val m = (Protocol.parse("""{"type":"metrics","cpu":{"load":10},"gpu":{},"ram":{},"net":{},"disks":[]}""", 0)
            as Protocol.Message.MetricsUpdate).metrics
        assertTrue(m.perCore.isEmpty())
        assertNull(m.uptime)
        assertNull(m.cpuCores)
    }

    @Test fun formats() {
        assertEquals("3 д 4 ч", Format.uptime(3 * 86_400L + 4 * 3600))
        assertEquals("5 ч 12 мин", Format.uptime(5 * 3600L + 12 * 60))
        assertEquals("—", Format.uptime(null))
        assertEquals("1,5 с", Format.seconds(1.5))
        assertEquals("3 с", Format.seconds(3.0))
    }

    @Test fun hardwareNames() {
        assertEquals("Intel Core i5-13420H", Names.cpu("13th Gen Intel(R) Core(TM) i5-13420H"))
        assertEquals("RTX 3050", Names.gpu("NVIDIA GeForce RTX 3050 Laptop GPU"))
    }

    @Test fun onlyPlaybackCommandsGoOverTheSocket() {
        try {
            Protocol.command("shutdown")
            throw AssertionError("must refuse")
        } catch (_: IllegalArgumentException) {
        }
    }
}

class WindowOrderTest {
    private fun w(h: Long) = com.kirdev.pulsedeckmobile.data.remote.PcWindow(h, 1, "w$h")

    @Test fun rowsStayPutAfterAnAction() {
        val result = com.kirdev.pulsedeckmobile.feature.remote.stableOrder(listOf(w(1), w(2), w(3)), listOf(w(3), w(4), w(1)))
        assertEquals(listOf(1L, 3L, 4L), result.map { it.hwnd })     // 2 closed, 4 new at the end
    }
}
