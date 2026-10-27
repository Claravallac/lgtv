package com.himari.telatv.data

/**
 * Decide o que o atalho faz, dado o estado de energia atual da TV. PURO (testado com kotlinc).
 * Regra: o Spotify abre ao APAGAR a tela (uso típico: ouvir música com a tela apagada).
 * Na TV, o Spotify abre ANTES de apagar a tela; no celular, depois que a TV confirmou.
 */
data class TogglePlan(
    val screenUri: String,
    val turnsOff: Boolean,
    val spotifyOnTv: Boolean,
    val spotifyOnPhone: Boolean
) {
    val successMessage: String get() = if (turnsOff) "Tela da TV apagada." else "Tela da TV acesa."

    companion object {
        fun from(powerState: String?, target: SpotifyTarget): TogglePlan {
            val uri = Ssap.toggleUri(powerState)
            val off = uri == Ssap.URI_SCREEN_OFF
            return TogglePlan(uri, off, off && target == SpotifyTarget.TV, off && target == SpotifyTarget.PHONE)
        }
    }
}
