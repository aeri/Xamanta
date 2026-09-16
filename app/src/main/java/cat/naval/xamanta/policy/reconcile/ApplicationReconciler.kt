package cat.naval.xamanta.policy.reconcile

import android.util.Log
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.packages.PackageInstallerCoordinator
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.InstallType
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.policy.engine.NonCompliance

private const val TAG = "ApplicationReconciler"

private val REQUIRED_TYPES =
    setOf(InstallType.INSTALLED, InstallType.FORCE_INSTALLED, InstallType.KIOSK)

class ApplicationReconciler(
    private val packages: ManagedPackages,
    private val installer: PackageInstallerCoordinator,
    private val pinnedPackage: () -> String?,
) {

    var deferredByLockTask = false
        private set

    var deferredByLockedStorage = false
        private set

    fun reconcile(desired: List<ApplicationPolicy>): List<NonComplianceDetail> =
        removeUnwanted(desired) + desired.flatMap { enforce(it) }

    fun hasWorkInFlight(desired: List<ApplicationPolicy>): Boolean =
        deferredByLockTask ||
                deferredByLockedStorage ||
                installer.hasUninstallsInFlight() ||
                desired.any {
                    it.installType in REQUIRED_TYPES && installer.isInFlight(it.packageName)
                }

    private fun removeUnwanted(desired: List<ApplicationPolicy>): List<NonComplianceDetail> {
        val keep = desired
            .filterNot { it.installType == InstallType.BLOCKED }
            .map { it.packageName }
            .toSet()
        return packages.userInstalledPackages()
            .filterNot { it in keep }
            .mapNotNull { startUninstall(it) }
    }

    private fun startUninstall(packageName: String): NonComplianceDetail? {
        if (installer.uninstallFailed(packageName)) {
            return NonCompliance.appStillInstalled(packageName)
        }
        if (installer.isUninstallInFlight(packageName)) return null

        if (packageName == pinnedPackage()) {
            Log.i(TAG, "[$packageName] pinned by lock task — removal deferred to the next pass")
            deferredByLockTask = true
            return null
        }

        Log.i(TAG, "[$packageName] not allowed by policy — uninstalling")
        installer.ensureUninstall(packageName) { packages.uninstall(packageName, it) }
        return NonCompliance.appStillInstalled(packageName)
            .takeIf { installer.uninstallFailed(packageName) }
    }

    private fun enforce(app: ApplicationPolicy): List<NonComplianceDetail> =
        when (app.installType) {
            InstallType.BLOCKED -> block(app.packageName)
            InstallType.AVAILABLE,
            InstallType.INSTALL_TYPE_UNSPECIFIED -> makeAvailable(app.packageName)

            InstallType.INSTALLED,
            InstallType.FORCE_INSTALLED,
            InstallType.KIOSK -> require(app)
        }

    private fun block(packageName: String): List<NonComplianceDetail> {
        if (packages.isSystemApp(packageName)) {
            packages.setUninstallBlocked(packageName, false)
            packages.setHidden(packageName, true)
        }
        return emptyList()
    }

    private fun makeAvailable(packageName: String): List<NonComplianceDetail> {
        if (!packages.isInstalled(packageName)) {
            if (!packages.isSystemApp(packageName)) return emptyList()
            packages.enableSystemApp(packageName)
        }
        packages.setHidden(packageName, false)
        return emptyList()
    }

    private fun require(app: ApplicationPolicy): List<NonComplianceDetail> {
        val packageName = app.packageName
        if (!packages.isInstalled(packageName) && packages.isSystemApp(packageName)) {
            packages.enableSystemApp(packageName)
            packages.setHidden(packageName, false)
            if (!packages.isInstalled(packageName)) {
                Log.w(TAG, "[$packageName] system app could not be enabled")
                return listOf(NonCompliance.appNotInstalled(packageName))
            }
        }
        if (!packages.isInstalled(packageName)) return listOfNotNull(startInstall(app))

        packages.setHidden(packageName, false)
        packages.setUninstallBlocked(packageName, app.installType != InstallType.INSTALLED)
        return listOfNotNull(startUpdateIfOutdated(app))
    }

    private fun startInstall(app: ApplicationPolicy): NonComplianceDetail? {
        val packageName = app.packageName
        installer.failureReason(packageName)?.let {
            return NonCompliance.appNotInstalled(packageName, it)
        }
        if (installer.isInFlight(packageName)) return null

        if (!installer.canStageDownloads()) return deferToUnlock(packageName, "install")

        Log.i(TAG, "[$packageName] installing from ${app.downloadUrl}")
        installer.ensureDownload(app)
        return installer.failureReason(packageName)
            ?.let { NonCompliance.appNotInstalled(packageName, it) }
    }

    private fun startUpdateIfOutdated(app: ApplicationPolicy): NonComplianceDetail? {
        val minimum = app.minimumVersionCode
        if (minimum <= 0L) return null
        val packageName = app.packageName
        val installed = packages.installedVersionCode(packageName)
        if (installed >= minimum) return null

        installer.failureReason(packageName)?.let {
            return NonCompliance.appNotUpdated(packageName, it)
        }
        if (installer.isInFlight(packageName)) return null

        if (!installer.canStageDownloads()) return deferToUnlock(packageName, "update")

        Log.i(TAG, "[$packageName] updating v$installed → v$minimum")
        installer.ensureDownload(app, installAboveVersion = installed)
        return installer.failureReason(packageName)
            ?.let { NonCompliance.appNotUpdated(packageName, it) }
    }

    private fun deferToUnlock(packageName: String, what: String): NonComplianceDetail? {
        Log.i(TAG, "[$packageName] $what deferred — storage is locked until the first unlock")
        deferredByLockedStorage = true
        return null
    }
}
