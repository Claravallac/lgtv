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

    var musicControl: MusicControl
        get() = MusicControl.fromName(sp.getString("music_control", null))
        set(v) { sp.edit().putString("music_control", v.name).apply() }
}
