package com.himari.telatv.data

/**
 * Chave privada SSH colada (ou importada) pelo usuário. PURO (testado com kotlinc).
 * Ao colar no celular as quebras de linha costumam se perder ou virar \r\n; aqui a chave é
 * reconstruída em formato PEM limpo, ou a pessoa recebe um erro claro em pt-BR.
 */
object SshKey {
    private val PEM = Regex("""-----BEGIN ([A-Z0-9 ]+)-----(.*?)-----END \1-----""", RegexOption.DOT_MATCHES_ALL)

    /** Texto vazio = sem chave (null). Chave inválida lança [TvException]. */
    fun normalize(input: String): String? {
        val t = input.replace("\r\n", "\n").replace('\r', '\n').trim()
        if (t.isEmpty()) return null
        if (t.startsWith("ssh-") || t.startsWith("ecdsa-") || t.startsWith("sk-")) {
            throw TvException("Isso é a chave PÚBLICA (arquivo .pub). Cole a chave PRIVADA, que começa com -----BEGIN.")
        }
        val m = PEM.find(t) ?: throw TvException(
            "Chave privada inválida. Cole o arquivo inteiro, de -----BEGIN até -----END (sem o .pub)."
        )
        val type = m.groupValues[1]
        val body = m.groupValues[2]
        if (type.contains("ENCRYPTED") || body.contains("ENCRYPTED")) {
            throw TvException("A chave tem senha (passphrase), o app não suporta. Gere uma chave sem senha para a TV.")
        }
        val b64 = body.lines()
            .filterNot { it.contains(':') }          // cabeçalhos tipo "Proc-Type: ..."
            .joinToString("") { it.filterNot(Char::isWhitespace) }
        if (b64.isEmpty()) throw TvException("Chave privada vazia ou cortada. Cole o arquivo inteiro.")
        return "-----BEGIN $type-----\n" + b64.chunked(64).joinToString("\n") + "\n-----END $type-----\n"
    }
}
