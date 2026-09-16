package cat.naval.xamanta.policy.engine

import android.os.Build
import android.util.Log
import cat.naval.xamanta.models.NonComplianceDetail

abstract class PolicyCommand {
    abstract val name: String
    open val minSdk: Int get() = Build.VERSION_CODES.LOLLIPOP

    abstract fun execute()

    open val details: List<NonComplianceDetail> get() = emptyList()

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= minSdk

    open val asked: Boolean get() = false

    protected fun deviceCannot(reason: String, asked: Boolean) {
        if (asked) throw UnsupportedDeviceException(reason)
    }
}

class UnsupportedManagementModeException(message: String) : IllegalStateException(message)

class UnsupportedApiLevelException(message: String) : IllegalStateException(message)

class UnsupportedDeviceException(message: String) : IllegalStateException(message)

class MissingPackageException(val packageName: String) :
    IllegalStateException("package not installed: $packageName")

fun List<PolicyCommand>.applyAll(
    tag: String,
    packageName: String? = null,
): List<NonComplianceDetail> = flatMap { command ->
    if (!command.isSupported) {
        if (!command.asked) return@flatMap emptyList()
        val tooOld = UnsupportedApiLevelException(
            "requires Android API ${command.minSdk}, device is on API ${Build.VERSION.SDK_INT}"
        )
        return@flatMap listOf(NonCompliance.forFailure(command, tooOld, packageName))
    }
    val failure = runCatching { command.execute() }.exceptionOrNull()
        ?: return@flatMap command.details
    val about = if (packageName == null) "" else "[$packageName] "
    Log.e(tag, "${about}failed to apply '${command.name}': ${failure.message}")
    listOf(NonCompliance.forFailure(command, failure, packageName))
}
