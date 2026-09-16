package cat.naval.xamanta.policy.commands.connectivity

import android.net.wifi.WifiSsid
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.WifiSsidPolicyType
import android.app.admin.WifiSsidPolicy as PlatformWifiSsidPolicy
import cat.naval.xamanta.models.WifiSsidPolicy as ModelWifiSsidPolicy

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class WifiSsidPolicyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "wifiSsidPolicy"
    override val minSdk = Build.VERSION_CODES.TIRAMISU

    override fun execute() {
        val policy = scope.policy.wifiSsidPolicy
        if (policy != null && policy.ssids.any { it.isEmpty() }) {
            throw IllegalArgumentException("a Wi-Fi SSID policy must not contain an empty SSID")
        }
        if (policy?.type == WifiSsidPolicyType.WIFI_SSID_ALLOWLIST && policy.ssids.isEmpty()) {
            throw IllegalArgumentException("a Wi-Fi SSID allowlist must not be empty")
        }
        scope.dpm.setWifiSsidPolicy(policy?.toPlatform())
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun ModelWifiSsidPolicy.toPlatform(): PlatformWifiSsidPolicy = PlatformWifiSsidPolicy(
    when (type) {
        WifiSsidPolicyType.WIFI_SSID_ALLOWLIST ->
            PlatformWifiSsidPolicy.WIFI_SSID_POLICY_TYPE_ALLOWLIST

        WifiSsidPolicyType.WIFI_SSID_DENYLIST ->
            PlatformWifiSsidPolicy.WIFI_SSID_POLICY_TYPE_DENYLIST
    },
    ssids.map { WifiSsid.fromBytes(it.toByteArray(Charsets.UTF_8)) }.toSet()
)
