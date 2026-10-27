package com.himari.telatv

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

/** Manda teclas de mídia para o player ativo do celular (ex.: Spotify tocando na TV via Spotify Connect). */
object PhoneMedia {
    fun send(ctx: Context, keyCode: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
