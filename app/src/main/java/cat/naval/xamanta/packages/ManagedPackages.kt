package cat.naval.xamanta.packages

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import cat.naval.xamanta.system.PackageIdentity
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.isDeviceOwner
import cat.naval.xamanta.models.PermissionGrant
import cat.naval.xamanta.models.PermissionPolicy

private const val TAG = "ManagedPackages"

class ManagedPackages(context: Context) {

    private val appContext = context.applicationContext
    private val dpm = appContext.dpm
    private val pm: PackageManager = appContext.packageManager
    private val admin = appContext.dpcAdmin

    val ownPackageName: String get() = appContext.packageName

    fun isDeviceOwner(): Boolean = appContext.isDeviceOwner()

    fun isInstalled(packageName: String): Boolean = runCatching {
        pm.getPackageInfo(packageName, 0)
    }.isSuccess

    fun isSystemApp(packageName: String): Boolean = runCatching {
        pm.getApplicationInfo(packageName, uninstalledPackageFlags()).isSystem()
    }.getOrDefault(false)

    fun installedVersionCode(packageName: String): Long =
        PackageIdentity.installedVersionCode(pm, packageName)

    fun userInstalledPackages(): List<String> =
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filterNot { it.isSystem() || it.packageName == ownPackageName }
            .map { it.packageName }

    fun setHidden(packageName: String, hidden: Boolean) = attempt(packageName, "setHidden") {
        if (dpm.isApplicationHidden(admin, packageName) != hidden) {
            dpm.setApplicationHidden(admin, packageName, hidden)
        }
    }

    fun setUninstallBlocked(packageName: String, blocked: Boolean) =
        attempt(packageName, "setUninstallBlocked") {
            if (dpm.isUninstallBlocked(admin, packageName) != blocked) {
                dpm.setUninstallBlocked(admin, packageName, blocked)
            }
        }

    fun releaseAll() {
        pm.getInstalledApplications(uninstalledPackageFlags()).forEach {
            setHidden(it.packageName, false)
            setUninstallBlocked(it.packageName, false)
        }
    }

    fun enableSystemApp(packageName: String) = attempt(packageName, "enableSystemApp") {
        dpm.enableSystemApp(admin, packageName)
    }

    fun uninstall(packageName: String, sender: IntentSender) = attempt(packageName, "uninstall") {
        if (dpm.isUninstallBlocked(admin, packageName)) {
            dpm.setUninstallBlocked(admin, packageName, false)
        }
        pm.packageInstaller.uninstall(packageName, sender)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun applyPermissionGrant(packageName: String, grant: PermissionGrant): Boolean = runCatching {
        if (permissionGrantState(packageName, grant.permission) == grant.state()) return@runCatching true
        dpm.setPermissionGrantState(admin, packageName, grant.permission, grant.state())
    }.getOrDefault(false)

    @SuppressLint("InlinedApi")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun permissionGrantState(packageName: String, permission: String): Int = runCatching {
        dpm.getPermissionGrantState(admin, packageName, permission)
    }.getOrDefault(DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT)

    fun dangerousPermissions(packageName: String): List<String> = runCatching {
        pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .filter { isDangerous(it) }
    }.getOrDefault(emptyList())

    @Suppress("DEPRECATION")
    private fun isDangerous(permission: String): Boolean = runCatching {
        val level = pm.getPermissionInfo(permission, 0).protectionLevel
        (level and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
    }.getOrDefault(false)

    private fun attempt(packageName: String, action: String, block: () -> Unit): Boolean =
        runCatching(block)
            .onFailure { Log.e(TAG, "[$packageName] $action failed: ${it.message}") }
            .isSuccess

    private fun ApplicationInfo.isSystem(): Boolean =
        (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0

    @SuppressLint("InlinedApi")
    private fun PermissionGrant.state(): Int = when (policy) {
        PermissionPolicy.GRANT -> DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
        PermissionPolicy.DENY -> DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
        PermissionPolicy.PROMPT -> DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT
    }

    @Suppress("DEPRECATION")
    private fun uninstalledPackageFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
        } else {
            PackageManager.GET_UNINSTALLED_PACKAGES or PackageManager.GET_DISABLED_COMPONENTS
        }
}
