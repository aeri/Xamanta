package cat.naval.xamanta

import cat.naval.xamanta.commands.Answered
import cat.naval.xamanta.commands.InvalidParamsException
import cat.naval.xamanta.commands.MAX_ANSWERED
import cat.naval.xamanta.commands.failureReasonFor
import cat.naval.xamanta.commands.hasExpired
import cat.naval.xamanta.commands.isVerdict
import cat.naval.xamanta.commands.remembering
import cat.naval.xamanta.policy.engine.MissingPackageException
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException
import cat.naval.xamanta.protos.Command
import cat.naval.xamanta.protos.CommandFailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ISSUED_AT = "2026-01-01T00:00:00Z"
private const val ISSUED_AT_MS = 1767225600000L

private fun command(issuedAt: String = ISSUED_AT, durationSeconds: Long = 0L): Command =
    Command.newBuilder()
        .setId("id")
        .setIssuedAt(issuedAt)
        .setDurationSeconds(durationSeconds)
        .build()

class CommandEngineTest {

    @Test
    fun `a command with no duration never expires`() {
        assertFalse(hasExpired(command(durationSeconds = 0L), nowMs = Long.MAX_VALUE))
    }

    @Test
    fun `a command inside its window has not expired`() {
        val subject = command(durationSeconds = 60L)

        assertFalse(hasExpired(subject, nowMs = ISSUED_AT_MS))
        assertFalse(hasExpired(subject, nowMs = ISSUED_AT_MS + 60_000L))
    }

    @Test
    fun `a command past its window has expired`() {
        assertTrue(hasExpired(command(durationSeconds = 60L), nowMs = ISSUED_AT_MS + 60_001L))
    }

    @Test
    fun `an unreadable issued_at never expires`() {
        assertFalse(hasExpired(command(issuedAt = "yesterday", durationSeconds = 1L), Long.MAX_VALUE))
        assertFalse(hasExpired(command(issuedAt = "", durationSeconds = 1L), Long.MAX_VALUE))
    }

    @Test
    fun `each failure names the reason the server acts on`() {
        assertEquals(
            CommandFailureReason.INVALID_PARAMS,
            failureReasonFor(InvalidParamsException("no password")),
        )
        assertEquals(
            CommandFailureReason.PACKAGE_NOT_FOUND,
            failureReasonFor(MissingPackageException("com.example.app")),
        )
        assertEquals(
            CommandFailureReason.COMMAND_API_LEVEL,
            failureReasonFor(UnsupportedApiLevelException("too old")),
        )
    }

    @Test
    fun `an unexpected throw is an internal error`() {
        assertEquals(
            CommandFailureReason.INTERNAL_ERROR,
            failureReasonFor(IllegalStateException("clearDeviceOwnerApp failed")),
        )
    }

    @Test
    fun `a verdict is remembered, a device failing right now is not`() {
        assertTrue(CommandFailureReason.INVALID_PARAMS.isVerdict)
        assertTrue(CommandFailureReason.PACKAGE_NOT_FOUND.isVerdict)
        assertTrue(CommandFailureReason.COMMAND_API_LEVEL.isVerdict)
        assertTrue(CommandFailureReason.UNKNOWN_COMMAND.isVerdict)

        assertFalse(CommandFailureReason.INTERNAL_ERROR.isVerdict)
        assertFalse(CommandFailureReason.NOT_DEVICE_OWNER.isVerdict)
        assertFalse(CommandFailureReason.EXPIRED.isVerdict)
    }

    @Test
    fun `the record keeps the newest answers and drops the oldest`() {
        val full = (1..MAX_ANSWERED).fold(emptyList<Answered>()) { entries, i ->
            entries.remembering(Answered("id-$i", "result-$i", reported = true))
        }

        val rolled = full.remembering(Answered("id-new", "result-new", reported = false))

        assertEquals(MAX_ANSWERED, rolled.size)
        assertEquals("id-2", rolled.first().id)
        assertEquals("id-new", rolled.last().id)
    }

    @Test
    fun `an answer still owed is never evicted, however old`() {
        val owed = Answered("id-owed", "result-owed", reported = false)
        val full = (1 until MAX_ANSWERED).fold(listOf(owed)) { entries, i ->
            entries.remembering(Answered("id-$i", "result-$i", reported = true))
        }

        val rolled = full.remembering(Answered("id-new", "result-new", reported = false))

        assertEquals(MAX_ANSWERED, rolled.size)
        assertEquals("id-owed", rolled.first().id)
        assertFalse(rolled.any { it.id == "id-1" })
        assertEquals("id-new", rolled.last().id)
    }

    @Test
    fun `the cap holds when nothing is evictable`() {
        val full = (1..MAX_ANSWERED).fold(emptyList<Answered>()) { entries, i ->
            entries.remembering(Answered("id-$i", "result-$i", reported = false))
        }

        val rolled = full.remembering(Answered("id-new", "result-new", reported = false))

        assertEquals(MAX_ANSWERED, rolled.size)
        assertEquals("id-2", rolled.first().id)
        assertEquals("id-new", rolled.last().id)
    }

    @Test
    fun `answering the same id again replaces its entry`() {
        val entries = listOf(Answered("a", "first", reported = false))
            .remembering(Answered("b", "other", reported = true))
            .remembering(Answered("a", "second", reported = true))

        assertEquals(listOf("b", "a"), entries.map { it.id })
        assertEquals("second", entries.last().resultB64)
        assertTrue(entries.last().reported)
    }
}
