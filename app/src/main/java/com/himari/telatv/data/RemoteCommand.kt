package com.himari.telatv.data

/** Onde mandar play/pausa/próxima/anterior: nos botões da TV ou no Spotify do celular. */
enum class MusicControl(val label: String) {
    TV("Na TV"),
    PHONE("No celular");

    companion object {
        fun fromName(name: String?): MusicControl = entries.firstOrNull { it.name == name } ?: TV
    }
}

/** Comandos básicos do controle. PURO. [uri] = comando SSAP; [button] = tecla do controle remoto. */
enum class RemoteCommand(val label: String, val uri: String? = null, val button: String? = null) {
    VOLUME_UP("Volume +", Ssap.URI_VOLUME_UP),
    VOLUME_DOWN("Volume −", Ssap.URI_VOLUME_DOWN),
    MUTE("Mudo", button = "MUTE"),
    PLAY("Tocar", Ssap.URI_MEDIA_PLAY),
    PAUSE("Pausar", Ssap.URI_MEDIA_PAUSE),
    NEXT("Próxima", button = "FASTFORWARD"),
    PREVIOUS("Anterior", button = "REWIND");

    val payload: String? get() = null

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
