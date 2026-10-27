package com.himari.telatv

import android.content.Context
import com.himari.telatv.data.Prefs
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.ToggleFlow
import com.himari.telatv.data.TvException
import com.himari.telatv.net.TvClient

/** A ação do atalho/Tile: conecta, alterna a tela e (se configurado) abre o Spotify. Bloqueante. */
object ToggleAction {
    data class Outcome(val ok: Boolean, val message: String, val openSpotifyOnPhone: Boolean = false)

    fun run(context: Context): Outcome {
        val prefs = Prefs(context)
        val host = Ssap.normalizeHost(prefs.host)
            ?: return Outcome(false, "Informe o IP da TV no app TelaTV.")
        val savedKey = prefs.clientKey
            ?: return Outcome(false, "Pareie a TV primeiro, abrindo o app TelaTV.")
        return try {
            TvClient(host).use { tv ->
                tv.connect()
                val key = tv.session.register(savedKey, 10_000)
                if (key != savedKey) prefs.clientKey = key
                val done = ToggleFlow.perform(tv.session, prefs.spotify, prefs.screenAssumedOff)
                prefs.screenAssumedOff = done.usedAlt && done.plan.turnsOff
                Outcome(true, done.plan.successMessage + (done.warning?.let { " $it" } ?: ""), done.plan.spotifyOnPhone)
            }
        } catch (e: TvException) {
            Outcome(false, e.message ?: "Erro ao falar com a TV.")
        } catch (e: Exception) {
            Outcome(false, "Erro inesperado: ${e.message}")
        }
    }
}
