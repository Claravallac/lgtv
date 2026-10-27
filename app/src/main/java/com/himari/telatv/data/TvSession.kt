package com.himari.telatv.data

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TvException(message: String) : Exception(message)

/**
 * Conversa SSAP sobre um canal qualquer: quem tem a rede (net/TvClient) chama
 * [onMessage]/[onClosed] e fornece [send]. Lógica PURA (testada com kotlinc).
 * Bloqueante: chamar fora da thread principal.
 */
class TvSession(private val send: (String) -> Boolean) {
    private val inbox = LinkedBlockingQueue<Ssap.Reply>()
    private val counter = AtomicInteger()

    fun onMessage(raw: String) { inbox.put(Ssap.parse(raw)) }

    fun onClosed(reason: String?) {
        inbox.put(Ssap.Reply("closed", null, null, null, null, reason))
    }

    /**
     * Pareia (1ª vez: a TV mostra o pedido na tela) ou reautentica com [clientKey].
     * Devolve a chave a guardar.
     */
    fun register(clientKey: String?, timeoutMs: Long = 60_000): String {
        if (!send(Ssap.registerMessage("register_0", clientKey)))
            throw TvException("Não consegui falar com a TV (conexão fechada).")
        val r = await(timeoutMs) { it.type == "registered" || it.type == "error" || it.type == "closed" }
            ?: throw TvException("A TV não confirmou o pareamento a tempo. Aceite o pedido na tela da TV e tente de novo.")
        return when (r.type) {
            "registered" -> r.clientKey ?: clientKey
                ?: throw TvException("A TV aceitou, mas não enviou a chave de pareamento.")
            "error" -> throw TvException(friendly(r.error))
            else -> throw TvException("A TV encerrou a conexão." + (r.error?.let { " ($it)" } ?: ""))
        }
    }

    /** Envia um comando e espera a resposta com o mesmo id. */
    fun request(uri: String, payloadJson: String? = null, timeoutMs: Long = 6_000): Ssap.Reply {
        val id = "req_${counter.incrementAndGet()}"
        if (!send(Ssap.requestMessage(id, uri, payloadJson)))
            throw TvException("A conexão com a TV caiu.")
        val r = await(timeoutMs) { it.id == id || it.type == "closed" }
            ?: throw TvException("A TV não respondeu ao comando.")
        return when {
            r.type == "closed" -> throw TvException("A TV encerrou a conexão." + (r.error?.let { " ($it)" } ?: ""))
            r.type == "error" -> throw TvException(friendly(r.error))
            r.returnValue == false -> throw TvException(r.error ?: "A TV recusou o comando.")
            else -> r
        }
    }

    private fun await(timeoutMs: Long, match: (Ssap.Reply) -> Boolean): Ssap.Reply? {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (true) {
            val left = deadline - System.nanoTime()
            if (left <= 0) return null
            val r = inbox.poll(left, TimeUnit.NANOSECONDS) ?: return null
            if (match(r)) return r
        }
    }

    companion object {
        fun friendly(error: String?): String = when {
            error == null -> "A TV retornou um erro desconhecido."
            error.startsWith("401") -> "A TV negou a permissão (401). Apague o pareamento e pareie de novo."
            else -> "A TV retornou erro: $error"
        }
    }
}
