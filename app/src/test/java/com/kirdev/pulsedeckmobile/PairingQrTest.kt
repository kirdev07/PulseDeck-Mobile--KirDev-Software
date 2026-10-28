package com.kirdev.pulsedeckmobile

import com.kirdev.pulsedeckmobile.feature.connection.PairingQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingQrTest {
    private val fp = "A".repeat(64)

    @Test fun parsesPcQr() {
        // Exactly what pulse_pc/services/local_api/pairing_qr.payload() produces.
        val qr = PairingQr.parse("pulsedeck://pair?v=1&name=DESKTOP-1&hosts=192.168.10.5%2C10.0.0.2&port=8767&fp=$fp&code=123456")!!
        assertEquals("DESKTOP-1", qr.name)
        assertEquals(listOf("192.168.10.5", "10.0.0.2"), qr.hosts)
        assertEquals(8767, qr.port)
        assertEquals("123456", qr.code)
    }

    @Test fun rejectsForeignOrBroken() {
        assertNull(PairingQr.parse("https://example.com"))
        assertNull(PairingQr.parse("pulsedeck://pair?hosts=192.168.1.2&fp=short&code=123456"))
        assertNull(PairingQr.parse("pulsedeck://pair?hosts=192.168.1.2&fp=$fp&code=12ab56"))
        assertNull(PairingQr.parse(null))
    }
}
