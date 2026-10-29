package com.himari.telatv.data

/**
 * Infravermelho da TV LG (protocolo NEC, portadora de 38 kHz, endereço 0x04). PURO (sem Android):
 * só mapeia botões para códigos e monta o padrão de pulsos; quem transmite é o ConsumerIrManager
 * (ver TvViewModel).
 *
 * Códigos conferidos na tabela de IR da LG (manual de TV comercial: Power 08, Back 28, OK 44, Home 7C,
 * Menu 43) e em listas da comunidade (0x20DF10EF = power, escrito no formato "LSB primeiro").
 */
object LgIr {
    const val CARRIER_HZ = 38_000
    const val ADDRESS = 0x04

    /** Liga/desliga. É um alterna (o mesmo botão do controle): a TV não diz se estava ligada. */
    const val POWER = 0x08

    /**
     * Código de comando do botão equivalente no controle IR da LG, ou null se o IR não tem esse botão
     * (o controle da LG não tem próxima/anterior: pular faixa só pela API do Spotify).
     */
    fun codeFor(cmd: RemoteCommand): Int? = when (cmd) {
        RemoteCommand.VOLUME_UP -> 0x02
        RemoteCommand.VOLUME_DOWN -> 0x03
        RemoteCommand.MUTE -> 0x09
        RemoteCommand.UP -> 0x40
        RemoteCommand.DOWN -> 0x41
        RemoteCommand.LEFT -> 0x07
        RemoteCommand.RIGHT -> 0x06
        RemoteCommand.OK -> 0x44
        RemoteCommand.BACK -> 0x28
        RemoteCommand.HOME -> 0x7C
        RemoteCommand.PLAY -> 0xB0
        RemoteCommand.PAUSE -> 0xBA
        RemoteCommand.NEXT, RemoteCommand.PREVIOUS -> null
    }

    // Tempos do NEC, em microssegundos.
    private const val HEADER_MARK = 9_000
    private const val HEADER_SPACE = 4_500
    private const val BIT_MARK = 560
    private const val ZERO_SPACE = 560
    private const val ONE_SPACE = 1_690
    private const val TRAILING_GAP = 40_000

    /**
     * Quadro NEC: cabeçalho, 32 bits (endereço, ~endereço, comando, ~comando; cada byte com o bit menos
     * significativo primeiro), pulso final e uma pausa no fim (o padrão fica com número PAR de valores,
     * alternando liga/desliga, como o ConsumerIrManager espera).
     */
    fun frame(address: Int, command: Int): IntArray {
        val bytes = intArrayOf(address and 0xFF, address.inv() and 0xFF, command and 0xFF, command.inv() and 0xFF)
        val out = ArrayList<Int>(68)
        out.add(HEADER_MARK)
        out.add(HEADER_SPACE)
        for (b in bytes) {
            for (bit in 0 until 8) {
                out.add(BIT_MARK)
                out.add(if ((b shr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE)
            }
        }
        out.add(BIT_MARK)
        out.add(TRAILING_GAP)
        return out.toIntArray()
    }

    fun frame(command: Int): IntArray = frame(ADDRESS, command)
}
