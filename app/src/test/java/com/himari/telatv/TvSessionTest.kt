package com.himari.telatv

import com.himari.telatv.data.SpotifyTarget
import com.himari.telatv.data.TvException
import com.himari.telatv.data.TvSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.concurrent.thread

class TvSessionTest {
    /** TV falsa: ao receber uma mensagem, responde (em outra thread) com as linhas dadas. */
    private fun fake(vararg replies: String, delayMs: Long = 30): TvSession {
        lateinit var s: TvSession
        s = TvSession { _ ->
            thread { replies.forEach { Thread.sleep(delayMs); s.onMessage(it) } }
            true
        }
        return s
    }

    private fun falha(block: () -> Unit): String =
        try { block(); "NAO_FALHOU" } catch (e: TvException) { e.message ?: "" }

    @Test fun pareiaComPedidoNaTela() {
        val s = fake(
            """{"type":"response","id":"register_0","payload":{"pairingType":"PROMPT","returnValue":true}}""",
            """{"type":"registered","id":"register_0","payload":{"client-key":"KEY1"}}"""
        )
        assertEquals("KEY1", s.register(null, 2_000))
    }

    @Test fun pareamentoNegado() {
        val s = fake("""{"type":"error","id":"register_0","error":"403 User denied access","payload":{}}""")
        assertTrue(falha { s.register(null, 2_000) }.contains("403 User denied access"))
    }

    @Test fun pareamentoExpira() {
        val s = fake()
        assertTrue(falha { s.register(null, 150) }.contains("pareamento"))
    }

    @Test fun requisicaoIgnoraMensagensDeOutroId() {
        val s = fake(
            """{"type":"response","id":"outro","payload":{"returnValue":true,"state":"X"}}""",
            """{"type":"response","id":"req_1","payload":{"returnValue":true,"state":"Screen Off"}}"""
        )
        assertEquals("Screen Off", s.request("ssap://x/y", null, 2_000).state)
    }

    @Test fun erro401ViraMensagemClara() {
        val s = fake("""{"type":"error","id":"req_1","error":"401 insufficient permissions","payload":{}}""")
        val msg = falha { s.request("ssap://x/y", null, 2_000) }
        assertTrue(msg.contains("permissão"))
        assertTrue(msg.contains("[x/y]"))   // diz qual comando foi negado
    }

    @Test fun returnValueFalsoViraErro() {
        val s = fake("""{"type":"response","id":"req_1","payload":{"returnValue":false,"errorText":"sem app"}}""")
        assertEquals("sem app", falha { s.request("ssap://x/y", null, 2_000) })
    }

    @Test fun semRespostaExpira() {
        assertTrue(falha { fake().request("ssap://x/y", null, 150) }.contains("não respondeu"))
    }

    @Test fun conexaoFechadaDuranteComando() {
        lateinit var s: TvSession
        s = TvSession { _ -> thread { Thread.sleep(30); s.onClosed("reset") }; true }
        assertTrue(falha { s.request("ssap://x/y", null, 2_000) }.contains("encerrou"))
    }

    @Test fun envioFalhoAvisa() {
        assertTrue(falha { TvSession { false }.request("ssap://x/y") }.contains("caiu"))
    }

    @Test fun destinoDoSpotify() {
        assertEquals(SpotifyTarget.TV, SpotifyTarget.fromName("TV"))
        assertEquals(SpotifyTarget.TV, SpotifyTarget.fromName(null))
        assertEquals(SpotifyTarget.TV, SpotifyTarget.fromName("lixo"))
    }
}
