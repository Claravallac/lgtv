package com.himari.telatv.data

/** Onde mandar play/pausa/próxima/anterior: nos botões da TV ou no Spotify do celular. */
enum class MusicControl(val label: String) {
    TV("Na TV"),
    PHONE("No celular");

    companion object {
        fun fromName(name: String?): MusicControl = entries.firstOrNull { it.name == name } ?: TV
    }
}

/**
 * Comandos do controle. PURO. [uri] = comando SSAP; [button] = tecla do controle remoto
 * (enviada pelo socket de "ponteiro", ver TvClient.sendButton).
 */
enum class RemoteCommand(val label: String, val uri: String? = null, val button: String? = null) {
    VOLUME_UP("Volume +", Ssap.URI_VOLUME_UP),
    VOLUME_DOWN("Volume −", Ssap.URI_VOLUME_DOWN),
    MUTE("Mudo", button = "MUTE"),
    PLAY("Tocar", Ssap.URI_MEDIA_PLAY),
    PAUSE("Pausar", Ssap.URI_MEDIA_PAUSE),

    // FASTFORWARD/REWIND só avançam/voltam dentro da faixa (era o bug). Pular de verdade:
    // CH+/CH− (próximo/anterior em players de mídia do webOS) na TV, ou tecla de mídia no celular.
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

    /** Código de tecla de mídia do Android (usado quando o controle é "No celular"). */
    val phoneKeyCode: Int?
        get() = when (this) {
            PLAY -> 126      // KEYCODE_MEDIA_PLAY
            PAUSE -> 127     // KEYCODE_MEDIA_PAUSE
            NEXT -> 87       // KEYCODE_MEDIA_NEXT
            PREVIOUS -> 88   // KEYCODE_MEDIA_PREVIOUS
            else -> null
        }
}
