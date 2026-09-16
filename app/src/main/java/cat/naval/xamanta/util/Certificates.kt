package cat.naval.xamanta.util

import android.util.Base64
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

fun String.decodeBase64(): ByteArray? = runCatching {
    val body = replace(PEM_ARMOUR, "").replace(WHITESPACE, "")
    Base64.decode(body, Base64.DEFAULT)
}.getOrNull()

private val PEM_ARMOUR = "-----(BEGIN|END)[^-]*-----".toRegex()
private val WHITESPACE = "\\s".toRegex()

fun ByteArray.parseX509Chain(): List<X509Certificate> =
    CertificateFactory.getInstance("X.509")
        .generateCertificates(ByteArrayInputStream(this))
        .filterIsInstance<X509Certificate>()
