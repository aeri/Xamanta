package cat.naval.xamanta

import cat.naval.xamanta.enrollment.RegistrationClient
import cat.naval.xamanta.enrollment.provisioningProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProvisioningPayloadTest {

    @Test
    fun `an https endpoint on a port in range is accepted`() {
        assertNull(provisioningProblem("https://mdm.example.org/connect/token", 8443, null))
        assertNull(provisioningProblem("HTTPS://mdm.example.org/connect/token", 443, ""))
        assertNull(provisioningProblem("https://10.0.2.2:8443/connect/token", 1, null))
    }

    @Test
    fun `an endpoint that is not https is refused`() {
        assertNotNull(provisioningProblem("http://mdm.example.org/connect/token", 8443, null))
        assertNotNull(provisioningProblem("ftp://mdm.example.org/token", 8443, null))
        assertNotNull(provisioningProblem("mdm.example.org/connect/token", 8443, null))
    }

    @Test
    fun `an https URL without a host is refused`() {
        assertNotNull(provisioningProblem("https://", 8443, null))
        assertNotNull(provisioningProblem("https:///connect/token", 8443, null))
    }

    @Test
    fun `a port outside the TCP range is refused`() {
        assertNotNull(provisioningProblem("https://mdm.example.org/connect/token", 0, null))
        assertNotNull(provisioningProblem("https://mdm.example.org/connect/token", -1, null))
        assertNotNull(provisioningProblem("https://mdm.example.org/connect/token", 65536, null))
        assertNull(provisioningProblem("https://mdm.example.org/connect/token", 65535, null))
    }

    @Test
    fun `the register endpoint is the token endpoint's sibling`() {
        assertEquals(
            "https://mdm.example.org/connect/register",
            RegistrationClient.registerEndpointFrom("https://mdm.example.org/connect/token"),
        )
        assertEquals(
            "https://mdm.example.org/connect/register",
            RegistrationClient.registerEndpointFrom("https://mdm.example.org/connect/token/"),
        )
    }
}
