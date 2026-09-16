package cat.naval.xamanta

import cat.naval.xamanta.protos.RemarryParams
import org.junit.Assert.assertEquals
import org.junit.Test

class RemarryParamsCompatTest {

    @Test
    fun `a message still carrying the retired use_tls field parses`() {
        val host = "c2.example.org"
        val wire = buildList {
            add(0x12)
            add(host.length)
            addAll(host.toByteArray(Charsets.UTF_8).map { it.toInt() })
            add(0x18)
            add(8443 and 0x7F or 0x80)
            add(8443 shr 7)
            add(0x28)
            add(0x00)
        }.map { it.toByte() }.toByteArray()

        val params = RemarryParams.parseFrom(wire)

        assertEquals(host, params.host)
        assertEquals(8443, params.port)
    }
}
