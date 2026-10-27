package com.himari.telatv

import com.himari.telatv.data.MusicControl
import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.Ssap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test fun teclasDeMidiaDoCelular() {
        assertEquals(87, RemoteCommand.NEXT.phoneKeyCode)
        assertEquals(88, RemoteCommand.PREVIOUS.phoneKeyCode)
        assertEquals(126, RemoteCommand.PLAY.phoneKeyCode)
        assertEquals(127, RemoteCommand.PAUSE.phoneKeyCode)
        assertNull(RemoteCommand.VOLUME_UP.phoneKeyCode)
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

    @Test fun controleDeMusicaPadraoENaTv() {
        assertEquals(MusicControl.TV, MusicControl.fromName(null))
        assertEquals(MusicControl.PHONE, MusicControl.fromName("PHONE"))
    }
}
