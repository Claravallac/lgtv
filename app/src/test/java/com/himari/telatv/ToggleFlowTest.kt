package com.himari.telatv

import com.himari.telatv.data.Ssap
import com.himari.telatv.data.SpotifyTarget
import com.himari.telatv.data.ToggleFlow
import com.himari.telatv.data.TvSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleFlowTest {
    /** TV falsa que responde na hora e registra a ordem das chamadas. */
    private fun tv(state: String, failLaunch: Boolean, log: MutableList<String>): TvSession {
        lateinit var s: TvSession
        s = TvSession { msg ->
            val id = Ssap.parse(msg).id!!
            val uri = Regex(""""uri":"([^"]*)"""").find(msg)!!.groupValues[1]
            log += uri.substringAfter("ssap://")
            s.onMessage(
                if (uri == Ssap.URI_LAUNCH && failLaunch)
                    """{"type":"error","id":"$id","error":"404 app not found","payload":{}}"""
                else
                    """{"type":"response","id":"$id","payload":{"returnValue":true,"state":"$state"}}"""
            )
            true
        }
        return s
    }

    @Test fun apagaComSpotifyNaTv() {
        val log = mutableListOf<String>()
        val d = ToggleFlow.perform(tv("Active", false, log), SpotifyTarget.TV)
        assertEquals(
            listOf("com.webos.service.tvpower/power/getPowerState", "system.launcher/launch",
                "com.webos.service.tvpower/power/turnOffScreen"), log
        )
        assertTrue(d.plan.turnsOff); assertNull(d.warning)
    }

    @Test fun acendeSemSpotify() {
        val log = mutableListOf<String>()
        val d = ToggleFlow.perform(tv("Screen Off", false, log), SpotifyTarget.TV)
        assertEquals(
            listOf("com.webos.service.tvpower/power/getPowerState", "com.webos.service.tvpower/power/turnOnScreen"), log
        )
        assertTrue(!d.plan.turnsOff)
    }

    @Test fun falhaNoSpotifyNaoImpedeApagar() {
        val log = mutableListOf<String>()
        val d = ToggleFlow.perform(tv("Active", true, log), SpotifyTarget.TV)
        assertTrue(d.warning!!.contains("404"))
        assertEquals("com.webos.service.tvpower/power/turnOffScreen", log.last())
    }

    @Test fun semSpotifyNaoLanca() {
        val log = mutableListOf<String>()
        ToggleFlow.perform(tv("Active", false, log), SpotifyTarget.NONE)
        assertEquals(2, log.size)
    }
}
