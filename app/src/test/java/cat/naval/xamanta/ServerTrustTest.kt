package cat.naval.xamanta

import cat.naval.xamanta.util.ServerTrust
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.GeneralSecurityException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class ServerTrustTest {

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/$name.der")) { "missing fixture $name.der" }
            .use { it.readBytes() }

    private fun chain(name: String): Array<X509Certificate> =
        arrayOf(
            CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(fixture(name))) as X509Certificate
        )

    @Test
    fun `a server certificate issued by the provisioned CA is accepted`() {
        ServerTrust.trustManager(fixture("provisioned-ca"))
            .checkServerTrusted(chain("provisioned-leaf"), "RSA")
    }

    @Test
    fun `a server certificate from any other CA is refused`() {
        val trust = ServerTrust.trustManager(fixture("provisioned-ca"))
        assertThrows(CertificateException::class.java) {
            trust.checkServerTrusted(chain("other-leaf"), "RSA")
        }
    }

    @Test
    fun `the provisioned CA is the only anchor`() {
        val anchors = ServerTrust.trustManager(fixture("provisioned-ca")).acceptedIssuers
        assertEquals(1, anchors.size)
        assertEquals("CN=Xamanta Test CA", anchors.single().subjectX500Principal.name)
    }

    @Test
    fun `a bundle anchors every certificate it carries`() {
        val trust = ServerTrust.trustManager(fixture("ca-bundle"))
        assertEquals(2, trust.acceptedIssuers.size)
        trust.checkServerTrusted(chain("provisioned-leaf"), "RSA")
        trust.checkServerTrusted(chain("other-leaf"), "RSA")
    }

    @Test
    fun `bytes that are not a certificate are refused rather than ignored`() {
        val garbage = "not a certificate at all".toByteArray()
        assertThrows(GeneralSecurityException::class.java) { ServerTrust.trustManager(garbage) }
        assertThrows(GeneralSecurityException::class.java) { ServerTrust.socketFactory(garbage) }
    }

    @Test
    fun `no provisioned CA leaves the platform anchors in place`() {
        assertNull(ServerTrust.socketFactory(null))
    }

    @Test
    fun `a provisioned CA yields a socket factory`() {
        assertNotNull(ServerTrust.socketFactory(fixture("provisioned-ca")))
    }

    @Test
    fun `a rejected chain is a trust failure however deeply it is wrapped`() {
        val handshake = SSLHandshakeException("handshake failed").initCause(
            CertificateException("Trust anchor for certification path not found.")
        )
        assertTrue(ServerTrust.isTrustFailure(handshake))
    }

    @Test
    fun `a name the certificate does not cover is a trust failure`() {
        assertTrue(ServerTrust.isTrustFailure(SSLPeerUnverifiedException("Hostname not verified")))
    }

    @Test
    fun `an interrupted connection is not a trust failure`() {
        assertFalse(ServerTrust.isTrustFailure(SocketTimeoutException("timeout")))
        assertFalse(ServerTrust.isTrustFailure(SSLException("Connection reset by peer")))
        assertFalse(ServerTrust.isTrustFailure(null))
    }

    @Test
    fun `a self-referencing cause terminates`() {
        val outer = IOException("outer")
        outer.initCause(IOException("inner").initCause(outer))
        assertFalse(ServerTrust.isTrustFailure(outer))
    }
}
