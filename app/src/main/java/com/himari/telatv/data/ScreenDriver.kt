package com.himari.telatv.data

/**
 * Como a tela da TV é apagada/acesa. PURO (sem Android): [SsapScreen] usa o protocolo SSAP;
 * [SshScreen] roda `luna-send` na TV por SSH (TVs com acesso root).
 */
interface ScreenDriver {
    /** Estado de energia ("Active", "Screen Off", ...) ou null se não deu para ler. */
    fun powerState(): String?

    /** Apaga ([off] = true) ou acende a tela. Devolve true se o estado de energia pode não refletir a tela. */
    fun setScreen(off: Boolean): Boolean

    /** Abre um app na TV (ex.: Spotify). Lança [TvException] se a TV recusar. */
    fun launchApp(appId: String)

    /** Libera conexões abertas pelo driver. */
    fun close() {}
}

private val DARK_STATES = setOf("screen off", "active standby")

fun isConnectionError(e: TvException): Boolean = e.message?.contains("conexão") == true

/**
 * SSAP: primeiro o serviço `tvpower`; se a TV negar ou não tiver, tenta o alternativo `tv.power`
 * (standbyMode "active", que mantém o áudio tocando).
 */
class SsapScreen(private val session: TvSession) : ScreenDriver {
    override fun launchApp(appId: String) {
        session.request(Ssap.URI_LAUNCH, Ssap.launchPayload(appId))
    }

    override fun powerState(): String? = try {
        session.request(Ssap.URI_POWER_STATE).state
    } catch (e: TvException) {
        if (isConnectionError(e)) throw e
        null   // sem estado (negado/inexistente): decide pelo que lembramos
    }

    override fun setScreen(off: Boolean): Boolean {
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
}

/**
 * SSH: executa `luna-send -n 1 luna://com.webos.service.tvpower/power/...` na TV (como root).
 * [run] executa um comando de shell na TV e devolve a saída.
 */
class SshScreen(private val run: (String) -> String, private val onClose: () -> Unit = {}) : ScreenDriver {
    companion object {
        fun lunaCommand(method: String, payload: String = "{}") =
            "luna-send -n 1 luna://com.webos.service.tvpower/power/$method '$payload'"

        fun launchCommand(appId: String) =
            "luna-send -n 1 luna://com.webos.applicationManager/launch '${Ssap.launchPayload(appId)}'"
    }

    override fun launchApp(appId: String) {
        val out = run(launchCommand(appId))
        if (Ssap.parse(out).returnValue != true) {
            throw TvException("A TV não abriu o app por SSH: ${out.trim().take(160)}")
        }
    }

    override fun powerState(): String? {
        val out = run(lunaCommand("getPowerState"))
        val r = Ssap.parse(out)
        if (r.returnValue != true) throw TvException("A TV não informou o estado por SSH: ${out.trim().take(160)}")
        return r.state
    }

    override fun setScreen(off: Boolean): Boolean {
        val method = if (off) "turnOffScreen" else "turnOnScreen"
        var out = run(lunaCommand(method))
        if (Ssap.parse(out).returnValue != true && off) {
            out = run(lunaCommand(method, """{"standbyMode":"active"}"""))
        }
        if (Ssap.parse(out).returnValue != true) {
            throw TvException("A TV recusou o comando por SSH: ${out.trim().take(160)}")
        }
        if (!off) return false
        // Algumas versões continuam dizendo "Active" com a tela apagada.
        val after = try { powerState() } catch (e: TvException) { null }
        return after?.trim()?.lowercase() !in DARK_STATES
    }

    override fun close() = onClose()
}
