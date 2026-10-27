package com.himari.telatv

import android.content.Context
import com.himari.telatv.data.Prefs
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.ToggleFlow
import com.himari.telatv.data.TvException
import com.himari.telatv.data.SsapScreen
import com.himari.telatv.net.TvClient
import com.himari.telatv.net.sshScreen

/** A ação do atalho/Tile: conecta, alterna a tela e (se configurado) abre o Spotify. Bloqueante. */
object ToggleAction {
    data class Outcome(val ok: Boolean, val message: String, val openSpotifyOnPhone: Boolean = false)

    /** Com root + SSH não precisa parear a TV (SSAP): apaga/acende e abre o Spotify pelo próprio SSH. */
    private fun runSsh(prefs: Prefs): Outcome {
        val ssh = sshScreen(prefs) ?: return Outcome(false, "Informe o IP da TV no app TelaTV.")
        try {
            val done = ToggleFlow.perform(prefs.spotify, prefs.screenAssumedOff, ssh)
            prefs.screenAssumedOff = done.usedAlt && done.plan.turnsOff
            return Outcome(true, done.plan.successMessage + (done.warning?.let { " $it" } ?: ""), done.plan.spotifyOnPhone)
        } finally {
            ssh.close()
        }
    }

    private fun runSsap(prefs: Prefs, host: String): Outcome {
        val savedKey = prefs.clientKey
            ?: return Outcome(false, "Pareie a TV primeiro, abrindo o app TelaTV.")
        TvClient(host).use { tv ->
            tv.connect()
            val key = tv.session.register(savedKey, 10_000)
            if (key != savedKey) prefs.clientKey = key
            val done = ToggleFlow.perform(tv.session, prefs.spotify, prefs.screenAssumedOff, SsapScreen(tv.session))
            prefs.screenAssumedOff = done.usedAlt && done.plan.turnsOff
            return Outcome(true, done.plan.successMessage + (done.warning?.let { " $it" } ?: ""), done.plan.spotifyOnPhone)
        }
    }

    fun run(context: Context): Outcome {
        val prefs = Prefs(context)
        val host = Ssap.normalizeHost(prefs.host)
            ?: return Outcome(false, "Informe o IP da TV no app TelaTV.")
        return try {
            if (prefs.sshEnabled) runSsh(prefs) else runSsap(prefs, host)
        } catch (e: TvException) {
            Outcome(false, e.message ?: "Erro ao falar com a TV.")
        } catch (e: Exception) {
            Outcome(false, "Erro inesperado: ${e.message}")
        }
    }
}
