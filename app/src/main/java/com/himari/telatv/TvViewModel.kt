package com.himari.telatv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.himari.telatv.data.MusicControl
import com.himari.telatv.data.Prefs
import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.ScreenDriver
import com.himari.telatv.data.SpotifyTarget
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.ToggleFlow
import com.himari.telatv.data.TvException
import com.himari.telatv.net.TvClient
import com.himari.telatv.data.SsapScreen
import com.himari.telatv.net.TvDiscovery
import com.himari.telatv.net.sshScreen
import java.util.concurrent.Executors
import kotlin.concurrent.thread

data class UiState(
    val host: String = "",
    val paired: Boolean = false,
    val busy: Boolean = false,          // pareando ou alternando a tela
    val message: String? = null,        // erro, aviso ou instrução
    val screenOff: Boolean? = null,     // último estado conhecido da tela (null = desconhecido)
    val spotifyOnTv: Boolean = true,
    val musicOnPhone: Boolean = false,  // play/pausa/próxima/anterior pelo celular em vez da TV
    val searching: Boolean = false,     // procurando a TV na rede
    val found: List<String> = emptyList(), // IPs de TVs LG achadas
    val sshEnabled: Boolean = false,
    val sshUser: String = "root",
    val sshPort: String = "22",
    val sshPassword: String = "",
    val sshKey: String = ""
) {
    /** Pode mostrar o controle: pareado (SSAP) ou com SSH ligado e IP salvo (TV com root não precisa parear). */
    val ready: Boolean get() = paired || (sshEnabled && host.isNotBlank())
}

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
            spotifyOnTv = prefs.spotify != SpotifyTarget.NONE,
            musicOnPhone = prefs.musicControl == MusicControl.PHONE,
            sshEnabled = prefs.sshEnabled,
            sshUser = prefs.sshUser,
            sshPort = prefs.sshPort.toString(),
            sshPassword = prefs.sshPassword,
            sshKey = prefs.sshKey
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

    /** Procura TVs LG na rede (SSDP) numa thread própria, sem travar a fila de comandos. */
    fun discover() {
        if (ui.searching) return
        update { it.copy(searching = true, found = emptyList(), message = null) }
        thread {
            val ips = try {
                TvDiscovery.find(getApplication<Application>())
            } catch (e: Exception) {
                emptyList()
            }
            update {
                it.copy(
                    searching = false,
                    found = ips,
                    message = if (ips.isEmpty())
                        "Nenhuma TV encontrada. Confira se o celular está no mesmo Wi‑Fi da TV (e a TV ligada), ou digite o IP."
                    else it.message
                )
            }
        }
    }

    fun toggleScreen() {
        if (ui.busy) return
        update { it.copy(busy = true, message = null) }
        io.execute {
            try {
                val done = if (prefs.sshEnabled) {
                    // TV com root: tudo por SSH, sem parear.
                    val ssh = sshScreen(prefs) ?: throw TvException("Informe o IP da TV.")
                    try {
                        ToggleFlow.perform(prefs.spotify, prefs.screenAssumedOff, ssh)
                    } finally {
                        ssh.close()
                    }
                } else {
                    withClient { ToggleFlow.perform(it.session, prefs.spotify, prefs.screenAssumedOff, SsapScreen(it.session)) }
                }
                prefs.screenAssumedOff = done.usedAlt && done.plan.turnsOff
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

    fun setSshEnabled(on: Boolean) {
        prefs.sshEnabled = on
        update { it.copy(sshEnabled = on) }
    }

    fun setSshUser(v: String) {
        prefs.sshUser = v
        update { it.copy(sshUser = v) }
    }

    fun setSshPort(v: String) {
        val digits = v.filter { it.isDigit() }.take(5)
        prefs.sshPort = digits.toIntOrNull() ?: 22
        update { it.copy(sshPort = digits) }
    }

    fun setSshPassword(v: String) {
        prefs.sshPassword = v
        update { it.copy(sshPassword = v) }
    }

    fun setSshKey(v: String) {
        prefs.sshKey = v
        update { it.copy(sshKey = v) }
    }

    /** Guarda o IP sem parear (modo SSH). Devolve false se o IP for inválido. */
    fun saveHost(input: String): Boolean {
        val host = Ssap.normalizeHost(input)
        if (host == null) {
            update { it.copy(message = "IP inválido. Exemplo: 192.168.0.10") }
            return false
        }
        if (host != prefs.host) {
            io.execute { closeClient() }
            prefs.host = host                       // trocar de TV apaga a chave de pareamento
            update { it.copy(host = host, paired = prefs.clientKey != null, screenOff = null) }
        }
        return true
    }

    /** Conecta por SSH e lê o estado de energia, para conferir usuário, chave/senha e porta. */
    fun testSsh() {
        if (ui.busy) return
        update { it.copy(busy = true, message = "Testando o SSH…") }
        thread {
            var ssh: ScreenDriver? = null
            try {
                ssh = sshScreen(prefs, ignoreSwitch = true)
                if (ssh == null) throw TvException("Informe o IP da TV.")
                val st = ssh.powerState()
                update { it.copy(busy = false, message = "SSH funcionando. Estado da TV: ${st ?: "desconhecido"}.") }
            } catch (e: Exception) {
                fail(e)
            } finally {
                ssh?.close()
            }
        }
    }

    fun setSpotifyOnTv(on: Boolean) {
        prefs.spotify = if (on) SpotifyTarget.TV else SpotifyTarget.NONE
        update { it.copy(spotifyOnTv = on) }
    }

    fun setMusicOnPhone(on: Boolean) {
        prefs.musicControl = if (on) MusicControl.PHONE else MusicControl.TV
        update { it.copy(musicOnPhone = on) }
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
