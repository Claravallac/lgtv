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

    // Manifesto ASSINADO pela LG (o mesmo usado por pywebostv/aiowebostv/Hubitat). Sem ele a TV responde
    // "401 insufficient permissions" em turnOffScreen. NÃO altere nada dentro de SIGNED_JSON: a assinatura
    // cobre esse trecho exatamente (permissões, serial, nomes...).
    private const val SIGNATURE =
        "eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2Iiwia2V5SWQiOiJ0ZXN0LXNpZ25pbmctY2VydCIsInNpZ25hdHVyZVZlcnNpb24iOjF9." +
            "hrVRgjCwXVvE2OOSpDZ58hR+59aFNwYDyjQgKk3auukd7pcegmE2CzPCa0bJ0ZsRAcKkCTJrWo5iDzNhMBWRyaMOv5zWSrthlf7G128qvIlpMT0YNY+n/FaOHE73uLrS/g7swl3/qH/BGFG2Hu4RlL48eb3lLKqTt2xKHdCs6Cd4RMfJPYnzgvI4BNrFUKsjkcu+WD4OO2A27Pq1n50cMchmcaXadJhGrOqH5YmHdOCj5NSHzJYrsW0HPlpuAx/ECMeIZYDh6RMqaFM2DXzdKX9NmmyqzJ3o/0lkk/N97gfVRLW5hA29yeAwaCViZNCP8iC9aO0q9fQojoa7NQnAtw=="

    private val SIGNED_PERMISSIONS = listOf(
        "TEST_SECURE", "CONTROL_INPUT_TEXT", "CONTROL_MOUSE_AND_KEYBOARD", "READ_INSTALLED_APPS", "READ_LGE_SDX",
        "READ_NOTIFICATIONS", "SEARCH", "WRITE_SETTINGS", "WRITE_NOTIFICATION_ALERT", "CONTROL_POWER",
        "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS", "READ_UPDATE_INFO", "UPDATE_FROM_REMOTE_APP",
        "READ_LGE_TV_INPUT_EVENTS", "READ_TV_CURRENT_TIME"
    )

    private val PERMISSIONS = listOf(
        "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE", "TEST_OPEN", "TEST_PROTECTED", "CONTROL_AUDIO",
        "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK", "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_MEDIA_PLAYBACK",
        "CONTROL_INPUT_TV", "CONTROL_POWER", "READ_APP_STATUS", "READ_CURRENT_CHANNEL", "READ_INPUT_DEVICE_LIST",
        "READ_NETWORK_STATE", "READ_RUNNING_APPS", "READ_TV_CHANNEL_LIST", "WRITE_NOTIFICATION_TOAST",
        "READ_POWER_STATE", "READ_COUNTRY_INFO"
    )

    private fun jsonList(items: List<String>) = items.joinToString(",") { "\"$it\"" }

    private val SIGNED_JSON: String =
        """{"created":"20140509","appId":"com.lge.test","vendorId":"com.lge",""" +
            """"localizedAppNames":{"":"LG Remote App","ko-KR":"리모컨 앱","zxx-XX":"ЛГ Rэмotэ AПП"},""" +
            """"localizedVendorNames":{"":"LG Electronics"},""" +
            """"permissions":[${jsonList(SIGNED_PERMISSIONS)}],"serial":"2f930e2d2cfe083771f68e4fe7bb07"}"""

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
        return """{"type":"register","id":"${escape(id)}","payload":{"forcePairing":false,""" +
            """"pairingType":"PROMPT"$key,"manifest":{"manifestVersion":1,"appVersion":"1.1",""" +
            """"signed":$SIGNED_JSON,"permissions":[${jsonList(PERMISSIONS)}],""" +
            """"signatures":[{"signatureVersion":1,"signature":"$SIGNATURE"}]}}}"""
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
