package com.himari.telatv.data

/**
 * O fluxo de "alternar a tela" sobre uma sessão já registrada: estado -> (Spotify na TV) -> tela.
 * PURO (testado com kotlinc). Usado pelo atalho (ToggleAction) e pelo botão da tela principal.
 *
 * A tela é apagada primeiro pelo serviço `tvpower`; se a TV negar ou não tiver esse serviço, tenta o serviço
 * alternativo `tv.power` (com standbyMode "active", que mantém o áudio tocando).
 */
object ToggleFlow {
    /** [usedAlt]: a tela foi alternada pelo serviço alternativo (o estado de energia pode não refletir a tela). */
    data class Performed(val plan: TogglePlan, val warning: String?, val usedAlt: Boolean = false)

    fun perform(session: TvSession, target: SpotifyTarget, assumedOff: Boolean = false): Performed {
        val rawState = try {
            session.request(Ssap.URI_POWER_STATE).state
        } catch (e: TvException) {
            if (isConnectionError(e)) throw e
            null   // sem estado (negado/inexistente): decide pelo que lembramos
        }
        val state = if (assumedOff && rawState?.trim()?.lowercase() == "active") "Screen Off" else rawState
        val plan = TogglePlan.from(state, target)
        var warning: String? = null
        if (plan.spotifyOnTv) {
            try {
                session.request(Ssap.URI_LAUNCH, Ssap.launchPayload(Ssap.SPOTIFY_TV_ID))
            } catch (e: TvException) {
                warning = "Spotify na TV: ${e.message}"
            }
        }
        val usedAlt = setScreen(session, off = plan.turnsOff)
        return Performed(plan, warning, usedAlt)
    }

    /** Devolve true se precisou do serviço alternativo. */
    private fun setScreen(session: TvSession, off: Boolean): Boolean {
        val primary = if (off) Ssap.URI_SCREEN_OFF else Ssap.URI_SCREEN_ON
        val alt = if (off) Ssap.URI_ALT_SCREEN_OFF else Ssap.URI_ALT_SCREEN_ON
        return try {
            session.request(primary)
            false
        } catch (first: TvException) {
            if (isConnectionError(first)) throw first
            try {
                session.request(alt, Ssap.ALT_SCREEN_PAYLOAD)
                true
            } catch (second: TvException) {
                // Algumas TVs respondem erro no 1º "acender" mesmo acendendo a tela.
                if (!off && second.message?.contains("POWER_ERROR_0005") == true) {
                    true
                } else {
                    throw TvException("${first.message} | Alternativa: ${second.message}")
                }
            }
        }
    }

    private fun isConnectionError(e: TvException): Boolean = e.message?.contains("conexão") == true
}
