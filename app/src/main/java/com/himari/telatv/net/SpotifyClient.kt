package com.himari.telatv.net

import com.himari.telatv.data.Prefs
import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.SpotifyApi
import com.himari.telatv.data.TvException
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fala com a Web API do Spotify (HTTPS normal, certificado verificado) para pular faixa no player
 * ativo da conta, que é o app Spotify da TV quando a música toca nele. Bloqueante: usar fora da
 * thread principal. Guarda os tokens em [Prefs].
 */
class SpotifyClient(private val prefs: Prefs) {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private class Reply(val code: Int, val body: String)

    private fun internetError(e: IOException) =
        TvException("Não consegui falar com o Spotify (internet do celular?). ${e.message ?: ""}".trim())

    private fun postForm(form: Map<String, String>): Reply {
        val body = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
        val req = Request.Builder().url(SpotifyApi.TOKEN_URL).post(body).build()
        try {
            http.newCall(req).execute().use { return Reply(it.code, it.body?.string() ?: "") }
        } catch (e: IOException) {
            throw internetError(e)
        }
    }

    private fun save(t: SpotifyApi.Tokens) {
        prefs.spotifyAccessToken = t.accessToken
        // O Spotify nem sempre manda um refresh token novo: nesse caso vale o antigo.
        if (t.refreshToken != null) prefs.spotifyRefreshToken = t.refreshToken
        prefs.spotifyExpiresAt = System.currentTimeMillis() + t.expiresInSec * 1000
    }

    /** Termina o login: troca o [code] recebido no redirecionamento por access/refresh token. */
    fun exchangeCode(code: String) {
        val verifier = prefs.spotifyVerifier ?: throw TvException("O login do Spotify não foi iniciado. Toque em Conectar.")
        val r = postForm(SpotifyApi.codeForm(prefs.spotifyClientId, code, verifier))
        val tokens = if (r.code == 200) SpotifyApi.parseTokens(r.body) else null
        if (tokens == null) {
            throw TvException("O Spotify recusou o login (${r.code})" + (SpotifyApi.parseError(r.body)?.let { ": $it" } ?: "."))
        }
        save(tokens)
        prefs.spotifyVerifier = null
        prefs.spotifyState = null
    }

    /** Renova o access token. Se o Spotify revogou o login (invalid_grant), desconecta. */
    private fun refresh(): String {
        val refreshToken = prefs.spotifyRefreshToken ?: throw TvException("Conecte o Spotify nas Configurações.")
        val r = postForm(SpotifyApi.refreshForm(prefs.spotifyClientId, refreshToken))
        val tokens = if (r.code == 200) SpotifyApi.parseTokens(r.body) else null
        if (tokens == null) {
            if (r.code == 400 || r.code == 401) {
                prefs.clearSpotifyLogin()
                throw TvException("O login do Spotify expirou. Toque em Conectar de novo nas Configurações.")
            }
            throw TvException("Não consegui renovar o login do Spotify (${r.code}).")
        }
        save(tokens)
        return tokens.accessToken
    }

    private fun accessToken(): String {
        val current = prefs.spotifyAccessToken
        return if (current != null && System.currentTimeMillis() < prefs.spotifyExpiresAt - 60_000) current else refresh()
    }

    private fun player(method: String, path: String, token: String): Reply {
        val req = Request.Builder()
            .url(SpotifyApi.PLAYER_URL + path)
            .method(method, ByteArray(0).toRequestBody())
            .header("Authorization", "Bearer $token")
            .build()
        try {
            http.newCall(req).execute().use { return Reply(it.code, it.body?.string() ?: "") }
        } catch (e: IOException) {
            throw internetError(e)
        }
    }

    /** Pula para a próxima/anterior faixa do player ativo. */
    fun skip(cmd: RemoteCommand) {
        val (method, path) = SpotifyApi.skipRequest(cmd) ?: throw TvException("Comando sem equivalente no Spotify.")
        var r = player(method, path, accessToken())
        if (r.code == 401) r = player(method, path, refresh())   // token revogado antes de expirar: renova 1x
        if (r.code !in 200..299) throw TvException(SpotifyApi.playerErrorMessage(r.code, r.body))
    }

    fun close() {
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }
}
