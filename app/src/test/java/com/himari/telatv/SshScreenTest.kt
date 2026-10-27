package com.himari.telatv

import com.himari.telatv.data.SshScreen
import com.himari.telatv.data.TvException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SshScreenTest {
    private val ok = """{ "returnValue": true }"""
    private fun state(s: String) = """{ "returnValue": true, "state": "$s" }"""

    @Test fun comandoLunaSend() {
        assertEquals(
            "luna-send -n 1 luna://com.webos.service.tvpower/power/turnOnScreen '{}'",
            SshScreen.lunaCommand("turnOnScreen")
        )
    }

    @Test fun lerEstado() {
        val s = SshScreen({ state("Active") })
        assertEquals("Active", s.powerState())
    }

    @Test fun apagaEEstadoRefleteATela() {
        val log = mutableListOf<String>()
        val s = SshScreen({ cmd -> log += cmd; if (cmd.contains("getPowerState")) state("Screen Off") else ok })
        assertEquals(false, s.setScreen(true))   // estado confiável
        assertTrue(log[0].contains("turnOffScreen '{}'"))
    }

    @Test fun apagaEEstadoContinuaAtivo() {
        val s = SshScreen({ cmd -> if (cmd.contains("getPowerState")) state("Active") else ok })
        assertEquals(true, s.setScreen(true))    // estado não reflete: lembrar que apagamos
    }

    @Test fun apagarTentaComStandbyModeSeOPrimeiroFalha() {
        val log = mutableListOf<String>()
        val s = SshScreen({ cmd ->
            log += cmd
            when {
                cmd.contains("getPowerState") -> state("Screen Off")
                cmd.contains("standbyMode") -> ok
                else -> """{ "returnValue": false, "errorText": "Invalid standbyMode" }"""
            }
        })
        s.setScreen(true)
        assertTrue(log.any { it.contains("standbyMode") })
    }

    @Test fun acender() {
        val log = mutableListOf<String>()
        val s = SshScreen({ cmd -> log += cmd; ok })
        assertEquals(false, s.setScreen(false))
        assertEquals(1, log.size)
        assertTrue(log[0].contains("turnOnScreen"))
    }

    @Test fun recusaViraErro() {
        val s = SshScreen({ "sh: luna-send: not found" })
        val msg = try { s.setScreen(false); "" } catch (e: TvException) { e.message!! }
        assertTrue(msg.contains("recusou")); assertTrue(msg.contains("not found"))
    }
}
