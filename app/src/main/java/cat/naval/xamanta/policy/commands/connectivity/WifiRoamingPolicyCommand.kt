package cat.naval.xamanta.policy.commands.connectivity

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiSsid
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.models.WifiRoamingMode

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class WifiRoamingPolicyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "wifiRoamingPolicy"
    override val minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM

    override fun execute() {
        val wifi = scope.context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi == null) {
            deviceCannot(
                reason = "this device has no Wi-Fi",
                asked = scope.policy.wifiRoamingSettings.isNotEmpty(),
            )
            return
        }
        val store = PolicyStore(scope.context)

        val settings = scope.policy.wifiRoamingSettings
        require(settings.none { it.ssid.isEmpty() }) { "a Wi-Fi roaming setting must name an SSID" }
        require(settings.map { it.ssid }.distinct().size == settings.size) {
            "Wi-Fi roaming settings must not repeat an SSID"
        }
        val desired = settings.associate { it.ssid to it.mode }
        val unsupported =
            if (wifi.isAggressiveRoamingModeSupported) emptySet()
            else desired.filterValues { it == WifiRoamingMode.WIFI_ROAMING_AGGRESSIVE }.keys
        val applied = desired - unsupported
        (store.roamingSsids() - applied.keys).forEach { ssid ->
            wifi.setRoaming(ssid, WifiRoamingMode.WIFI_ROAMING_DEFAULT)
        }
        applied.forEach { (ssid, mode) -> wifi.setRoaming(ssid, mode) }

        store.setRoamingSsids(applied.keys)
        deviceCannot(
            reason = "this device has no aggressive roaming: ${unsupported.joinToString()}",
            asked = unsupported.isNotEmpty(),
        )
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun WifiManager.setRoaming(ssid: String, mode: WifiRoamingMode) = setPerSsidRoamingMode(
    WifiSsid.fromBytes(ssid.toByteArray(Charsets.UTF_8)),
    when (mode) {
        WifiRoamingMode.WIFI_ROAMING_DISABLED -> WifiManager.ROAMING_MODE_NONE
        WifiRoamingMode.WIFI_ROAMING_DEFAULT -> WifiManager.ROAMING_MODE_NORMAL
        WifiRoamingMode.WIFI_ROAMING_AGGRESSIVE -> WifiManager.ROAMING_MODE_AGGRESSIVE
    },
)
