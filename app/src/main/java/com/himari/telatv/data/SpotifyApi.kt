package com.himari.telatv.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Spotify Web API para controlar o player da TV por Spotify Connect (a LG não tem comando de
 * "pular faixa"). Login por OAuth com PKCE: sem segredo no app, só o Client ID de um app criado
 * pelo próprio usuário em developer.spotify.com. Lógica PURA (sem Android nem rede); a rede fica
 * em net/SpotifyClient.kt.
 */
object SpotifyApi {
    /** Cadastre exatamente este endereço em "Redirect URIs" do app no painel do Spotify. */
    const val REDIRECT_URI = "telatv://spotify-callback"
    const val SCHEME = "telatv"
    const val SCOPES = "user-modify-playback-state user-read-playback-state"
    const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
    const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    const val PLAYER_URL = "https://api.spotify.com/v1/me/player"
    const val DASHBOARD_URL = "https://developer.spotify.com/dashboard"

    private val base64Url = Base64.getUrlEncoder().withoutPadding()
    private val random = SecureRandom()

    private fun randomToken(bytes: Int): String {
        val b = ByteArray(bytes)
        random.nextBytes(b)
        return base64Url.encodeToString(b)
    }

    /** code_verifier do PKCE: 64 caracteres de [A-Za-z0-9_-] (o padrão pede de 43 a 128). */
    fun newVerifier(): String = randomToken(48)

    /** Valor aleatório para conferir que a resposta é do login que começamos. */
    fun newState(): String = randomToken(16)

    /** code_challenge (S256) = base64url(sha256(verifier)), sem preenchimento "=". */
    fun challenge(verifier: String): String =
        base64Url.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    /** Client ID do Spotify: letras e números (hoje 32 caracteres). */
    fun isValidClientId(id: String): Boolean = Regex("^[A-Za-z0-9]{20,64}$").matches(id.trim())

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun authorizeUrl(clientId: String, challenge: String, state: String): String =
        "$AUTHORIZE_URL?client_id=${enc(clientId.trim())}&response_type=code" +
            "&redirect_uri=${enc(REDIRECT_URI)}&code_challenge_method=S256" +
            "&code_challenge=${enc(challenge)}&state=${enc(state)}&scope=${enc(SCOPES)}"

    /** Resposta do navegador ao nosso REDIRECT_URI: ?code=...&state=... ou ?error=... */
    data class Callback(val code: String?, val state: String?, val error: String?)

    fun parseCallback(uri: String): Callback {
        val query = uri.substringBefore('#').substringAfter('?', "")
        val params = query.split('&').filter { it.isNotEmpty() }.associate {
            val k = it.substringBefore('=')
            val v = it.substringAfter('=', "")
            URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
        }
        return Callback(params["code"], params["state"], params["error"])
    }

    /** Campos do POST de troca do código por tokens. */
    fun codeForm(clientId: String, code: String, verifier: String): Map<String, String> = mapOf(
        "grant_type" to "authorization_code",
        "code" to code,
        "redirect_uri" to REDIRECT_URI,
        "client_id" to clientId.trim(),
        "code_verifier" to verifier
    )

    /** Campos do POST que renova o access token. */
    fun refreshForm(clientId: String, refreshToken: String): Map<String, String> = mapOf(
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
        "client_id" to clientId.trim()
    )

    /** [refreshToken] vem null quando o Spotify não manda um novo (continua valendo o antigo). */
    data class Tokens(val accessToken: String, val refreshToken: String?, val expiresInSec: Long)

    private fun jsonString(raw: String, key: String): String? =
        Regex(""""${Regex.escape(key)}"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(raw)?.groupValues?.get(1)

    fun parseTokens(json: String): Tokens? {
        val access = jsonString(json, "access_token") ?: return null
        val refresh = jsonString(json, "refresh_token")
        val expires = Regex(""""expires_in"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 3600L
        return Tokens(access, refresh, expires)
    }

    /** Mensagem de erro do Spotify no corpo ({"error":{"status":..,"message":".."}} ou {"error":"invalid_grant"}). */
    fun parseError(json: String): String? =
        jsonString(json, "message") ?: jsonString(json, "error_description") ?: jsonString(json, "error")

    /** Método e caminho (depois de [PLAYER_URL]) do comando de pular faixa; null para os demais. */
    fun skipRequest(cmd: RemoteCommand): Pair<String, String>? = when (cmd) {
        RemoteCommand.NEXT -> "POST" to "/next"
        RemoteCommand.PREVIOUS -> "POST" to "/previous"
        else -> null
    }

    /** Texto em pt-BR para um erro HTTP do player. [body] = corpo da resposta. */
    fun playerErrorMessage(code: Int, body: String): String = when {
        code == 404 -> "O Spotify não achou um player ativo. Abra o Spotify na TV, toque uma música e tente de novo."
        code == 403 && body.contains("PREMIUM_REQUIRED", ignoreCase = true) ->
            "O Spotify só deixa controlar a reprodução com conta Premium."
        code == 403 -> "O Spotify negou o comando (403). Confira se o seu usuário foi adicionado ao app em developer.spotify.com."
        code == 401 -> "O login do Spotify expirou. Toque em Conectar de novo nas Configurações."
        code == 429 -> "O Spotify pediu para esperar um pouco. Tente de novo em instantes."
        else -> "O Spotify retornou erro $code" + (parseError(body)?.let { ": $it" } ?: ".")
    }
}
