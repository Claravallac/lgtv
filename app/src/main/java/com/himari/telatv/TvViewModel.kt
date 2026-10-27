package com.himari.telatv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.himari.telatv.data.MusicControl
import com.himari.telatv.data.Prefs
import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.SpotifyTarget
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.ToggleFlow
import com.himari.telatv.data.TvException
import com.himari.telatv.net.TvClient
import java.util.concurrent.Executors

data class UiState(
    val host: String = "",
    val paired: Boolean = false,
    val busy: Boolean = false,          // pareando ou alternando a tela
    val message: String? = null,        // erro, aviso ou instrução
    val screenOff: Boolean? = null,     // último estado conhecido da tela (null = desconhecido)
    val spotifyOnTv: Boolean = true
)

/**
 * Mantém UMA conexão com a TV aberta (volume instantâneo) e reconecta sozinho se cair.
 * Todo acesso à rede passa por uma única thread ([io]), então os comandos nunca se misturam.
 */
class TvViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val io = Executors.newSingleThreadExecutor()
    private var client: TvClient? = null   // só mexer dentro de [io]

    var ui by mutableStateOf(
        UiState(
            host = prefs.host,
            paired = prefs.clientKey != null,
            spotifyOnTv = prefs.spotify != SpotifyTarget.NONE
        )
    )
        private set

    private fun update(f: (UiState) -> UiState) { synchronized(this) { ui = f(ui) } }

    private fun fail(e: Exception) {
        val msg = if (e is TvException) e.message else "Erro inesperado: ${e.message}"
        update { it.copy(busy = false, message = msg) }
    }

    fun pair(hostInput: String) {
        val host = Ssap.normalizeHost(hostInput)
        if (host == null) {
            update { it.copy(message = "IP inválido. Exemplo: 192.168.0.10") }
            return
        }
        update { it.copy(busy = true, message = "Conectando… se aparecer um pedido na tela da TV, aceite.") }
        io.execute {
            try {
                closeClient()
                prefs.host = host                       // trocar de TV apaga a chave antiga
                val c = TvClient(host)
                client = c
                c.connect()
                prefs.clientKey = c.session.register(null, 60_000)
                update { it.copy(host = host, paired = true, busy = false, message = "TV pareada.", screenOff = null) }
            } catch (e: Exception) {
                closeClient()
                update { it.copy(host = host, paired = prefs.clientKey != null) }
                fail(e)
            }
        }
    }

    fun toggleScreen() {
        if (ui.busy) return
        update { it.copy(busy = true, message = null) }
        io.execute {
            try {
                val done = withClient { ToggleFlow.perform(it.session, prefs.spotify) }
                update { it.copy(busy = false, screenOff = done.plan.turnsOff, message = done.warning) }
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun send(cmd: RemoteCommand) {
        io.execute {
            try {
                val key = cmd.phoneKeyCode
                if (key != null && prefs.musicControl == MusicControl.PHONE) {
                    PhoneMedia.send(getApplication<Application>(), key)
                } else {
                    withClient { it.execute(cmd) }
                }
                if (ui.message != null) update { it.copy(message = null) }
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun setSpotifyOnTv(on: Boolean) {
        prefs.spotify = if (on) SpotifyTarget.TV else SpotifyTarget.NONE
        update { it.copy(spotifyOnTv = on) }
    }

    /** Usa a conexão aberta; se ela caiu, reconecta uma vez e repete o comando. */
    private fun <T> withClient(block: (TvClient) -> T): T {
        val host = Ssap.normalizeHost(prefs.host) ?: throw TvException("Informe o IP da TV e toque em Parear.")
        val key = prefs.clientKey ?: throw TvException("Pareie a TV primeiro.")
        val existing = client
        if (existing != null) {
            try {
                return block(existing)
            } catch (e: TvException) {
                closeClient()   // pode ter sido queda de conexão: tenta de novo abaixo
            }
        }
        val c = TvClient(host)
        try {
            c.connect()
            val newKey = c.session.register(key, 10_000)
            if (newKey != key) prefs.clientKey = newKey
            client = c
            return block(c)
        } catch (e: Exception) {
            try { c.close() } catch (_: Exception) {}
            client = null
            throw e
        }
    }

    private fun closeClient() {
        try { client?.close() } catch (_: Exception) {}
        client = null
    }

    override fun onCleared() {
        io.execute { closeClient() }
        io.shutdown()
    }
}
