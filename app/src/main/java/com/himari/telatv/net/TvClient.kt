package com.himari.telatv.net

import com.himari.telatv.data.RemoteCommand
import com.himari.telatv.data.Ssap
import com.himari.telatv.data.TvException
import com.himari.telatv.data.TvSession
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.Closeable
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Conexão WebSocket com a TV (wss:3001, senão ws:3000). Bloqueante: use fora da thread principal.
 * O certificado da TV é autoassinado, então este cliente aceita qualquer certificado.
 * Isso é aceitável porque ele só fala com o host da TV digitado pelo usuário, na rede local.
 */
class TvClient(private val host: String) : Closeable {
    private val http: OkHttpClient = buildClient()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var pointer: WebSocket? = null
    val session = TvSession { text -> socket?.send(text) ?: false }

    fun connect(timeoutMs: Long = 4_000) {
        var lastError: String? = null
        for ((scheme, port) in listOf("wss" to Ssap.PORT_WSS, "ws" to Ssap.PORT_WS)) {
            val opened = CountDownLatch(1)
            val ok = AtomicBoolean(false)
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    ok.set(true); opened.countDown()
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (ok.get()) session.onMessage(text)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (ok.get()) session.onClosed(reason.ifBlank { null })
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    lastError = t.message
                    opened.countDown()
                    if (ok.get()) session.onClosed(t.message)
                }
            }
            val ws = http.newWebSocket(Request.Builder().url("$scheme://$host:$port").build(), listener)
            opened.await(timeoutMs, TimeUnit.MILLISECONDS)
            if (ok.get()) { socket = ws; return }
            ws.cancel()
        }
        throw TvException(
            "Não consegui conectar na TV ($host). Confira se a TV está ligada, o IP, o Wi‑Fi e se o " +
                "controle pela rede está ativado na TV." + (lastError?.let { " ($it)" } ?: "")
        )
    }

    /** Executa um comando do controle: via SSAP ou como botão do controle remoto. */
    fun execute(cmd: RemoteCommand) {
        val uri = cmd.uri
        if (uri != null) session.request(uri, cmd.payload) else sendButton(cmd.button!!)
    }

    /** Abre o socket dos botões já (sem enviar nada), para o 1º toque não pagar essa espera. */
    fun prepareButtons() {
        if (pointer == null) pointer = openPointer()
    }

    /** Envia um botão do controle remoto (abre o socket de "ponteiro" na 1ª vez). */
    fun sendButton(name: String) {
        val sock = pointer ?: openPointer().also { pointer = it }
        if (!sock.send(Ssap.buttonMessage(name))) throw TvException("Não consegui enviar o botão $name à TV.")
    }

    private fun openPointer(): WebSocket {
        val path = session.request(Ssap.URI_POINTER_SOCKET).socketPath
            ?: throw TvException("A TV não abriu o canal do controle remoto (permissão?). Refaça o pareamento.")
        val opened = CountDownLatch(1)
        val ok = AtomicBoolean(false)
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { ok.set(true); opened.countDown() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { opened.countDown() }
        }
        val ws = http.newWebSocket(Request.Builder().url(path).build(), listener)
        opened.await(4, TimeUnit.SECONDS)
        if (!ok.get()) { ws.cancel(); throw TvException("Não consegui abrir o canal do controle remoto da TV.") }
        return ws
    }

    override fun close() {
        pointer?.close(1000, null)
        socket?.close(1000, null)
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    private fun buildClient(): OkHttpClient {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val ctx = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(ctx.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }
}
