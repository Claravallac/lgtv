package com.himari.telatv

import com.himari.telatv.data.Ssdp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpTest {
    private val lg = "HTTP/1.1 200 OK\r\n" +
        "CACHE-CONTROL: max-age=1800\r\n" +
        "LOCATION: http://192.168.0.10:1234/\r\n" +
        "SERVER: Linux/4.4 UPnP/1.0 webOS/5.0\r\n" +
        "ST: urn:lge-com:service:webos-second-screen:1\r\n" +
        "USN: uuid:abc::urn:lge-com:service:webos-second-screen:1\r\n\r\n"

    @Test fun perguntaTemOsCamposDoSsdp() {
        val q = Ssdp.searchRequest
        assertTrue(q.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue(q.contains("MAN: \"ssdp:discover\""))
        assertTrue(q.contains("ST: urn:lge-com:service:webos-second-screen:1"))
        assertTrue(q.endsWith("\r\n\r\n"))
    }

    @Test fun reconheceTvLg() {
        assertTrue(Ssdp.isLgTv(lg))
    }

    @Test fun ignoraOutrosAparelhos() {
        val roteador = "HTTP/1.1 200 OK\r\nSERVER: Linux UPnP/1.0 MiniUPnPd/2.1\r\nST: upnp:rootdevice\r\n\r\n"
        assertFalse(Ssdp.isLgTv(roteador))
    }

    @Test fun lePeloCabecalhoSemImportarMaiusculas() {
        assertTrue(Ssdp.isLgTv("HTTP/1.1 200 OK\r\nst: URN:LGE-COM:SERVICE:WEBOS-SECOND-SCREEN:1\r\n\r\n"))
    }

    @Test fun hostVemDoLocation() {
        assertEquals("192.168.0.10", Ssdp.hostFromLocation(lg))
        assertNull(Ssdp.hostFromLocation("HTTP/1.1 200 OK\r\nST: x\r\n\r\n"))
        assertNull(Ssdp.hostFromLocation("HTTP/1.1 200 OK\r\nLOCATION: http://999.1.1.1:80/\r\n\r\n"))
    }
}
