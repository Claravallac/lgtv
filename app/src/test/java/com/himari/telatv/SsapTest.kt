package com.himari.telatv

import com.himari.telatv.data.Ssap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsapTest {
    @Test fun alternaTela() {
        assertEquals(Ssap.URI_SCREEN_ON, Ssap.toggleUri("Screen Off"))
        assertEquals(Ssap.URI_SCREEN_ON, Ssap.toggleUri(" active standby "))
        assertEquals(Ssap.URI_SCREEN_OFF, Ssap.toggleUri("Active"))
        assertEquals(Ssap.URI_SCREEN_OFF, Ssap.toggleUri(null))
    }

    @Test fun registroPrimeiraVezNaoTemChave() {
        val m = Ssap.registerMessage("reg_0", null)
        assertTrue(m.contains("\"type\":\"register\""))
        assertTrue(!m.contains("client-key"))
        assertTrue(m.contains("\"CONTROL_POWER\""))
    }

    @Test fun registroTemManifestoAssinado() {
        val m = Ssap.registerMessage("reg_0", null)
        assertTrue(m.contains("\"signed\":{\"created\":\"20140509\""))
        assertTrue(m.contains("\"appId\":\"com.lge.test\""))
        assertTrue(m.contains("\"signatures\":[{\"signatureVersion\":1,\"signature\":\"eyJhbGdvcml0aG0i"))
        assertTrue(m.contains("\"READ_POWER_STATE\""))
        assertTrue(m.contains("\"CONTROL_INPUT_MEDIA_PLAYBACK\""))
        // chaves e colchetes balanceados (sem aspas dentro de strings, então a contagem simples vale)
        assertEquals(m.count { it == '{' }, m.count { it == '}' })
        assertEquals(m.count { it == '[' }, m.count { it == ']' })
    }

    @Test fun registroComChave() {
        val m = Ssap.registerMessage("reg_0", "abc\"123")
        assertTrue(m.contains("\"client-key\":\"abc\\\"123\""))
    }

    @Test fun requisicao() {
        assertEquals(
            """{"type":"request","id":"r1","uri":"ssap://x/y"}""",
            Ssap.requestMessage("r1", "ssap://x/y")
        )
        assertEquals(
            """{"type":"request","id":"r2","uri":"ssap://system.launcher/launch","payload":{"id":"spotify-beehive"}}""",
            Ssap.requestMessage("r2", Ssap.URI_LAUNCH, Ssap.launchPayload(Ssap.SPOTIFY_TV_ID))
        )
    }

    @Test fun leRespostas() {
        val a = Ssap.parse("""{"type":"registered","id":"reg_0","payload":{"client-key":"K123"}}""")
        assertEquals("registered", a.type); assertEquals("K123", a.clientKey)

        val b = Ssap.parse("""{"type":"response","id":"r1","payload":{"returnValue":true,"state":"Screen Off"}}""")
        assertTrue(b.ok); assertEquals("Screen Off", b.state); assertEquals("r1", b.id)

        val c = Ssap.parse("""{"type":"error","id":"r1","error":"401 insufficient permissions","payload":{}}""")
        assertEquals("error", c.type); assertEquals("401 insufficient permissions", c.error); assertTrue(!c.ok)

        val d = Ssap.parse("""{"type":"response","id":"r1","payload":{"returnValue":false,"errorText":"nope"}}""")
        assertTrue(!d.ok); assertEquals("nope", d.error)
    }

    @Test fun validaHost() {
        assertEquals("192.168.0.10", Ssap.normalizeHost(" 192.168.0.10 "))
        assertEquals("192.168.0.10", Ssap.normalizeHost("ws://192.168.0.10:3000"))
        assertEquals("lgwebostv.local", Ssap.normalizeHost("lgwebostv.local"))
        assertNull(Ssap.normalizeHost("300.1.1.1"))
        assertNull(Ssap.normalizeHost("192.168.0"))
        assertNull(Ssap.normalizeHost(""))
        assertNull(Ssap.normalizeHost("a b"))
    }
}
