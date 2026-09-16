package cat.naval.xamanta.policy.commands.connectivity

import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.ConfigureWifi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.M)
class WifiConfigsLockdownCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "configureWifi"
    override val minSdk = Build.VERSION_CODES.M

    override fun execute() {
        val lockdown = scope.policy.configureWifi == ConfigureWifi.DISALLOW_CONFIGURING_WIFI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scope.dpm.setConfiguredNetworksLockdownState(scope.admin, lockdown)
        } else {
            scope.dpm.setGlobalSetting(
                scope.admin,
                Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN,
                if (lockdown) "1" else "0",
            )
        }
    }
}
