package cat.naval.xamanta.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

private const val BUFFER_BYTES = 64 * 1024

fun InputStream.sha256Hex(copyTo: OutputStream? = null, maxBytes: Long = Long.MAX_VALUE): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_BYTES)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("stream exceeds $maxBytes bytes")
        digest.update(buffer, 0, read)
        copyTo?.write(buffer, 0, read)
    }
    return digest.digest().toHexString()
}

fun String.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8)).toHexString()

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
