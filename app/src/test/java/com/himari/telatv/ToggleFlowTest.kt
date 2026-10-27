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

    /** TV falsa que responde erro para as URIs em [errors] (uri -> texto do erro). */
    private fun tvErrors(state: String, errors: Map<String, String>, log: MutableList<String>): TvSession {
        lateinit var s: TvSession
        s = TvSession { msg ->
            val id = Ssap.parse(msg).id!!
            val uri = Regex(""""uri":"([^"]*)"""").find(msg)!!.groupValues[1]
            log += uri.substringAfter("ssap://")
            val err = errors[uri]
            s.onMessage(
                if (err != null) """{"type":"error","id":"$id","error":"$err","payload":{}}"""
                else """{"type":"response","id":"$id","payload":{"returnValue":true,"state":"$state"}}"""
            )
            true
        }
        return s
    }

    @Test fun apagaPeloServicoAlternativoQuandoTvpowerNega() {
        val log = mutableListOf<String>()
        val s = tvErrors("Active", mapOf(Ssap.URI_SCREEN_OFF to "401 insufficient permissions"), log)
        val d = ToggleFlow.perform(s, SpotifyTarget.NONE)
        assertEquals(
            listOf("com.webos.service.tvpower/power/getPowerState", "com.webos.service.tvpower/power/turnOffScreen",
                "com.webos.service.tv.power/turnOffScreen"), log
        )
        assertTrue(d.plan.turnsOff); assertTrue(d.usedAlt)
    }

    @Test fun acendeIgnorandoErro0005DoServicoAlternativo() {
        val log = mutableListOf<String>()
        val s = tvErrors(
            "Screen Off",
            mapOf(Ssap.URI_SCREEN_ON to "500 -102", Ssap.URI_ALT_SCREEN_ON to "500 POWER_ERROR_0005 UnKnown Error"),
            log
        )
        val d = ToggleFlow.perform(s, SpotifyTarget.NONE)
        assertTrue(!d.plan.turnsOff); assertTrue(d.usedAlt)
    }

    @Test fun seAsDuasFalhamMostraAsDuasMensagens() {
        val s = tvErrors(
            "Active",
            mapOf(Ssap.URI_SCREEN_OFF to "401 insufficient permissions", Ssap.URI_ALT_SCREEN_OFF to "404 no such service"),
            mutableListOf()
        )
        val msg = try { ToggleFlow.perform(s, SpotifyTarget.NONE); "" } catch (e: com.himari.telatv.data.TvException) { e.message!! }
        assertTrue(msg.contains("401")); assertTrue(msg.contains("Alternativa")); assertTrue(msg.contains("404"))
    }

    @Test fun estadoAtivoMasLembradoComoApagadoAcende() {
        val log = mutableListOf<String>()
        val d = ToggleFlow.perform(tv("Active", false, log), SpotifyTarget.TV, assumedOff = true)
        assertTrue(!d.plan.turnsOff)
        assertEquals("com.webos.service.tvpower/power/turnOnScreen", log.last())
    }

    @Test fun semEstadoDeEnergiaAindaTentaApagar() {
        val log = mutableListOf<String>()
        val s = tvErrors("Active", mapOf(Ssap.URI_POWER_STATE to "404 no such service or method"), log)
        val d = ToggleFlow.perform(s, SpotifyTarget.NONE)
        assertTrue(d.plan.turnsOff)
        assertEquals("com.webos.service.tvpower/power/turnOffScreen", log.last())
    }
}
