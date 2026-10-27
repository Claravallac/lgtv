package com.himari.telatv.data

/**
 * Protocolo SSAP das TVs LG webOS (WebSocket, mesmo usado pelo app LG ThinQ).
 * Lógica PURA: só monta e lê mensagens. Sem Android, sem rede, sem org.json
 * (assim compila e testa com kotlinc). A rede fica em net/TvClient.kt.
 */
object Ssap {
    const val PORT_WS = 3000          // webOS antigo (sem TLS)
    const val PORT_WSS = 3001         // webOS 4+ (TLS com certificado autoassinado)

    const val URI_POWER_STATE = "ssap://com.webos.service.tvpower/power/getPowerState"
    const val URI_SCREEN_OFF = "ssap://com.webos.service.tvpower/power/turnOffScreen"
    const val URI_SCREEN_ON = "ssap://com.webos.service.tvpower/power/turnOnScreen"
    const val URI_LAUNCH = "ssap://system.launcher/launch"
    const val URI_VOLUME_UP = "ssap://audio/volumeUp"
    const val URI_VOLUME_DOWN = "ssap://audio/volumeDown"
    const val URI_MEDIA_PLAY = "ssap://media.controls/play"
    const val URI_MEDIA_PAUSE = "ssap://media.controls/pause"
    /** Devolve o endereço de um 2º WebSocket que aceita "botões do controle remoto". */
    const val URI_POINTER_SOCKET = "ssap://com.webos.service.networkinput/getPointerInputSocket"

    /** Id do app Spotify na TV (Content Store). */
    const val SPOTIFY_TV_ID = "spotify-beehive"

    /** Estados em que a tela está apagada e o certo é ACENDER. */
    private val SCREEN_DARK_STATES = setOf("screen off", "active standby")

    // Permissões pedidas no pareamento. Manifesto SEM assinatura (ver PLANO.md, "Riscos").
    private val PERMISSIONS = listOf(
        "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CONTROL_POWER", "READ_POWER_STATE",
        "READ_RUNNING_APPS", "READ_INSTALLED_APPS", "CONTROL_DISPLAY", "CONTROL_INPUT_TV",
        "CONTROL_AUDIO", "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_MOUSE_AND_KEYBOARD", "CONTROL_INPUT_TEXT"
    )

    data class Reply(
        val type: String?,
        val id: String?,
        val returnValue: Boolean?,
        val state: String?,
        val clientKey: String?,
        val error: String?,
        val socketPath: String? = null
    ) {
        /** Resposta de comando bem-sucedida (ou sem returnValue explícito). */
        val ok: Boolean get() = type == "response" && returnValue != false
    }

    fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }

    /** Mensagem de pareamento. [clientKey] null = primeira vez (a TV mostra o pedido). */
    fun registerMessage(id: String, clientKey: String?): String {
        val key = if (clientKey.isNullOrBlank()) "" else ""","client-key":"${escape(clientKey)}""""
        val perms = PERMISSIONS.joinToString(",") { "\"$it\"" }
        return """{"type":"register","id":"${escape(id)}","payload":{"forcePairing":false,""" +
            """"pairingType":"PROMPT"$key,"manifest":{"manifestVersion":1,"appVersion":"1.0",""" +
            """"permissions":[$perms]}}}"""
    }

    fun requestMessage(id: String, uri: String, payloadJson: String? = null): String {
        val payload = if (payloadJson.isNullOrBlank()) "" else ""","payload":$payloadJson"""
        return """{"type":"request","id":"${escape(id)}","uri":"${escape(uri)}"$payload}"""
    }

    /** Texto enviado no socket do controle remoto, ex.: "type:button\nname:MUTE\n\n". */
    fun buttonMessage(name: String): String = "type:button\nname:${name.filter { it.isLetterOrDigit() }}\n\n"

    fun launchPayload(appId: String): String = """{"id":"${escape(appId)}"}"""

    /** Dado o estado atual de energia, qual comando alterna a tela. */
    fun toggleUri(powerState: String?): String =
        if (powerState?.trim()?.lowercase() in SCREEN_DARK_STATES) URI_SCREEN_ON else URI_SCREEN_OFF

    private fun str(raw: String, key: String): String? =
        Regex(""""${Regex.escape(key)}"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(raw)?.groupValues?.get(1)

    fun parse(raw: String): Reply {
        val rv = Regex(""""returnValue"\s*:\s*(true|false)""").find(raw)?.groupValues?.get(1)
        return Reply(
            type = str(raw, "type"),
            id = str(raw, "id"),
            returnValue = rv?.toBooleanStrictOrNull(),
            state = str(raw, "state"),
            clientKey = str(raw, "client-key"),
            error = str(raw, "error") ?: str(raw, "errorText"),
            socketPath = str(raw, "socketPath")?.replace("\\/", "/")
        )
    }

    private val IPV4 = Regex("""^(25[0-5]|2[0-4]\d|1?\d?\d)(\.(25[0-5]|2[0-4]\d|1?\d?\d)){3}$""")
    private val HOSTNAME = Regex("""^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*$""")

    /** Aceita IPv4 ou nome de host (ex.: lgwebostv.local). Retorna null se inválido. */
    fun normalizeHost(input: String): String? {
        val h = input.trim().removePrefix("ws://").removePrefix("wss://").substringBefore(":").trim()
        if (h.isEmpty()) return null
        if (h.all { it.isDigit() || it == '.' }) return if (IPV4.matches(h)) h else null
        return if (HOSTNAME.matches(h)) h else null
    }
}
