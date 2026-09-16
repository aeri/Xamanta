package cat.naval.xamanta.commands

import android.content.Context
import android.os.Build
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.policy.engine.MissingPackageException
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException
import cat.naval.xamanta.protos.CommandFailureReason
import cat.naval.xamanta.protos.CommandResult
import cat.naval.xamanta.protos.DeviceInfo
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm

class CommandScope(val context: Context) {
    val dpm = context.dpm
    val admin = context.dpcAdmin
    val packages = ManagedPackages(context)
}

abstract class DeviceCommand {

    abstract val name: String

    open val minSdk: Int get() = Build.VERSION_CODES.LOLLIPOP

    open val maxSdk: Int get() = Int.MAX_VALUE

    val isSupported: Boolean get() = Build.VERSION.SDK_INT in minSdk..maxSdk

    abstract suspend fun execute()

    open val deviceInfo: DeviceInfo? get() = null

    open val afterReported: (suspend () -> Unit)? get() = null
}

class InvalidParamsException(message: String) : IllegalArgumentException(message)

fun failureReasonFor(failure: Throwable): CommandFailureReason = when (failure) {
    is InvalidParamsException -> CommandFailureReason.INVALID_PARAMS
    is MissingPackageException -> CommandFailureReason.PACKAGE_NOT_FOUND
    is UnsupportedApiLevelException -> CommandFailureReason.COMMAND_API_LEVEL
    else -> CommandFailureReason.INTERNAL_ERROR
}

class CommandOutcome(
    val result: CommandResult,
    val afterReported: (suspend () -> Unit)? = null,
)
