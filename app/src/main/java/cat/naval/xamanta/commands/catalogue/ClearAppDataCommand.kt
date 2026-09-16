package cat.naval.xamanta.commands.catalogue

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.commands.InvalidParamsException
import cat.naval.xamanta.policy.engine.MissingPackageException
import cat.naval.xamanta.protos.ClearAppDataParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "ClearAppDataCommand"

private const val CLEARED_TIMEOUT_MS = 30_000L

@RequiresApi(Build.VERSION_CODES.P)
internal class ClearAppDataCommand(
    private val scope: CommandScope,
    private val params: ClearAppDataParams?,
) : DeviceCommand() {
    override val name = "clearAppData"
    override val minSdk = Build.VERSION_CODES.P

    override suspend fun execute() {
        val packageName = params?.packageName
        if (packageName.isNullOrBlank()) {
            throw InvalidParamsException("clear_app_data_params.package_name is required")
        }
        if (!scope.packages.isInstalled(packageName)) throw MissingPackageException(packageName)

        val protectedPackages =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) liftUserControl(packageName)
            else null

        val cleared = withTimeoutOrNull(CLEARED_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                scope.dpm.clearApplicationUserData(
                    scope.admin,
                    packageName,
                    { it.run() },
                ) { name, ok ->
                    Log.i(TAG, "cleared data of $name ok=$ok")
                    restoreUserControl(protectedPackages)
                    if (continuation.isActive) continuation.resume(ok)
                }
            }
        }

        if (cleared == null) {
            restoreUserControl(protectedPackages)
            error("clearApplicationUserData did not answer within ${CLEARED_TIMEOUT_MS}ms")
        }
        if (!cleared) error("clearApplicationUserData reported failure for $packageName")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun liftUserControl(packageName: String): List<String>? =
        scope.dpm.getUserControlDisabledPackages(scope.admin).takeIf { packageName in it }
            ?.also { scope.dpm.setUserControlDisabledPackages(scope.admin, it - packageName) }

    private fun restoreUserControl(packages: List<String>?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || packages == null) return
        if (scope.dpm.getUserControlDisabledPackages(scope.admin) == packages) return
        scope.dpm.setUserControlDisabledPackages(scope.admin, packages)
    }
}
