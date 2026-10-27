package com.himari.telatv.net

import android.content.Context
import android.net.wifi.WifiManager
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.Ssdp
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/** Procura TVs LG webOS na rede Wi‑Fi (SSDP). Bloqueante (~3 s): use fora da thread principal. */
object TvDiscovery {
    fun find(context: Context, timeoutMs: Long = 3_000): List<String> {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = try {
            wifi?.createMulticastLock("telatv")?.also { it.setReferenceCounted(false); it.acquire() }
        } catch (_: Exception) {
            null
        }
        val found = LinkedHashSet<String>()
        try {
            val socket = DatagramSocket()
            try {
                socket.soTimeout = 500
                val data = Ssdp.searchRequest.toByteArray()
                val group = InetAddress.getByName(Ssdp.ADDRESS)
                for (i in 0 until 2) {
                    try {
                        socket.send(DatagramPacket(data, data.size, group, Ssdp.PORT))
                    } catch (_: Exception) {
                    }
                }
                val buf = ByteArray(2048)
                val end = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < end) {
                    val packet = DatagramPacket(buf, buf.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val text = String(packet.data, 0, packet.length)
                    if (!Ssdp.isLgTv(text)) continue
                    val host = Ssdp.hostFromLocation(text) ?: packet.address?.hostAddress
                    val ip = host?.let { Ssap.normalizeHost(it) }
                    if (ip != null) found.add(ip)
                }
            } finally {
                socket.close()
            }
        } catch (_: Exception) {
            // sem rede / sem permissão: devolve o que achou (nada)
        } finally {
            try { lock?.release() } catch (_: Exception) {}
        }
        return found.toList()
    }
}
