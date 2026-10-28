package com.himari.telatv.data

/**
 * Comandos do controle. PURO. [uri] = comando SSAP; [button] = tecla do controle remoto
 * (enviada pelo socket de "ponteiro", ver TvClient.sendButton). O app só controla a TV.
 */
enum class RemoteCommand(val label: String, val uri: String? = null, val button: String? = null) {
    VOLUME_UP("Volume +", Ssap.URI_VOLUME_UP),
    VOLUME_DOWN("Volume −", Ssap.URI_VOLUME_DOWN),
    MUTE("Mudo", button = "MUTE"),
    PLAY("Tocar", Ssap.URI_MEDIA_PLAY),
    PAUSE("Pausar", Ssap.URI_MEDIA_PAUSE),

    // FASTFORWARD/REWIND só avançam/voltam dentro da faixa (era o bug). Pular de verdade é pela
    // API do Spotify (ver SpotifyApi/SpotifyClient), usada quando o Spotify está conectado; sem ela,
    // tenta CH+/CH− da TV (palpite: próxima/anterior em players de mídia do webOS).
    NEXT("Próxima", button = "CHANNELUP"),
    PREVIOUS("Anterior", button = "CHANNELDOWN"),

    UP("Cima", button = "UP"),
    DOWN("Baixo", button = "DOWN"),
    LEFT("Esquerda", button = "LEFT"),
    RIGHT("Direita", button = "RIGHT"),
    OK("Confirmar", button = "ENTER"),
    BACK("Voltar", button = "BACK"),
    HOME("Início", button = "HOME");

    val payload: String? get() = null

    /** Segurar o botão repete o comando (navegar em menus, mudar o volume). */
    val repeatable: Boolean
        get() = when (this) {
            UP, DOWN, LEFT, RIGHT, VOLUME_UP, VOLUME_DOWN -> true
            else -> false
        }

    /** Pular de faixa: precisa do player do Spotify (não existe comando de "pular" no SSAP da LG). */
    val skipsTrack: Boolean get() = this == NEXT || this == PREVIOUS
}
