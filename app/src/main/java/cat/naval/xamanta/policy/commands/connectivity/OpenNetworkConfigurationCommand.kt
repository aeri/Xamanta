package cat.naval.xamanta.policy.commands.connectivity

import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.Log
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.models.ConfigureWifi
import cat.naval.xamanta.models.EapInner
import cat.naval.xamanta.models.EapOuter
import cat.naval.xamanta.models.EapSettings
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.NetworkConfiguration
import cat.naval.xamanta.models.ProvisionedNetwork
import cat.naval.xamanta.models.WifiSecurity
import java.security.cert.X509Certificate

private const val TAG = "OncCommand"

class OpenNetworkConfigurationCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "openNetworkConfiguration"

    override var details: List<NonComplianceDetail> = emptyList()
        private set

    override fun execute() {
        val wifi = scope.context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi == null) {
            deviceCannot(
                reason = "this device has no Wi-Fi",
                asked = scope.policy.networkConfigurations.isNotEmpty(),
            )
            return
        }
        val store = PolicyStore(scope.context)

        val listsAllNetworks = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

        val credentials = OncCredentials(scope.dpm, scope.admin)
        val aliases = credentials.install(scope.policy.oncCertificates)

        val problems = mutableListOf<NonComplianceDetail>()

        val connectedId = if (listsAllNetworks) wifi.activeNetworkId() else -1

        val previous = store.provisionedNetworks()
            ?: throw IllegalStateException("the provisioned-network record could not be read")

        val wanted = scope.policy.networkConfigurations.associateBy { it.guid }
        val deferred = mutableMapOf<String, ProvisionedNetwork>()
        (previous - wanted.keys).forEach { (guid, record) ->
            when {
                record.networkId == connectedId -> {
                    Log.i(TAG, "network '$guid' is the active connection — deferring its removal")
                    deferred[guid] = record
                }
                !wifi.removeNetwork(record.networkId) -> {
                    Log.w(TAG, "the platform kept network '$guid' (id ${record.networkId})")
                    problems += NonCompliance.userAction(name, "could not remove network '$guid'")
                }
            }
        }

        val installed = mutableMapOf<String, ProvisionedNetwork>()
        wanted.values.forEach { network ->
            if (network.wifi.eap != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                problems += NonCompliance.apiLevel(
                    settingName = name,
                    currentValue = "network '${network.guid}' uses EAP, which needs API 24",
                )
                return@forEach
            }

            val digest = network.digest(scope.policy.oncCertificates)
            val applied = previous[network.guid]
            if (applied != null &&
                applied.digest == digest &&
                wifi.hasNetwork(applied.networkId, listsAllNetworks) != false
            ) {
                installed[network.guid] = applied
                return@forEach
            }

            val config = network.toWifiConfiguration(credentials)
                ?: run {
                    problems += missingCredential(network)
                    return@forEach
                }
            applied?.let { config.networkId = it.networkId }

            if (applied?.networkId == connectedId && connectedId != -1) {
                Log.w(TAG, "rewriting network '${network.guid}', the active connection")
            } else {
                Log.i(TAG, "writing network '${network.guid}'")
            }

            val id = wifi.add(config)
            if (id == -1) {
                problems += NonCompliance.invalidValue(
                    settingName = name,
                    currentValue = "the platform refused network '${network.guid}'",
                )
                return@forEach
            }
            if (network.wifi.autoConnect) wifi.enableNetwork(id, false) else wifi.disableNetwork(id)
            installed[network.guid] = ProvisionedNetwork(id, digest)
        }

        if (scope.policy.configureWifi == ConfigureWifi.DISALLOW_CONFIGURING_WIFI) {
            val ours = installed.values.map { it.networkId }.toSet()
            problems += when {
                listsAllNetworks -> removeForeignNetworks(wifi, ours, connectedId)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> removeNonCallerNetworks(wifi)
                else -> listOf(
                    NonCompliance.userAction(name, "non-policy networks cannot be listed on API 29-30"),
                )
            }
        }

        store.setProvisionedNetworks(installed + deferred)
        credentials.revokeStaleKeys(store.provisionedKeyAliases())
        store.setProvisionedKeyAliases(aliases)

        val protectedCaCerts = EnrollmentStore(scope.context).serverCaCert()
            ?.let { OncCredentials.canonicalCaCertDers(it) }.orEmpty()
        credentials.revokeStaleCaCerts(store.provisionedCaCerts(), protectedCaCerts)
        store.setProvisionedCaCerts(credentials.installedCaCertDers)

        details = problems
    }

    @Suppress("DEPRECATION")
    private fun removeForeignNetworks(
        wifi: WifiManager,
        ours: Set<Int>,
        connectedId: Int,
    ): List<NonComplianceDetail> = wifi.configuredNetworks.orEmpty()
        .filter { it.networkId !in ours }
        .mapNotNull { configuration ->
            when {
                configuration.networkId == connectedId ->
                    NonCompliance.userAction(name, "a non-policy network is the active connection")

                !wifi.removeNetwork(configuration.networkId) -> {
                    Log.w(TAG, "the platform kept non-policy network ${configuration.networkId}")
                    NonCompliance.userAction(
                        name,
                        "could not remove non-policy network ${configuration.networkId}",
                    )
                }

                else -> null
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun removeNonCallerNetworks(wifi: WifiManager): List<NonComplianceDetail> =
        runCatching { wifi.removeNonCallerConfiguredNetworks() }.fold(
            onSuccess = { emptyList() },
            onFailure = {
                Log.w(TAG, "removeNonCallerConfiguredNetworks refused: ${it.message}")
                listOf(NonCompliance.invalidValue(name, "the platform refused to remove non-policy networks"))
            },
        )

    private fun missingCredential(network: NetworkConfiguration) = NonCompliance.invalidValue(
        settingName = name,
        currentValue = "network '${network.guid}' names a certificate that did not install",
    )
}

@Suppress("DEPRECATION")
private fun WifiManager.activeNetworkId(): Int {
    val info = runCatching { connectionInfo }.getOrNull() ?: return -1
    return if (info.supplicantState == SupplicantState.COMPLETED) info.networkId else -1
}

@Suppress("DEPRECATION")
private fun WifiManager.hasNetwork(networkId: Int, listsAllNetworks: Boolean): Boolean? {
    val networks = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> runCatching { callerConfiguredNetworks }.getOrNull()
        listsAllNetworks -> runCatching { configuredNetworks }.getOrNull()
        else -> null
    } ?: return null
    return networks.any { it.networkId == networkId }
}

