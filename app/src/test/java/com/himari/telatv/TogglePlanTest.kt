package com.himari.telatv

import com.himari.telatv.data.Ssap
import com.himari.telatv.data.SpotifyTarget
import com.himari.telatv.data.TogglePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TogglePlanTest {
    @Test fun apagaEAbreSpotifyNoCelular() {
        val p = TogglePlan.from("Active", SpotifyTarget.PHONE)
        assertEquals(Ssap.URI_SCREEN_OFF, p.screenUri)
        assertTrue(p.turnsOff && p.spotifyOnPhone && !p.spotifyOnTv)
        assertEquals("Tela da TV apagada.", p.successMessage)
    }

    @Test fun apagaEAbreSpotifyNaTv() {
        val p = TogglePlan.from("Active", SpotifyTarget.TV)
        assertTrue(p.turnsOff && p.spotifyOnTv && !p.spotifyOnPhone)
    }

    @Test fun apagaSemSpotify() {
        val p = TogglePlan.from("Active", SpotifyTarget.NONE)
        assertTrue(p.turnsOff && !p.spotifyOnTv && !p.spotifyOnPhone)
    }

    @Test fun acendeNaoAbreSpotify() {
        for (t in SpotifyTarget.entries) {
            val p = TogglePlan.from("Screen Off", t)
            assertEquals(Ssap.URI_SCREEN_ON, p.screenUri)
            assertTrue(!p.turnsOff && !p.spotifyOnTv && !p.spotifyOnPhone)
            assertEquals("Tela da TV acesa.", p.successMessage)
        }
    }
}
