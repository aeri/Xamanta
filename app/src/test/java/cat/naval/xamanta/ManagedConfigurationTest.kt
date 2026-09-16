package cat.naval.xamanta

import cat.naval.xamanta.policy.commands.application.ManagedConfiguration
import cat.naval.xamanta.models.ManagedProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedConfigurationTest {

    @Test
    fun `a well-formed configuration has nothing to report`() {
        val properties = listOf(
            text("server", "https://example.test"),
            ManagedProperty(key = "offline", boolValue = false),
            ManagedProperty(key = "retries", intValue = 0),
            ManagedProperty(key = "tags", stringListValue = emptyList()),
            ManagedProperty(key = "nested", bundleValue = listOf(text("inner", "x"))),
            ManagedProperty(
                key = "rows",
                bundleArrayValue = listOf(listOf(text("a", "1")), listOf(text("b", "2"))),
            ),
        )

        assertNull(ManagedConfiguration.validate(properties))
    }

    @Test
    fun `falsy values are values`() {
        assertNull(ManagedConfiguration.validate(listOf(ManagedProperty("a", boolValue = false))))
        assertNull(ManagedConfiguration.validate(listOf(ManagedProperty("b", intValue = 0))))
        assertNull(ManagedConfiguration.validate(listOf(ManagedProperty("c", stringValue = ""))))
    }

    @Test
    fun `a blank key is refused`() {
        val fault = ManagedConfiguration.validate(listOf(text(" ", "value")))

        assertEquals("managed configuration has a blank key", fault)
    }

    @Test
    fun `a duplicate key is refused by name`() {
        val fault = ManagedConfiguration.validate(listOf(text("server", "a"), text("server", "b")))

        assertEquals("duplicate managed configuration key 'server'", fault)
    }

    @Test
    fun `a key repeated in a sibling bundle is not a duplicate`() {
        val properties = listOf(
            ManagedProperty(key = "first", bundleValue = listOf(text("name", "a"))),
            ManagedProperty(key = "second", bundleValue = listOf(text("name", "b"))),
        )

        assertNull(ManagedConfiguration.validate(properties))
    }

    @Test
    fun `an entry with no value is refused by name`() {
        val fault = ManagedConfiguration.validate(listOf(ManagedProperty(key = "server")))

        assertTrue(fault.orEmpty(), fault.orEmpty().contains("'server'"))
        assertTrue(fault.orEmpty(), fault.orEmpty().endsWith("found 0"))
    }

    @Test
    fun `an entry with two values is refused by name`() {
        val fault = ManagedConfiguration.validate(
            listOf(ManagedProperty(key = "server", stringValue = "a", intValue = 1))
        )

        assertTrue(fault.orEmpty(), fault.orEmpty().endsWith("found 2"))
    }

    @Test
    fun `nesting is bounded at eight levels`() {
        assertNull(ManagedConfiguration.validate(listOf(nest(8))))

        val fault = ManagedConfiguration.validate(listOf(nest(9)))
        assertTrue(fault.orEmpty(), fault.orEmpty().contains("nests deeper than 8 levels"))
    }

    @Test
    fun `a nested bundle is validated too`() {
        val fault = ManagedConfiguration.validate(
            listOf(ManagedProperty(key = "outer", bundleValue = listOf(ManagedProperty("inner"))))
        )

        assertTrue(fault.orEmpty(), fault.orEmpty().contains("'inner'"))
    }

    @Test
    fun `a bundle array member is validated too`() {
        val fault = ManagedConfiguration.validate(
            listOf(
                ManagedProperty(
                    key = "rows",
                    bundleArrayValue = listOf(listOf(text("ok", "1")), listOf(text("", "2"))),
                )
            )
        )

        assertEquals("managed configuration has a blank key", fault)
    }

    private fun text(key: String, value: String) = ManagedProperty(key = key, stringValue = value)

    private fun nest(levels: Int): ManagedProperty =
        (1 until levels).fold(text("leaf", "x")) { inner, level ->
            ManagedProperty(key = "level$level", bundleValue = listOf(inner))
        }
}
