package cat.naval.xamanta.policy.commands.application

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.os.Build
import cat.naval.xamanta.models.DelegatedScope

object DelegationScopes {

    @SuppressLint("InlinedApi")
    val PLATFORM: Map<DelegatedScope, String> = mapOf(
        DelegatedScope.CERT_INSTALL to DevicePolicyManager.DELEGATION_CERT_INSTALL,
        DelegatedScope.MANAGED_CONFIGURATIONS to DevicePolicyManager.DELEGATION_APP_RESTRICTIONS,
        DelegatedScope.BLOCK_UNINSTALL to DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL,
        DelegatedScope.PERMISSION_GRANT to DevicePolicyManager.DELEGATION_PERMISSION_GRANT,
        DelegatedScope.PACKAGE_ACCESS to DevicePolicyManager.DELEGATION_PACKAGE_ACCESS,
        DelegatedScope.ENABLE_SYSTEM_APP to DevicePolicyManager.DELEGATION_ENABLE_SYSTEM_APP,
        DelegatedScope.NETWORK_ACTIVITY_LOGS to DevicePolicyManager.DELEGATION_NETWORK_LOGGING,
        DelegatedScope.SECURITY_LOGS to DevicePolicyManager.DELEGATION_SECURITY_LOGGING,
        DelegatedScope.CERT_SELECTION to DevicePolicyManager.DELEGATION_CERT_SELECTION,
    )

    val MIN_SDK: Map<DelegatedScope, Int> = mapOf(
        DelegatedScope.CERT_INSTALL to Build.VERSION_CODES.O,
        DelegatedScope.MANAGED_CONFIGURATIONS to Build.VERSION_CODES.O,
        DelegatedScope.BLOCK_UNINSTALL to Build.VERSION_CODES.O,
        DelegatedScope.PERMISSION_GRANT to Build.VERSION_CODES.O,
        DelegatedScope.PACKAGE_ACCESS to Build.VERSION_CODES.O,
        DelegatedScope.ENABLE_SYSTEM_APP to Build.VERSION_CODES.O,
        DelegatedScope.NETWORK_ACTIVITY_LOGS to Build.VERSION_CODES.Q,
        DelegatedScope.SECURITY_LOGS to Build.VERSION_CODES.S,
        DelegatedScope.CERT_SELECTION to Build.VERSION_CODES.Q,
    )

    fun isSupported(scope: DelegatedScope): Boolean =
        Build.VERSION.SDK_INT >= (MIN_SDK[scope] ?: Int.MAX_VALUE)
}
