package cat.naval.xamanta.policy.commands.connectivity

import android.app.admin.PreferentialNetworkServiceConfig
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.UnsupportedManagementModeException
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.PreferentialNetworkId

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class PreferentialNetworkCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "preferentialNetworkId"
    override val minSdk = Build.VERSION_CODES.TIRAMISU

    override fun execute() {
        val configs = if (!scope.policy.preferentialNetworkServiceEnabled) {
            listOf(noPreference())
        } else {
            scope.policy.applications
                .groupBy { it.preferentialNetworkId ?: scope.policy.defaultPreferentialNetworkId }
                .mapNotNull { (networkId, apps) -> networkId?.let { configFor(it, apps) } }
                .ifEmpty { listOf(noPreference()) }
        }
        try {
            scope.dpm.setPreferentialNetworkServiceConfigs(configs)
        } catch (failure: SecurityException) {
            throw UnsupportedManagementModeException(
                "preferential network service is not available to this DPC: ${failure.message}"
            )
        }
    }

    private fun configFor(
        networkId: PreferentialNetworkId,
        apps: List<ApplicationPolicy>,
    ): PreferentialNetworkServiceConfig? {
        val uids = apps.mapNotNull { uidOf(it.packageName) }.distinct()
        if (uids.isEmpty()) return null
        val settings = scope.policy.preferentialNetworkConfigs.firstOrNull {
            it.networkId == networkId
        }
        val builder = PreferentialNetworkServiceConfig.Builder()
            .setEnabled(true)
            .setNetworkId(networkId.toPlatform())
            .setIncludedUids(uids.toIntArray())
            .setFallbackToDefaultConnectionAllowed(settings?.fallbackToDefaultAllowed ?: true)
        if (settings?.blockNonMatchingNetworks == true &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            builder.setShouldBlockNonMatchingNetworks(true)
        }
        return builder.build()
    }

    private fun noPreference(): PreferentialNetworkServiceConfig =
        PreferentialNetworkServiceConfig.Builder().setEnabled(false).build()

    private fun uidOf(packageName: String): Int? =
        runCatching { scope.context.packageManager.getApplicationInfo(packageName, 0).uid }
            .getOrNull()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun PreferentialNetworkId.toPlatform(): Int = when (this) {
    PreferentialNetworkId.ONE -> PreferentialNetworkServiceConfig.PREFERENTIAL_NETWORK_ID_1
    PreferentialNetworkId.TWO -> PreferentialNetworkServiceConfig.PREFERENTIAL_NETWORK_ID_2
    PreferentialNetworkId.THREE -> PreferentialNetworkServiceConfig.PREFERENTIAL_NETWORK_ID_3
    PreferentialNetworkId.FOUR -> PreferentialNetworkServiceConfig.PREFERENTIAL_NETWORK_ID_4
    PreferentialNetworkId.FIVE -> PreferentialNetworkServiceConfig.PREFERENTIAL_NETWORK_ID_5
}
