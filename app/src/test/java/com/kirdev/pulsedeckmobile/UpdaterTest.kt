package com.kirdev.pulsedeckmobile

import com.kirdev.pulsedeckmobile.data.update.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test fun versions() {
        assertTrue(Updater.isNewer("1.0.1", "1.0"))
        assertTrue(Updater.isNewer("1.0.10", "1.0.9"))
        assertFalse(Updater.isNewer("1.0", "1.0.0"))
        assertFalse(Updater.isNewer("0.9", "1.0"))
    }

    @Test fun parsesReleaseWithApk() {
        val json = """{"tag_name":"v1.0.1","body":"## Что нового\n- **Обновления** с [GitHub](https://github.com)",
            "assets":[{"name":"PulseDesk-1.0.1.apk","browser_download_url":"https://x/a.apk","size":5}]}"""
        val release = Updater.parse(json)!!
        assertEquals("1.0.1", release.version)
        assertEquals("Что нового\n• Обновления с GitHub", release.notes)
        assertEquals(5L, release.size)
    }

    @Test fun skipsPrereleaseAndNoApk() {
        assertNull(Updater.parse("""{"tag_name":"v2","prerelease":true,"assets":[{"name":"a.apk"}]}"""))
        assertNull(Updater.parse("""{"tag_name":"v2","assets":[{"name":"src.zip"}]}"""))
    }
}
