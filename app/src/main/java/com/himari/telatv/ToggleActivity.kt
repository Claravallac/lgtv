package com.himari.telatv

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/** Activity transparente: roda a ação, mostra um aviso e fecha. Evita limites de segundo plano. */
class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext
        Thread {
            val out = ToggleAction.run(app)
            runOnUiThread {
                Toast.makeText(app, out.message, Toast.LENGTH_LONG).show()
                if (out.ok && out.openSpotifyOnPhone && !isFinishing) openSpotifyOnPhone()
                finish()
            }
        }.start()
    }

    private fun openSpotifyOnPhone() {
        val intent = packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (intent == null) {
            Toast.makeText(applicationContext, "Spotify não está instalado no celular.", Toast.LENGTH_LONG).show()
        } else {
            startActivity(intent)
        }
    }
}
