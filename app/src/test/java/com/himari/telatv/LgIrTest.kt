package com.himari.telatv

import com.himari.telatv.data.LgIr
import com.himari.telatv.data.RemoteCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LgIrTest {
    /** Lê os 32 bits do quadro de volta (LSB primeiro) e devolve os 4 bytes enviados. */
    private fun decode(frame: IntArray): List<Int> {
        val bytes = MutableList(4) { 0 }
        for (i in 0 until 32) {
            val space = frame[2 + i * 2 + 1]
            if (space > 1_000) bytes[i / 8] = bytes[i / 8] or (1 shl (i % 8))
        }
        return bytes
    }

    private fun reverseBits(b: Int): Int {
        var r = 0
        for (i in 0 until 8) if ((b shr i) and 1 == 1) r = r or (1 shl (7 - i))
        return r
    }

    @Test fun quadroTemFormatoNec() {
        val f = LgIr.frame(LgIr.POWER)
        assertEquals(68, f.size)                    // par: liga/desliga alternados
        assertEquals(9_000, f[0])
        assertEquals(4_500, f[1])
        for (i in 0 until 32) assertEquals(560, f[2 + i * 2])   // todo bit começa com pulso de 560 µs
        assertEquals(560, f[66])                    // pulso final
        assertTrue(f[67] > 10_000)                  // pausa depois do quadro
        assertEquals(38_000, LgIr.CARRIER_HZ)
    }

    @Test fun bytesDoPower() {
        // endereço 0x04, ~endereço 0xFB, comando 0x08, ~comando 0xF7
        assertEquals(listOf(0x04, 0xFB, 0x08, 0xF7), decode(LgIr.frame(LgIr.POWER)))
    }

    @Test fun bateComOFormatoDaListaDaComunidade() {
        // Nas listas, 0x20DF10EF = power: os mesmos 4 bytes com os bits de cada um invertidos de ordem.
        val bytes = decode(LgIr.frame(LgIr.POWER)).map(::reverseBits)
        assertEquals(listOf(0x20, 0xDF, 0x10, 0xEF), bytes)
        // 0x20DF906F = mute, 0x20DF40BF = vol +, 0x20DF22DD = ok, 0x20DF14EB = voltar.
        assertEquals(listOf(0x20, 0xDF, 0x90, 0x6F), decode(LgIr.frame(LgIr.codeFor(RemoteCommand.MUTE)!!)).map(::reverseBits))
        assertEquals(listOf(0x20, 0xDF, 0x40, 0xBF), decode(LgIr.frame(LgIr.codeFor(RemoteCommand.VOLUME_UP)!!)).map(::reverseBits))
        assertEquals(listOf(0x20, 0xDF, 0x22, 0xDD), decode(LgIr.frame(LgIr.codeFor(RemoteCommand.OK)!!)).map(::reverseBits))
        assertEquals(listOf(0x20, 0xDF, 0x14, 0xEB), decode(LgIr.frame(LgIr.codeFor(RemoteCommand.BACK)!!)).map(::reverseBits))
    }

    @Test fun codigosDosBotoes() {
        assertEquals(0x08, LgIr.POWER)
        assertEquals(0x02, LgIr.codeFor(RemoteCommand.VOLUME_UP))
        assertEquals(0x03, LgIr.codeFor(RemoteCommand.VOLUME_DOWN))
        assertEquals(0x09, LgIr.codeFor(RemoteCommand.MUTE))
        assertEquals(0x40, LgIr.codeFor(RemoteCommand.UP))
        assertEquals(0x41, LgIr.codeFor(RemoteCommand.DOWN))
        assertEquals(0x07, LgIr.codeFor(RemoteCommand.LEFT))
        assertEquals(0x06, LgIr.codeFor(RemoteCommand.RIGHT))
        assertEquals(0x44, LgIr.codeFor(RemoteCommand.OK))
        assertEquals(0x28, LgIr.codeFor(RemoteCommand.BACK))
        assertEquals(0x7C, LgIr.codeFor(RemoteCommand.HOME))
        assertEquals(0xB0, LgIr.codeFor(RemoteCommand.PLAY))
        assertEquals(0xBA, LgIr.codeFor(RemoteCommand.PAUSE))
    }

    @Test fun proximaEAnteriorNaoExistemNoControleIr() {
        assertNull(LgIr.codeFor(RemoteCommand.NEXT))
        assertNull(LgIr.codeFor(RemoteCommand.PREVIOUS))
    }

    @Test fun codigosSaoUnicosEDiferentesDoPower() {
        val codes = RemoteCommand.entries.mapNotNull { LgIr.codeFor(it) }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(LgIr.POWER !in codes)
        for (c in codes) assertNotNull(LgIr.frame(c))
    }

    @Test fun todoComandoTemMapeamentoOuEhPularFaixa() {
        // Só próxima/anterior ficam sem código de IR.
        val semCodigo = RemoteCommand.entries.filter { LgIr.codeFor(it) == null }.toSet()
        assertEquals(setOf(RemoteCommand.NEXT, RemoteCommand.PREVIOUS), semCodigo)
    }
}
