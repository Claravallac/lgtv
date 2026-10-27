package com.himari.telatv

import com.himari.telatv.data.SshKey
import com.himari.telatv.data.TvException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SshKeyTest {
    private fun erro(input: String): String = try { SshKey.normalize(input); "" } catch (e: TvException) { e.message!! }

    @Test fun vazioEhSemChave() {
        assertNull(SshKey.normalize("   \n "))
    }

    @Test fun reconstroiChaveColadaEmUmaLinhaSo() {
        val k = SshKey.normalize("-----BEGIN RSA PRIVATE KEY----- AAAA BBBB CCCC -----END RSA PRIVATE KEY-----")!!
        assertEquals("-----BEGIN RSA PRIVATE KEY-----\nAAAABBBBCCCC\n-----END RSA PRIVATE KEY-----\n", k)
    }

    @Test fun aceitaCrLfEQuebraEm64() {
        val body = "A".repeat(100)
        val k = SshKey.normalize("-----BEGIN OPENSSH PRIVATE KEY-----\r\n$body\r\n-----END OPENSSH PRIVATE KEY-----\r\n")!!
        val lines = k.trim().lines()
        assertEquals("-----BEGIN OPENSSH PRIVATE KEY-----", lines.first())
        assertEquals(64, lines[1].length); assertEquals(36, lines[2].length)
        assertEquals("-----END OPENSSH PRIVATE KEY-----", lines.last())
    }

    @Test fun chavePublicaEhRecusadaComAviso() {
        assertTrue(erro("ssh-rsa AAAAB3Nza user@pc").contains("PÚBLICA"))
        assertTrue(erro("ssh-ed25519 AAAAC3 user@pc").contains("PÚBLICA"))
    }

    @Test fun chaveComSenhaEhRecusada() {
        val pem = "-----BEGIN RSA PRIVATE KEY-----\nProc-Type: 4,ENCRYPTED\nDEK-Info: AES-128-CBC,AB\n\nAAAA\n-----END RSA PRIVATE KEY-----"
        assertTrue(erro(pem).contains("senha"))
        assertTrue(erro("-----BEGIN ENCRYPTED PRIVATE KEY-----\nAAAA\n-----END ENCRYPTED PRIVATE KEY-----").contains("senha"))
    }

    @Test fun textoSemPemEhInvalido() {
        assertTrue(erro("qualquer coisa").contains("inválida"))
        assertTrue(erro("-----BEGIN RSA PRIVATE KEY-----\n-----END RSA PRIVATE KEY-----").contains("vazia"))
    }

    @Test fun cabecalhoLegadoSemSenhaEhRemovido() {
        val k = SshKey.normalize("-----BEGIN RSA PRIVATE KEY-----\nComment: x\nAAAA\nBBBB\n-----END RSA PRIVATE KEY-----")!!
        assertEquals("-----BEGIN RSA PRIVATE KEY-----\nAAAABBBB\n-----END RSA PRIVATE KEY-----\n", k)
    }
}
