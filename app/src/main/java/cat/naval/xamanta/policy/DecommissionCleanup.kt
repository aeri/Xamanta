package cat.naval.xamanta.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import cat.naval.xamanta.enrollment.CaCertInstaller
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.policy.commands.buildApplicationCommands
import cat.naval.xamanta.policy.commands.buildDevicePolicyCommands
import cat.naval.xamanta.policy.commands.managedUserRestrictions
import cat.naval.xamanta.policy.engine.ApplicationScope
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.applyAll
import cat.naval.xamanta.policy.kiosk.KioskManager
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.DevicePolicy

private const val TAG = "DecommissionCleanup"

object DecommissionCleanup {

    fun run(context: Context) {
        val appContext = context.applicationContext
        val dpm = appContext.dpm
        val admin = appContext.dpcAdmin

        step("exit kiosk") {
            KioskManager.disableKioskMode(appContext)
            dpm.setLockTaskPackages(admin, emptyArray())
        }
        step("release applications") {
            ManagedPackages(appContext).releaseAll()
        }
        step("reset application settings") {
            resetApplicationSettings(appContext, dpm, admin)
        }
        step("clear user restrictions") {
            managedUserRestrictions().forEach { dpm.clearUserRestriction(admin, it) }
        }
        step("reset device policies") {
            buildDevicePolicyCommands(PolicyScope(appContext, DevicePolicy(id = ""), dpm, admin))
                .applyAll(TAG)
        }
        step("remove server CA certificate") {
            CaCertInstaller.removeIfPresent(appContext)
        }
        step("revoke reset password token") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ResetPasswordToken.clear(appContext)
        }
    }

    private fun resetApplicationSettings(
        appContext: Context,
        dpm: DevicePolicyManager,
        admin: ComponentName,
    ) {
        val store = PolicyStore(appContext)
        val packages = ManagedPackages(appContext)
        val lastPolicy = store.load()?.devicePolicy ?: DevicePolicy(id = "")
        val named = lastPolicy.applications.map { it.packageName }
        (named + store.managedApplicationPackages())
            .distinct()
            .filterNot { it == admin.packageName }
            .filter { packages.isInstalled(it) }
            .forEach { packageName ->
                val scope = ApplicationScope(
                    ApplicationPolicy(packageName = packageName),
                    dpm,
                    admin,
                    packages,
                )
                buildApplicationCommands(scope).applyAll(TAG, packageName)
            }
        store.setManagedApplicationPackages(emptySet())
    }

    private fun step(name: String, block: () -> Unit) {
        runCatching(block).onFailure { Log.e(TAG, "step '$name' failed: ${it.message}") }
    }
}
