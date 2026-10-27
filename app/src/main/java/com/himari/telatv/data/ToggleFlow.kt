package com.himari.telatv.data

/**
 * O fluxo de "alternar a tela" sobre uma sessão já registrada: estado -> (Spotify na TV) -> tela.
 * PURO (testado com kotlinc). Usado pelo atalho (ToggleAction) e pelo botão da tela principal.
 */
object ToggleFlow {
    data class Performed(val plan: TogglePlan, val warning: String?)

    fun perform(session: TvSession, target: SpotifyTarget): Performed {
        val state = session.request(Ssap.URI_POWER_STATE).state
        val plan = TogglePlan.from(state, target)
        var warning: String? = null
        if (plan.spotifyOnTv) {
            try {
                session.request(Ssap.URI_LAUNCH, Ssap.launchPayload(Ssap.SPOTIFY_TV_ID))
            } catch (e: TvException) {
                warning = "Spotify na TV: ${e.message}"
            }
        }
        session.request(plan.screenUri)
        return Performed(plan, warning)
    }
}
