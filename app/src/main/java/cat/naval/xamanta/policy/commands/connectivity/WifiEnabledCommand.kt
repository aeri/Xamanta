package cat.naval.xamanta.policy.commands.connectivity

import android.content.Context
import android.net.wifi.WifiManager
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class WifiEnabledCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "wifiEnabled"

    override fun execute() {
        val desired = scope.policy.wifiEnabled ?: return
        val wifi = scope.context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi == null) {
            deviceCannot("this device has no Wi-Fi", asked = true)
            return
        }
        @Suppress("DEPRECATION")
        if (wifi.isWifiEnabled != desired && !wifi.setWifiEnabled(desired)) {
            throw IllegalStateException("the platform refused to set Wi-Fi to $desired")
        }
    }
}
