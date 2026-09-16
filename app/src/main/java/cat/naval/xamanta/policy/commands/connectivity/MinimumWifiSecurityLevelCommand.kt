package cat.naval.xamanta.policy.commands.connectivity

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.WifiSecurityLevel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MinimumWifiSecurityLevelCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "minimumWifiSecurityLevel"
    override val minSdk = Build.VERSION_CODES.TIRAMISU

    override fun execute() = scope.dpm.setMinimumRequiredWifiSecurityLevel(
        when (scope.policy.minimumWifiSecurityLevel) {
            WifiSecurityLevel.OPEN_NETWORK_SECURITY ->
                DevicePolicyManager.WIFI_SECURITY_OPEN

            WifiSecurityLevel.PERSONAL_NETWORK_SECURITY ->
                DevicePolicyManager.WIFI_SECURITY_PERSONAL

            WifiSecurityLevel.ENTERPRISE_NETWORK_SECURITY ->
                DevicePolicyManager.WIFI_SECURITY_ENTERPRISE_EAP

            WifiSecurityLevel.ENTERPRISE_BIT192_NETWORK_SECURITY ->
                DevicePolicyManager.WIFI_SECURITY_ENTERPRISE_192
        },
    )
}
