package cat.naval.xamanta.policy.reconcile

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.InstallType
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.policy.commands.application.DelegationScopes
import cat.naval.xamanta.policy.commands.buildApplicationCommands
import cat.naval.xamanta.policy.engine.ApplicationScope
import cat.naval.xamanta.policy.engine.applyAll
import cat.naval.xamanta.system.dpm

private const val TAG = "ApplicationSettings"

class ApplicationSettingsReconciler(
    private val dpm: DevicePolicyManager,
    private val admin: ComponentName,
    private val packages: ManagedPackages,
    private val store: PolicyStore,
) {

    fun reconcile(policy: DevicePolicy): List<NonComplianceDetail> {
        val managed = policy.applications.filter { it.isConfigurable() }
        val managedNames = managed.map { it.packageName }.toSet()

        val applied = managed.flatMap { apply(it) }
        val withdrawals = (rememberedPackages() - managedNames)
            .filterNot { it == admin.packageName }
            .associateWith { withdraw(it) }

        val unfinished = withdrawals.filterValues { it.isNotEmpty() }.keys
        store.setManagedApplicationPackages(managedNames + unfinished)
        return applied + withdrawals.values.flatten()
    }

    private fun ApplicationPolicy.isConfigurable(): Boolean =
        installType != InstallType.BLOCKED &&
                packageName != admin.packageName &&
                packages.isInstalled(packageName)

    private fun apply(app: ApplicationPolicy): List<NonComplianceDetail> =
        buildApplicationCommands(scopeFor(app)).applyAll(TAG, app.packageName)

    private fun withdraw(packageName: String): List<NonComplianceDetail> {
        if (!packages.isInstalled(packageName)) return emptyList()
        Log.i(TAG, "[$packageName] no longer configured by the policy — withdrawing its settings")
        val empty = ApplicationPolicy(packageName = packageName)
        return buildApplicationCommands(scopeFor(empty)).applyAll(TAG, packageName)
    }

    private fun scopeFor(app: ApplicationPolicy) = ApplicationScope(app, dpm, admin, packages)

    private fun rememberedPackages(): Set<String> =
        store.managedApplicationPackages() + currentDelegates()

    private fun currentDelegates(): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) delegatePackages() else emptySet()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun delegatePackages(): Set<String> = DelegationScopes.PLATFORM.values
        .flatMap { scope ->
            runCatching { dpm.getDelegatePackages(admin, scope) }.getOrNull().orEmpty()
        }
        .toSet()
}
