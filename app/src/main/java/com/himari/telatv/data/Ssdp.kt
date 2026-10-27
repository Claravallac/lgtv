package com.himari.telatv.data

/**
 * Descoberta de TVs LG webOS na rede local (SSDP). PURO: só monta a pergunta e lê as respostas.
 * A rede (UDP) fica em net/TvDiscovery.kt.
 */
object Ssdp {
    const val ADDRESS = "239.255.255.250"
    const val PORT = 1900
    const val SEARCH_TARGET = "urn:lge-com:service:webos-second-screen:1"

    val searchRequest: String =
        "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: $ADDRESS:$PORT\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 2\r\n" +
            "ST: $SEARCH_TARGET\r\n" +
            "\r\n"

    /** Cabeçalhos da resposta, com o nome em minúsculas. */
    fun headers(response: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (line in response.split("\r\n", "\n")) {
            val i = line.indexOf(':')
            if (i <= 0) continue
            map.putIfAbsent(line.substring(0, i).trim().lowercase(), line.substring(i + 1).trim())
        }
        return map
    }

    /** A resposta é de uma TV LG webOS? */
    fun isLgTv(response: String): Boolean {
        val h = headers(response)
        val st = h["st"]?.lowercase().orEmpty()
        val usn = h["usn"]?.lowercase().orEmpty()
        val server = h["server"]?.lowercase().orEmpty()
        return "webos-second-screen" in st || "webos-second-screen" in usn || "webos" in server
    }

    /** IP ou nome do host em "LOCATION: http://192.168.0.10:1234/...", ou null. */
    fun hostFromLocation(response: String): String? {
        val loc = headers(response)["location"] ?: return null
        val host = loc.substringAfter("://", loc).substringBefore("/").substringBefore(":").trim()
        return Ssap.normalizeHost(host)
    }
}
