package com.himari.telatv.data

/** Onde abrir o Spotify depois de alternar a tela. */
enum class SpotifyTarget(val label: String) {
    NONE("Não abrir"),
    PHONE("No celular"),
    TV("Na TV");

    companion object {
        fun fromName(name: String?): SpotifyTarget = entries.firstOrNull { it.name == name } ?: TV
    }
}
