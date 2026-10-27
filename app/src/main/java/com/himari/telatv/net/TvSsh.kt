package com.himari.telatv.net

import com.himari.telatv.data.Prefs
import com.himari.telatv.data.ScreenDriver
import com.himari.telatv.data.SshKey
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.SshScreen
import com.himari.telatv.data.TvException
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.io.Closeable

/** Conexão SSH com a TV (reaproveitada entre comandos). Bloqueante: use fora da thread principal. */
class TvSsh(
    private val host: String,
    private val user: String,
    private val port: Int,
    private val password: String,
    /** Chave privada já normalizada (PEM) ou null para entrar só por senha. */
    private val privateKey: String? = null
) : Closeable {
    private var session: Session? = null

    private fun allow(s: Session, key: String, extra: String) {
        val cur = s.getConfig(key)
        s.setConfig(key, if (cur.isNullOrEmpty()) extra else "$cur,$extra")
    }

    private fun open(timeoutMs: Int): Session {
        session?.let { if (it.isConnected) return it }
        val jsch = JSch()
        if (!privateKey.isNullOrEmpty()) jsch.addIdentity("telatv", privateKey.toByteArray(Charsets.UTF_8), null, null)
        val s = jsch.getSession(user, host, port)
        if (password.isNotEmpty()) s.setPassword(password)
        s.setConfig("StrictHostKeyChecking", "no")
        // Chave primeiro (TV com root + Homebrew Channel); senha/vazio como alternativa.
        s.setConfig("PreferredAuthentications", "publickey,none,password,keyboard-interactive")
        // TVs com SSH antigo (dropbear): aceitar algoritmos legados (inclusive chave RSA assinada com ssh-rsa).
        allow(s, "server_host_key", "ssh-rsa,ssh-dss")
        allow(s, "PubkeyAcceptedAlgorithms", "ssh-rsa")
        allow(s, "kex", "diffie-hellman-group14-sha1,diffie-hellman-group1-sha1")
        s.connect(timeoutMs)
        session = s
        return s
    }

    /** Roda um comando e devolve a saída (stdout + stderr). */
    fun exec(command: String, timeoutMs: Int = 6_000): String {
        try {
            val s = open(timeoutMs)
            val ch = s.openChannel("exec") as ChannelExec
            val out = ByteArrayOutputStream()
            ch.setCommand(command)
            ch.setInputStream(null)
            ch.setOutputStream(out)
            ch.setErrStream(out)
            ch.connect(timeoutMs)
            val end = System.currentTimeMillis() + timeoutMs
            while (!ch.isClosed && System.currentTimeMillis() < end) Thread.sleep(40)
            Thread.sleep(80)
            ch.disconnect()
            return out.toString("UTF-8")
        } catch (e: JSchException) {
            close()
            val m = e.message.orEmpty()
            val low = m.lowercase()
            throw TvException(
                when {
                    "invalid privatekey" in low || "passphrase" in low || "unsupported" in low && "key" in low ->
                        "A TV não aceitou o formato da chave. Use uma chave RSA sem senha " +
                            "(ssh-keygen -t rsa -b 2048 -m PEM) e cole a privada inteira."
                    "Auth fail" in m || "Auth cancel" in m ->
                        "O SSH da TV recusou o login. Confira o usuário (root) e a chave ou senha."
                    "refused" in m ->
                        "A TV recusou a conexão SSH (porta $port). O SSH do Homebrew Channel usa a porta 22; " +
                            "confira se ele está ligado na TV."
                    "timeout" in m.lowercase() || "timed out" in m.lowercase() ->
                        "A TV não respondeu ao SSH (porta $port). Confira o IP e se o SSH está ativo."
                    else -> "Erro de SSH: $m"
                }
            )
        }
    }

    override fun close() {
        try { session?.disconnect() } catch (_: Exception) {}
        session = null
    }
}

/** Driver de tela por SSH, se estiver ligado nas configurações ([ignoreSwitch] = usar mesmo desligado, p/ testar). */
fun sshScreen(prefs: Prefs, ignoreSwitch: Boolean = false): ScreenDriver? {
    if (!prefs.sshEnabled && !ignoreSwitch) return null
    val host = Ssap.normalizeHost(prefs.host) ?: return null
    val key = SshKey.normalize(prefs.sshKey)   // lança TvException se a chave colada for inválida
    val ssh = TvSsh(host, prefs.sshUser.ifBlank { "root" }, prefs.sshPort, prefs.sshPassword, key)
    return SshScreen(run = { ssh.exec(it) }, onClose = { ssh.close() })
}
