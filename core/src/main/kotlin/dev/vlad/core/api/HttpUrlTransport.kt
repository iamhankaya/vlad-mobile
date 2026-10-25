package dev.vlad.core.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 20_000
private const val MAX_BODY_BYTES = 4 * 1024 * 1024
private const val HTTP_ERROR = 400
private const val BUF_BYTES = 8192

/** `HttpURLConnection` ile ağ katmanı (Android ve JVM'de aynı). Yönlendirme izlenmez: imza yola bağlıdır. */
class HttpUrlTransport : Transport {
    override fun execute(request: HttpRequest): HttpResponse {
        val conn = URL(request.url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = request.method
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.instanceFollowRedirects = false
            request.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (request.body.isNotEmpty()) {
                conn.doOutput = true
                conn.outputStream.use { it.write(request.body) }
            }
            val status = conn.responseCode
            val stream = if (status >= HTTP_ERROR) conn.errorStream else conn.inputStream
            val body = stream?.use { it.readNBytesCapped() } ?: ByteArray(0)
            return HttpResponse(status, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun java.io.InputStream.readNBytesCapped(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(BUF_BYTES)
        while (true) {
            val n = read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > MAX_BODY_BYTES) throw IOException("yanıt çok büyük")
        }
        return out.toByteArray()
    }
}
