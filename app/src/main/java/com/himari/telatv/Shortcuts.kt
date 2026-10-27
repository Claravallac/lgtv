package com.himari.telatv

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Atalho do app (toque longo no ícone) e atalho fixável na tela inicial. */
object Shortcuts {
    private const val ID = "toggle_tv"

    private fun info(ctx: Context): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(ctx, ID)
            .setShortLabel("Tela da TV")
            .setLongLabel("Alternar tela da TV")
            .setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
            .setIntent(Intent(ctx, ToggleActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()

    fun publish(ctx: Context) {
        try {
            ShortcutManagerCompat.setDynamicShortcuts(ctx, listOf(info(ctx)))
        } catch (_: Exception) {
        }
    }

    fun canPin(ctx: Context): Boolean = ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)

    /** Pede ao launcher para fixar o atalho na tela inicial (o usuário confirma). */
    fun requestPin(ctx: Context): Boolean =
        try { ShortcutManagerCompat.requestPinShortcut(ctx, info(ctx), null) } catch (_: Exception) { false }
}
