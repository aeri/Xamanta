package cat.naval.xamanta.commands

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import cat.naval.xamanta.models.PreferenceConstants.ANSWERED_COMMANDS
import cat.naval.xamanta.protos.Command
import cat.naval.xamanta.protos.CommandFailureReason
import cat.naval.xamanta.protos.CommandResult
import cat.naval.xamanta.protos.CommandStatus
import cat.naval.xamanta.protos.CommandType
import cat.naval.xamanta.protos.DeviceInfo
import cat.naval.xamanta.system.getJson
import cat.naval.xamanta.system.policyPrefs
import cat.naval.xamanta.system.putJson
import cat.naval.xamanta.util.isoNow
import cat.naval.xamanta.util.parseIsoOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private const val TAG = "CommandEngine"

internal const val MAX_ANSWERED = 16

class CommandEngine(context: Context) {

    private val scope = CommandScope(context.applicationContext)
    private val answered = AnsweredCommands(context.applicationContext)

    suspend fun execute(command: Command): CommandOutcome =
        withContext(Dispatchers.IO + NonCancellable) { answer(command) }

    private suspend fun answer(command: Command): CommandOutcome {
        answered.resultFor(command.id)?.let {
            Log.i(TAG, "command ${command.id} already answered — resending the stored result")
            return CommandOutcome(it)
        }

        Log.i(TAG, "executing command id=${command.id} type=${command.type}")

        if (hasExpired(command, System.currentTimeMillis())) {
            return refuse(command, CommandFailureReason.EXPIRED, "expired before execution")
        }
        if (!scope.packages.isDeviceOwner()) {
            return refuse(command, CommandFailureReason.NOT_DEVICE_OWNER, "DPC is not device owner")
        }
        val device = buildCommand(scope, command) ?: return refuse(
            command,
            CommandFailureReason.UNKNOWN_COMMAND,
            "unrecognised command type: ${command.type}",
        )
        if (!device.isSupported) {
            return refuse(
                command,
                CommandFailureReason.COMMAND_API_LEVEL,
                "${device.name} needs API ${device.minSdk}..${device.maxSdk}, " +
                        "this device is API ${Build.VERSION.SDK_INT}",
            )
        }

        val failure = runCatching { device.execute() }.exceptionOrNull()
        if (failure != null) {
            Log.e(TAG, "command id=${command.id} failed: ${failure.message}", failure)
            val reason = failureReasonFor(failure)
            val message = failure.message ?: failure::class.simpleName ?: "unknown"
            return refuse(command, reason, message)
        }

        return record(command, success(command.id, device.deviceInfo), device.afterReported)
    }

    fun pendingResults(): List<CommandResult> = answered.pending()

    fun markResultSent(id: String) = answered.markReported(id)

    private fun refuse(
        command: Command,
        reason: CommandFailureReason,
        message: String,
    ): CommandOutcome {
        Log.w(TAG, "command id=${command.id} answered $reason — $message")
        val result = failed(command.id, reason, message)
        if (!reason.isVerdict) return CommandOutcome(result)
        return record(command, result, afterReported = null)
    }

    private fun record(
        command: Command,
        result: CommandResult,
        afterReported: (suspend () -> Unit)?,
    ): CommandOutcome {
        answered.remember(result, reported = false)
        if (afterReported == null) return CommandOutcome(result)
        return CommandOutcome(result) {
            if (command.type == CommandType.REMARRY) answered.clear()
            afterReported()
        }
    }

    private fun success(id: String, deviceInfo: DeviceInfo?): CommandResult =
        CommandResult.newBuilder()
            .setId(id)
            .setStatus(CommandStatus.SUCCESS)
            .setExecutedAt(isoNow())
            .apply { deviceInfo?.let { setDeviceInfo(it) } }
            .build()

    private fun failed(
        id: String,
        reason: CommandFailureReason,
        message: String,
    ): CommandResult = CommandResult.newBuilder()
        .setId(id)
        .setStatus(CommandStatus.FAILED)
        .setFailureReason(reason)
        .setFailureMessage(message)
        .setExecutedAt(isoNow())
        .build()
}

internal val CommandFailureReason.isVerdict: Boolean
    get() = this != CommandFailureReason.INTERNAL_ERROR &&
            this != CommandFailureReason.NOT_DEVICE_OWNER &&
            this != CommandFailureReason.EXPIRED

internal fun hasExpired(command: Command, nowMs: Long): Boolean {
    if (command.durationSeconds <= 0L) return false
    val issuedAtMs = parseIsoOrNull(command.issuedAt) ?: return false
    return nowMs > issuedAtMs + command.durationSeconds * 1000L
}

@Serializable
internal data class Answered(
    val id: String,
    val resultB64: String,
    val reported: Boolean,
)

internal fun List<Answered>.remembering(entry: Answered, max: Int = MAX_ANSWERED): List<Answered> {
    val entries = filterNot { it.id == entry.id } + entry
    if (entries.size <= max) return entries

    var toEvict = entries.size - max
    val kept = entries.filter { answered ->
        val evict = toEvict > 0 && answered.reported
        if (evict) toEvict--
        !evict
    }
    return if (kept.size <= max) kept else kept.takeLast(max)
}

private class AnsweredCommands(context: Context) {

    private val preferences = context.policyPrefs()

    private var entries: List<Answered>? = null

    @Synchronized
    fun resultFor(id: String): CommandResult? =
        read().firstOrNull { it.id == id }?.let { decode(it.resultB64) }

    @Synchronized
    fun remember(result: CommandResult, reported: Boolean) {
        write(read().remembering(Answered(result.id, encode(result), reported)))
    }

    @Synchronized
    fun markReported(id: String) {
        val entries = read()
        if (entries.none { it.id == id && !it.reported }) return
        write(entries.map { if (it.id == id) it.copy(reported = true) else it })
    }

    @Synchronized
    fun pending(): List<CommandResult> =
        read().filterNot { it.reported }.mapNotNull { decode(it.resultB64) }

    @Synchronized
    fun clear() {
        preferences.edit(commit = true) { remove(ANSWERED_COMMANDS) }
        entries = emptyList()
    }

    private fun read(): List<Answered> = entries
        ?: preferences.getJson<List<Answered>>(ANSWERED_COMMANDS).orEmpty().also { entries = it }

    private fun write(entries: List<Answered>) {
        preferences.edit(commit = true) { putJson(ANSWERED_COMMANDS, entries) }
        this.entries = entries
    }

    private fun encode(result: CommandResult): String =
        Base64.encodeToString(result.toByteArray(), Base64.NO_WRAP)

    private fun decode(value: String): CommandResult? =
        runCatching { CommandResult.parseFrom(Base64.decode(value, Base64.NO_WRAP)) }
            .onFailure { Log.e(TAG, "unreadable stored result: ${it.message}") }
            .getOrNull()
}
