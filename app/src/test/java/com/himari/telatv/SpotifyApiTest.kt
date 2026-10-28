package com.himari.telatv

import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.SpotifyApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyApiTest {
    @Test fun desafioPkceBateComOExemploDaRfc7636() {
        // Apêndice B da RFC 7636.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            SpotifyApi.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test fun verifierTemTamanhoECaracteresValidos() {
        val v = SpotifyApi.newVerifier()
        assertTrue(v.length in 43..128)
        assertTrue(Regex("^[A-Za-z0-9_-]+$").matches(v))
        assertNotEquals(v, SpotifyApi.newVerifier())
    }

    @Test fun clientIdValido() {
        assertTrue(SpotifyApi.isValidClientId("0123456789abcdef0123456789abcdef"))
        assertTrue(SpotifyApi.isValidClientId("  0123456789abcdef0123456789abcdef  "))
        assertFalse(SpotifyApi.isValidClientId(""))
        assertFalse(SpotifyApi.isValidClientId("curto"))
        assertFalse(SpotifyApi.isValidClientId("0123456789abcdef0123456789abcde!"))
    }

    @Test fun urlDeLoginTemOsParametrosDoPkce() {
        val url = SpotifyApi.authorizeUrl("abc123abc123abc123abc123", "CHALLENGE", "STATE")
        assertTrue(url.startsWith("https://accounts.spotify.com/authorize?"))
        assertTrue(url.contains("client_id=abc123abc123abc123abc123"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("code_challenge=CHALLENGE"))
        assertTrue(url.contains("state=STATE"))
        assertTrue(url.contains("redirect_uri=telatv%3A%2F%2Fspotify-callback"))
        assertTrue(url.contains("scope=user-modify-playback-state%20user-read-playback-state"))
        assertFalse(url.contains(" "))
    }

    @Test fun leRespostaDeSucessoDoLogin() {
        val cb = SpotifyApi.parseCallback("telatv://spotify-callback?code=AQD%2Bx-y&state=S1")
        assertEquals("AQD+x-y", cb.code)
        assertEquals("S1", cb.state)
        assertNull(cb.error)
    }

    @Test fun leRespostaDeErroDoLogin() {
        val cb = SpotifyApi.parseCallback("telatv://spotify-callback?error=access_denied&state=S1")
        assertNull(cb.code)
        assertEquals("access_denied", cb.error)
    }

    @Test fun respostaSemParametros() {
        val cb = SpotifyApi.parseCallback("telatv://spotify-callback")
        assertNull(cb.code)
        assertNull(cb.state)
        assertNull(cb.error)
    }

    @Test fun formulariosDeToken() {
        val f = SpotifyApi.codeForm(" cid ", "CODE", "VER")
        assertEquals("authorization_code", f["grant_type"])
        assertEquals("cid", f["client_id"])
        assertEquals("CODE", f["code"])
        assertEquals("VER", f["code_verifier"])
        assertEquals("telatv://spotify-callback", f["redirect_uri"])
        val r = SpotifyApi.refreshForm("cid", "REF")
        assertEquals("refresh_token", r["grant_type"])
        assertEquals("REF", r["refresh_token"])
        assertEquals("cid", r["client_id"])
    }

    @Test fun leTokens() {
        val t = SpotifyApi.parseTokens(
            """{"access_token":"AAA","token_type":"Bearer","expires_in":3600,"refresh_token":"RRR","scope":"x"}"""
        )
        assertNotNull(t)
        assertEquals("AAA", t!!.accessToken)
        assertEquals("RRR", t.refreshToken)
        assertEquals(3600L, t.expiresInSec)
    }

    @Test fun renovacaoSemRefreshTokenNovo() {
        val t = SpotifyApi.parseTokens("""{"access_token":"BBB","expires_in":1800}""")
        assertEquals("BBB", t!!.accessToken)
        assertNull(t.refreshToken)
        assertEquals(1800L, t.expiresInSec)
    }

    @Test fun respostaSemAccessTokenNaoEhToken() {
        assertNull(SpotifyApi.parseTokens("""{"error":"invalid_grant","error_description":"Invalid authorization code"}"""))
    }

    @Test fun leMensagemDeErro() {
        assertEquals(
            "Invalid authorization code",
            SpotifyApi.parseError("""{"error":"invalid_grant","error_description":"Invalid authorization code"}""")
        )
        assertEquals(
            "Player command failed: Premium required",
            SpotifyApi.parseError("""{"error":{"status":403,"message":"Player command failed: Premium required","reason":"PREMIUM_REQUIRED"}}""")
        )
    }

    @Test fun soProximaEAnteriorViramRequisicaoDePular() {
        assertEquals("POST" to "/next", SpotifyApi.skipRequest(RemoteCommand.NEXT))
        assertEquals("POST" to "/previous", SpotifyApi.skipRequest(RemoteCommand.PREVIOUS))
        assertNull(SpotifyApi.skipRequest(RemoteCommand.PLAY))
        assertNull(SpotifyApi.skipRequest(RemoteCommand.VOLUME_UP))
    }

    @Test fun mensagensDeErroDoPlayer() {
        assertTrue(SpotifyApi.playerErrorMessage(404, "").contains("player ativo"))
        assertTrue(SpotifyApi.playerErrorMessage(403, """{"error":{"reason":"PREMIUM_REQUIRED"}}""").contains("Premium"))
        assertTrue(SpotifyApi.playerErrorMessage(403, "{}").contains("403"))
        assertTrue(SpotifyApi.playerErrorMessage(401, "").contains("expirou"))
        assertTrue(SpotifyApi.playerErrorMessage(429, "").contains("esperar"))
        assertTrue(SpotifyApi.playerErrorMessage(500, """{"error":{"message":"boom"}}""").contains("boom"))
    }
}
