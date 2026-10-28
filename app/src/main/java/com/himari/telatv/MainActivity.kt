package com.himari.telatv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.himari.telatv.data.SpotifyApi
import com.himari.telatv.ui.MainScreen
import com.himari.telatv.ui.TelaTvTheme

class MainActivity : ComponentActivity() {
    private val vm: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shortcuts.publish(this)
        // Só na 1ª criação: ao girar a tela o mesmo intent chegaria de novo com um código já usado.
        if (savedInstanceState == null) handleSpotifyRedirect(intent)
        setContent {
            TelaTvTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSpotifyRedirect(intent)
    }

    /** O navegador volta para telatv://spotify-callback?code=... ao fim do login no Spotify. */
    private fun handleSpotifyRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == SpotifyApi.SCHEME) vm.finishSpotifyLogin(data.toString())
    }
}
