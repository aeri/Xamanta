package cat.naval.xamanta.util

import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object ServerTrust {

    fun socketFactory(der: ByteArray?): SSLSocketFactory? {
        val anchors = der ?: return null
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trustManager(anchors)), null)
        return context.socketFactory
    }

    fun trustManager(der: ByteArray): X509TrustManager {
        val anchors = der.parseX509Chain()
        if (anchors.isEmpty()) {
            throw GeneralSecurityException("the provisioned server CA holds no X.509 certificate")
        }

        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            anchors.forEachIndexed { index, anchor -> setCertificateEntry("server-ca-$index", anchor) }
        }
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(store) }

        return factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: throw GeneralSecurityException("no X509TrustManager for the provisioned server CA")
    }

    fun isTrustFailure(error: Throwable?): Boolean {
        var cause = error
        val seen = mutableSetOf<Throwable>()
        while (cause != null && seen.add(cause)) {
            when (cause) {
                is CertificateException,
                is CertPathValidatorException,
                is SSLPeerUnverifiedException -> return true
            }
            cause = cause.cause
        }
        return false
    }
}
