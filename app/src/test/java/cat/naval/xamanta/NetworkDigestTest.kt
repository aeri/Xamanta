package cat.naval.xamanta

import cat.naval.xamanta.models.EapOuter
import cat.naval.xamanta.models.EapSettings
import cat.naval.xamanta.models.NetworkConfiguration
import cat.naval.xamanta.models.OncCertificate
import cat.naval.xamanta.models.OncCertificateType
import cat.naval.xamanta.models.WifiSecurity
import cat.naval.xamanta.models.WifiSettings
import cat.naval.xamanta.policy.commands.connectivity.digest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NetworkDigestTest {

    private fun psk(
        guid: String = "guid-1",
        ssid: String = "WLAN_IOT",
        passphrase: String = "hunter2",
        hidden: Boolean = false,
        autoConnect: Boolean = true,
    ) = NetworkConfiguration(
        guid = guid,
        wifi = WifiSettings(
            ssid = ssid,
            hiddenSsid = hidden,
            security = WifiSecurity.WPA_PSK,
            autoConnect = autoConnect,
            passphrase = passphrase,
        ),
    )

    @Test
    fun `the same network digests the same`() {
        assertEquals(psk().digest(emptyList()), psk().digest(emptyList()))
    }

    @Test
    fun `a new passphrase changes the digest`() {
        assertNotEquals(
            psk().digest(emptyList()),
            psk(passphrase = "hunter3").digest(emptyList()),
        )
    }

    @Test
    fun `a new ssid changes the digest`() {
        assertNotEquals(psk().digest(emptyList()), psk(ssid = "WLAN_OTHER").digest(emptyList()))
    }

    @Test
    fun `hiding the ssid changes the digest`() {
        assertNotEquals(psk().digest(emptyList()), psk(hidden = true).digest(emptyList()))
    }

    @Test
    fun `turning auto-connect off changes the digest`() {
        assertNotEquals(psk().digest(emptyList()), psk(autoConnect = false).digest(emptyList()))
    }

    @Test
    fun `an unreferenced certificate does not change the digest`() {
        val other = OncCertificate("ca-other", OncCertificateType.SERVER, x509 = "AAAA")
        assertEquals(psk().digest(emptyList()), psk().digest(listOf(other)))
    }

    private fun eap(caGuid: String = "ca-1") = NetworkConfiguration(
        guid = "guid-eap",
        wifi = WifiSettings(
            ssid = "WLAN_CORP",
            security = WifiSecurity.WPA_EAP,
            eap = EapSettings(
                outer = EapOuter.PEAP,
                identity = "device",
                domainSuffixMatch = listOf("naval.cat"),
                serverCaRefs = listOf(caGuid),
            ),
        ),
    )

    @Test
    fun `rotating a referenced certificate under the same guid changes the digest`() {
        val before = OncCertificate("ca-1", OncCertificateType.SERVER, x509 = "AAAA")
        val after = OncCertificate("ca-1", OncCertificateType.SERVER, x509 = "BBBB")
        assertNotEquals(eap().digest(listOf(before)), eap().digest(listOf(after)))
    }

    @Test
    fun `reordering the certificate list does not change the digest`() {
        val ca = OncCertificate("ca-1", OncCertificateType.SERVER, x509 = "AAAA")
        val other = OncCertificate("ca-0", OncCertificateType.SERVER, x509 = "CCCC")
        assertEquals(eap().digest(listOf(ca, other)), eap().digest(listOf(other, ca)))
    }
}
