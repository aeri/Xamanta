package cat.naval.xamanta.policy

import android.content.Context
import android.os.Build
import android.util.Log
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.packages.PackageInstallerCoordinator
import cat.naval.xamanta.packages.SelfUpdater
import cat.naval.xamanta.policy.commands.buildDevicePolicyCommands
import cat.naval.xamanta.policy.commands.buildUserRestrictionCommands
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.applyAll
import cat.naval.xamanta.policy.kiosk.KioskManager
import cat.naval.xamanta.policy.reconcile.ApplicationReconciler
import cat.naval.xamanta.policy.reconcile.ApplicationSettingsReconciler
import cat.naval.xamanta.policy.reconcile.KioskReconciler
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.InstallationFailureReason
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.Policy
import cat.naval.xamanta.models.SelfUpdate

private const val TAG = "PolicyEnforcement"

private const val STEP_DEVICE_POLICIES = "devicePolicies"
private const val STEP_APPLICATIONS = "applications"
private const val STEP_APPLICATION_SETTINGS = "applicationSettings"
private const val STEP_KIOSK = "kiosk"

class PolicyEnforcement(
    context: Context,
    installer: PackageInstallerCoordinator,
    private val selfUpdater: SelfUpdater,
) {

    sealed interface Verdict {
        object InForce : Verdict

        object Working : Verdict

        data class Defiance(val details: List<NonComplianceDetail>) : Verdict
    }

    private val appContext = context.applicationContext
    private val dpm = appContext.dpm
    private val admin = appContext.dpcAdmin
    private val packages = ManagedPackages(appContext)
    private val policyStore = PolicyStore(appContext)
    private val applications = ApplicationReconciler(packages, installer, ::pinnedPackage)
    private val applicationSettings =
        ApplicationSettingsReconciler(dpm, admin, packages, policyStore)
    private val kiosk = KioskReconciler(appContext, packages, policyStore)

    fun reconcile(
        desired: Policy,
        forceKioskTransition: Boolean = false,
        kioskCrashLooping: Boolean = false,
    ): Verdict {
        Log.i(TAG, "reconcile begin name='${desired.name}' v${desired.version}")

        val details = mutableListOf<NonComplianceDetail>()
        details += step(NonCompliance.SELF_UPDATE) { reconcileSelfUpdate(desired.selfUpdate) }
        val updatingSelf = isSelfUpdateInFlight(desired.selfUpdate)

        if (!packages.isDeviceOwner()) {
            Log.e(TAG, "not device owner — refusing to apply the rest of the policy")
            return if (updatingSelf) Verdict.Working
            else Verdict.Defiance(listOf(NonCompliance.notDeviceOwner()) + details)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ResetPasswordToken.ensure(appContext)

        val policy = desired.devicePolicy
        details += step(STEP_DEVICE_POLICIES) { applyDevicePolicies(policy) }
        details += step(STEP_APPLICATIONS) { applications.reconcile(policy.applications) }
        details += step(STEP_APPLICATION_SETTINGS) { applicationSettings.reconcile(policy) }
        details += step(STEP_KIOSK) {
            kiosk.reconcile(
                policy,
                forceKioskTransition || applications.deferredByLockTask,
                kioskCrashLooping,
            )
        }

        val verdict = when {
            updatingSelf || applications.hasWorkInFlight(policy.applications) -> Verdict.Working
            details.isEmpty() -> Verdict.InForce
            else -> Verdict.Defiance(details)
        }
        Log.i(TAG, "reconcile end verdict=$verdict")
        return verdict
    }

    private fun pinnedPackage(): String? =
        policyStore.pinnedPackage()?.takeIf { KioskManager.isLockTaskActive(appContext) }

    private inline fun step(
        name: String,
        block: () -> List<NonComplianceDetail>,
    ): List<NonComplianceDetail> = runCatching(block).getOrElse {
        Log.e(TAG, "step '$name' threw — reported as non-compliance: ${it.message}", it)
        listOf(NonCompliance.invalidValue(name, it.message.orEmpty()))
    }

    private fun applyDevicePolicies(policy: DevicePolicy): List<NonComplianceDetail> {
        val scope = PolicyScope(appContext, policy, dpm, admin)
        val commands = buildUserRestrictionCommands(scope) + buildDevicePolicyCommands(scope)
        return commands.applyAll(TAG)
    }

    private fun reconcileSelfUpdate(update: SelfUpdate?): List<NonComplianceDetail> {
        if (update == null || !isSelfUpdatePending(update)) return emptyList()

        selfUpdater.failureReason()?.let { return listOf(selfUpdateFailed(it)) }
        if (selfUpdater.isInFlight()) return emptyList()

        Log.i(TAG, "self-update to v${update.versionCode} — starting")
        selfUpdater.ensure(update)
        return listOfNotNull(selfUpdater.failureReason()?.let { selfUpdateFailed(it) })
    }

    private fun isSelfUpdateInFlight(update: SelfUpdate?): Boolean =
        update != null && isSelfUpdatePending(update) &&
                (selfUpdater.isInFlight() || selfUpdater.isDeferred())

    private fun isSelfUpdatePending(update: SelfUpdate): Boolean =
        update.versionCode > packages.installedVersionCode(packages.ownPackageName)

    private fun selfUpdateFailed(reason: InstallationFailureReason) =
        NonCompliance.appNotUpdated(
            packageName = packages.ownPackageName,
            reason = reason,
            settingName = NonCompliance.SELF_UPDATE,
        )
}
