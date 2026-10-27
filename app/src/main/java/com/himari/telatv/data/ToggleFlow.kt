package com.himari.telatv.data

/**
 * O fluxo de "alternar a tela" sobre uma sessão já registrada: estado -> (Spotify na TV) -> tela.
 * PURO (testado com kotlinc). Usado pelo atalho (ToggleAction) e pelo botão da tela principal.
 *
 * Quem apaga/acende a tela é o [ScreenDriver] (SSAP por padrão; SSH em TVs com root).
 */
object ToggleFlow {
    /** [usedAlt]: o estado de energia da TV pode não refletir a tela (lembrar que apagamos). */
    data class Performed(val plan: TogglePlan, val warning: String?, val usedAlt: Boolean = false)

    /** Versão com sessão SSAP (o app Spotify também é aberto pelo [screen], seja SSAP ou SSH). */
    fun perform(
        session: TvSession,
        target: SpotifyTarget,
        assumedOff: Boolean = false,
        screen: ScreenDriver = SsapScreen(session)
    ): Performed = perform(target, assumedOff, screen)

    /** Sem SSAP: tudo pelo [screen]. Com SSH (TV com root) não precisa parear a TV. */
    fun perform(
        target: SpotifyTarget,
        assumedOff: Boolean,
        screen: ScreenDriver
    ): Performed {
        val rawState = screen.powerState()
        val state = if (assumedOff && rawState?.trim()?.lowercase() == "active") "Screen Off" else rawState
        val plan = TogglePlan.from(state, target)
        var warning: String? = null
        if (plan.spotifyOnTv) {
            try {
                screen.launchApp(Ssap.SPOTIFY_TV_ID)
            } catch (e: TvException) {
                warning = "Spotify na TV: ${e.message}"
            }
        }
        val usedAlt = screen.setScreen(off = plan.turnsOff)
        return Performed(plan, warning, usedAlt)
    }
}
