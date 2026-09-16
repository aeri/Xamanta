package cat.naval.xamanta.policy.commands.connectivity

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.MissingPackageException
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException

@RequiresApi(Build.VERSION_CODES.N)
class AlwaysOnVpnPackageCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "alwaysOnVpnPackage"
    override val minSdk = Build.VERSION_CODES.N

    override fun execute() {
        val vpn = scope.policy.alwaysOnVpnPackage
        if (vpn == null) {
            scope.dpm.setAlwaysOnVpnPackage(scope.admin, null, false)
            return
        }
        val exempt = if (vpn.lockdownEnabled) exemptPackages() else emptySet()
        try {
            when {
                exempt.isEmpty() ->
                    scope.dpm.setAlwaysOnVpnPackage(
                        scope.admin,
                        vpn.packageName,
                        vpn.lockdownEnabled,
                    )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    scope.dpm.setAlwaysOnVpnPackage(scope.admin, vpn.packageName, true, exempt)

                else -> throw UnsupportedApiLevelException(
                    "lockdown exemptions require Android 10: ${exempt.joinToString()}"
                )
            }
        } catch (_: PackageManager.NameNotFoundException) {
            throw MissingPackageException(vpn.packageName)
        }
    }

    private fun exemptPackages(): Set<String> = scope.policy.applications
        .filter { it.alwaysOnVpnLockdownExempt }
        .map { it.packageName }
        .filter { isInstalled(it) }
        .toSet()

    private fun isInstalled(packageName: String): Boolean =
        runCatching { scope.context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
}
