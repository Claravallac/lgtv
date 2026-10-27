package com.himari.telatv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.himari.telatv.ui.MainScreen
import com.himari.telatv.ui.TelaTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shortcuts.publish(this)
        setContent {
            TelaTvTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel<TvViewModel>())
                }
            }
        }
    }
}
