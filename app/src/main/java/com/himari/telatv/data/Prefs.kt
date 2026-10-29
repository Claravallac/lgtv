package com.himari.telatv.data

import android.content.Context

/** Configuração local (SharedPreferences). A chave de pareamento vale só para a TV do [host]. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("telatv", Context.MODE_PRIVATE)

    var host: String
        get() = sp.getString("host", "") ?: ""
        set(v) {
            if (v != host) clientKey = null
            sp.edit().putString("host", v).apply()
        }

    var clientKey: String?
        get() = sp.getString("client_key", null)
        set(v) {
            sp.edit().apply { if (v == null) remove("client_key") else putString("client_key", v) }.apply()
        }

    var spotify: SpotifyTarget
        get() = SpotifyTarget.fromName(sp.getString("spotify", null))
        set(v) { sp.edit().putString("spotify", v.name).apply() }

    /**
     * Em TVs onde getPowerState continua dizendo "Active" com a tela apagada (serviço alternativo),
     * guardamos que fomos nós que apagamos, para o próximo toque ACENDER.
     */
    var screenAssumedOff: Boolean
        get() = sp.getBoolean("screen_assumed_off", false)
        set(v) { sp.edit().putBoolean("screen_assumed_off", v).apply() }

    /** Apagar/acender a tela por SSH (TV com acesso root), em vez de SSAP. */
    var sshEnabled: Boolean
        get() = sp.getBoolean("ssh_enabled", false)
        set(v) { sp.edit().putBoolean("ssh_enabled", v).apply() }

    var sshUser: String
        get() = sp.getString("ssh_user", "root") ?: "root"
        set(v) { sp.edit().putString("ssh_user", v).apply() }

    var sshPort: Int
        get() = sp.getInt("ssh_port", 22)
        set(v) { sp.edit().putInt("ssh_port", v).apply() }

    var sshPassword: String
        get() = sp.getString("ssh_password", "") ?: ""
        set(v) { sp.edit().putString("ssh_password", v).apply() }

    /** Chave privada SSH (texto colado/importado). Vazia = login por senha. allowBackup=false no manifesto. */
    var sshKey: String
        get() = sp.getString("ssh_key", "") ?: ""
        set(v) { sp.edit().putString("ssh_key", v).apply() }

    /** Mandar os botões pelo emissor de infravermelho do celular (apontado para a TV), em vez da rede. */
    var irEnabled: Boolean
        get() = sp.getBoolean("ir_enabled", false)
        set(v) { sp.edit().putBoolean("ir_enabled", v).apply() }

    // --- Spotify Web API (login PKCE, sem segredo): pular faixa no player da TV por Spotify Connect ---

    var spotifyClientId: String
        get() = sp.getString("spotify_client_id", "") ?: ""
        set(v) { sp.edit().putString("spotify_client_id", v).apply() }

    var spotifyRefreshToken: String?
        get() = sp.getString("spotify_refresh", null)
        set(v) { sp.edit().apply { if (v == null) remove("spotify_refresh") else putString("spotify_refresh", v) }.apply() }

    var spotifyAccessToken: String?
        get() = sp.getString("spotify_access", null)
        set(v) { sp.edit().apply { if (v == null) remove("spotify_access") else putString("spotify_access", v) }.apply() }

    /** Quando o access token expira (epoch ms). */
    var spotifyExpiresAt: Long
        get() = sp.getLong("spotify_expires_at", 0L)
        set(v) { sp.edit().putLong("spotify_expires_at", v).apply() }

    /** Segredos do login em andamento (PKCE); apagados ao terminar. */
    var spotifyVerifier: String?
        get() = sp.getString("spotify_verifier", null)
        set(v) { sp.edit().apply { if (v == null) remove("spotify_verifier") else putString("spotify_verifier", v) }.apply() }

    var spotifyState: String?
        get() = sp.getString("spotify_state", null)
        set(v) { sp.edit().apply { if (v == null) remove("spotify_state") else putString("spotify_state", v) }.apply() }

    /** Desconecta o Spotify (mantém o Client ID). */
    fun clearSpotifyLogin() {
        sp.edit().remove("spotify_refresh").remove("spotify_access").remove("spotify_expires_at")
            .remove("spotify_verifier").remove("spotify_state").apply()
    }
}
