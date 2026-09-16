package cat.naval.xamanta

import cat.naval.xamanta.system.unsealSecret
import org.junit.Assert.assertEquals
import org.junit.Test

class SecretsTest {

    @Test
    fun `a value stored in the clear reads back unchanged`() {
        assertEquals("a-client-secret", unsealSecret("a-client-secret"))
        assertEquals("""{"accessToken":"x"}""", unsealSecret("""{"accessToken":"x"}"""))
        assertEquals("", unsealSecret(""))
    }

    @Test
    fun `the sealed marker is only recognised at the front`() {
        assertEquals("secret v1: not sealed", unsealSecret("secret v1: not sealed"))
    }
}
