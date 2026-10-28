package com.himari.telatv

import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.Ssap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCommandTest {
    @Test fun todoComandoTemUmCaminho() {
        for (c in RemoteCommand.entries) assertTrue((c.uri != null) != (c.button != null))
    }

    @Test fun volumeUsaSsap() {
        assertEquals("ssap://audio/volumeUp", RemoteCommand.VOLUME_UP.uri)
        assertEquals("ssap://audio/volumeDown", RemoteCommand.VOLUME_DOWN.uri)
        assertEquals("MUTE", RemoteCommand.MUTE.button)
    }

    @Test fun proximaEAnteriorNaoUsamBuscaDentroDaFaixa() {
        // FASTFORWARD/REWIND só avançam/voltam dentro da música.
        for (c in listOf(RemoteCommand.NEXT, RemoteCommand.PREVIOUS)) {
            assertTrue(c.button != "FASTFORWARD" && c.button != "REWIND")
        }
        assertEquals("CHANNELUP", RemoteCommand.NEXT.button)
        assertEquals("CHANNELDOWN", RemoteCommand.PREVIOUS.button)
    }

    @Test fun soProximaEAnteriorPulamFaixa() {
        val pulam = RemoteCommand.entries.filter { it.skipsTrack }.toSet()
        assertEquals(setOf(RemoteCommand.NEXT, RemoteCommand.PREVIOUS), pulam)
    }

    @Test fun direcionaisEConfirmarSaoBotoesDoControle() {
        assertEquals("UP", RemoteCommand.UP.button)
        assertEquals("DOWN", RemoteCommand.DOWN.button)
        assertEquals("LEFT", RemoteCommand.LEFT.button)
        assertEquals("RIGHT", RemoteCommand.RIGHT.button)
        assertEquals("ENTER", RemoteCommand.OK.button)
        assertEquals("BACK", RemoteCommand.BACK.button)
        assertEquals("HOME", RemoteCommand.HOME.button)
        for (c in listOf(RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT,
            RemoteCommand.OK, RemoteCommand.BACK, RemoteCommand.HOME)) {
            assertTrue(!c.skipsTrack)
        }
    }

    @Test fun segurarRepeteSoDirecionaisEVolume() {
        val repete = RemoteCommand.entries.filter { it.repeatable }.toSet()
        assertEquals(
            setOf(RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT,
                RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN),
            repete
        )
    }

    @Test fun nomesDeBotaoPassamPeloFiltroDaMensagem() {
        for (c in RemoteCommand.entries) {
            val b = c.button ?: continue
            assertEquals("type:button\nname:$b\n\n", Ssap.buttonMessage(b))
        }
    }

    @Test fun idsDosAppsNaTv() {
        assertEquals("spotify-beehive", Ssap.SPOTIFY_TV_ID)
        assertEquals("youtube.leanback.v4", Ssap.YOUTUBE_TV_ID)
        assertEquals("{\"id\":\"youtube.leanback.v4\"}", Ssap.launchPayload(Ssap.YOUTUBE_TV_ID))
    }

    @Test fun mensagemDeBotao() {
        assertEquals("type:button\nname:MUTE\n\n", Ssap.buttonMessage("MUTE"))
        assertEquals("type:button\nname:MUTE\n\n", Ssap.buttonMessage("MU\nTE"))
    }

    @Test fun leCaminhoDoSocket() {
        val r = Ssap.parse("""{"type":"response","id":"req_1","payload":{"socketPath":"wss://192.168.0.5:3001/resources/abc/netinput.pointer.sock","returnValue":true}}""")
        assertEquals("wss://192.168.0.5:3001/resources/abc/netinput.pointer.sock", r.socketPath)
    }

    @Test fun caminhoDoSocketComBarraEscapada() {
        val r = Ssap.parse("""{"type":"response","id":"r","payload":{"socketPath":"wss:\/\/10.0.0.2:3001\/resources\/x.sock"}}""")
        assertEquals("wss://10.0.0.2:3001/resources/x.sock", r.socketPath)
    }
}
