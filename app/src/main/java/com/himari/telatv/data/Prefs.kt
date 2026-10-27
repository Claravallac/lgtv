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

    var musicControl: MusicControl
        get() = MusicControl.fromName(sp.getString("music_control", null))
        set(v) { sp.edit().putString("music_control", v.name).apply() }
}