private fun WifiManager.add(config: WifiConfiguration): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val result = runCatching { addNetworkPrivileged(config) }.getOrNull()
        if (result == null || result.statusCode != WifiManager.AddNetworkResult.STATUS_SUCCESS) {
            Log.e(TAG, "addNetworkPrivileged refused: ${result?.statusCode}")
            -1
        } else {
            result.networkId
        }
    } else {
        @Suppress("DEPRECATION")
        runCatching { addNetwork(config) }.getOrDefault(-1)
    }

@Suppress("DEPRECATION")
private fun NetworkConfiguration.toWifiConfiguration(
    credentials: OncCredentials,
): WifiConfiguration? {
    val config = WifiConfiguration().apply {
        SSID = "\"${wifi.ssid}\""
        hiddenSSID = wifi.hiddenSsid
        status =
            if (wifi.autoConnect) WifiConfiguration.Status.ENABLED
            else WifiConfiguration.Status.DISABLED
    }

    when (wifi.security) {
        WifiSecurity.NONE ->
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

        WifiSecurity.WEP_PSK -> {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            val key = wifi.passphrase.orEmpty().removePrefix("0x")
            config.wepKeys[0] = if (key.isHexKey()) key else "\"$key\""
            config.wepTxKeyIndex = 0
        }

        WifiSecurity.WPA_PSK -> {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            config.preSharedKey = "\"${wifi.passphrase.orEmpty()}\""
        }

        WifiSecurity.WPA_EAP, WifiSecurity.WEP_8021X, WifiSecurity.WPA3_ENTERPRISE_192 -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X)
            config.enterpriseConfig = wifi.eap?.toEnterpriseConfig(credentials) ?: return null
        }
    }
    return config
}

@RequiresApi(Build.VERSION_CODES.N)
@Suppress("DEPRECATION")
private fun EapSettings.toEnterpriseConfig(credentials: OncCredentials): WifiEnterpriseConfig? {
    val config = WifiEnterpriseConfig()
    config.eapMethod = when (outer) {
        EapOuter.EAP_TLS -> WifiEnterpriseConfig.Eap.TLS
        EapOuter.EAP_TTLS -> WifiEnterpriseConfig.Eap.TTLS
        EapOuter.PEAP -> WifiEnterpriseConfig.Eap.PEAP
        EapOuter.EAP_SIM -> WifiEnterpriseConfig.Eap.SIM
        EapOuter.EAP_AKA -> WifiEnterpriseConfig.Eap.AKA
        EapOuter.EAP_PWD -> WifiEnterpriseConfig.Eap.PWD
    }
    config.phase2Method = when (inner) {
        EapInner.MSCHAPV2 -> WifiEnterpriseConfig.Phase2.MSCHAPV2
        EapInner.PAP -> WifiEnterpriseConfig.Phase2.PAP
        null -> WifiEnterpriseConfig.Phase2.NONE
    }
    identity?.let { config.identity = it }
    anonymousIdentity?.let { config.anonymousIdentity = it }
    password?.let { config.password = it }
    config.domainSuffixMatch = domainSuffixMatch.joinToString(";")

    if (serverCaRefs.isNotEmpty()) {
        val chain = serverCaRefs.flatMap { credentials.serverCerts[it].orEmpty() }
        if (chain.isEmpty()) return null
        config.caCertificates = chain.toTypedArray()
    }

    val alias = clientCertKeyPairAlias ?: clientCertRef?.let { credentials.clientAliases[it] }
    if (clientCertRef != null && alias == null) return null

    if (alias != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            config.setClientKeyPairAlias(alias)
        } else {
            val key = clientCertRef?.let { credentials.clientKeys[it] } ?: return null
            val leaf = key.second.firstOrNull() as? X509Certificate ?: return null
            config.setClientKeyEntry(key.first, leaf)
        }
    }

    return config
}

private fun String.isHexKey(): Boolean =
    (length == 10 || length == 26) && all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
