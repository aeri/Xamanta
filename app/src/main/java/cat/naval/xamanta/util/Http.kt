package cat.naval.xamanta.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

private const val POST_TIMEOUT_MS = 15_000
private const val DOWNLOAD_TIMEOUT_MS = 30_000

class OAuthException(val statusCode: Int, message: String) : IOException(message)

suspend fun postForJson(
    endpoint: String,
    body: String,
    contentType: String,
    authorization: String? = null,
    sslSocketFactory: SSLSocketFactory? = null,
): String = withContext(Dispatchers.IO) {
    val connection = URL(endpoint).openConnection() as? HttpsURLConnection
        ?: throw IOException("refusing a non-HTTPS enrollment endpoint")
    try {
        sslSocketFactory?.let { connection.sslSocketFactory = it }
        connection.requestMethod = "POST"
        authorization?.let { connection.setRequestProperty("Authorization", it) }
        connection.setRequestProperty("Content-Type", contentType)
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.connectTimeout = POST_TIMEOUT_MS
        connection.readTimeout = POST_TIMEOUT_MS
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val status = connection.responseCode
        if (status !in 200..299) {
            val detail = runCatching { connection.errorStream?.bufferedReader()?.readText() }
                .getOrNull()
            throw OAuthException(status, "HTTP $status${detail?.let { ": $it" }.orEmpty()}")
        }
        connection.inputStream.bufferedReader().readText()
    } finally {
        connection.disconnect()
    }
}

suspend fun downloadTo(url: String, target: File, maxBytes: Long): String =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as? HttpsURLConnection
            ?: throw IOException("refusing a non-HTTPS download URL")
        try {
            connection.connectTimeout = DOWNLOAD_TIMEOUT_MS
            connection.readTimeout = DOWNLOAD_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { source ->
                FileOutputStream(target).use { file ->
                    source.sha256Hex(copyTo = file, maxBytes = maxBytes)
                }
            }
        } finally {
            connection.disconnect()
        }
    }
