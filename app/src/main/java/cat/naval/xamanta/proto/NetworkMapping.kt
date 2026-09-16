package cat.naval.xamanta.proto

import cat.naval.xamanta.models.ApnAuthType as ModelApnAuthType
import cat.naval.xamanta.models.ApnMvnoType as ModelApnMvnoType
import cat.naval.xamanta.models.ApnNetworkType as ModelApnNetworkType
import cat.naval.xamanta.models.ApnProtocol as ModelApnProtocol
import cat.naval.xamanta.models.ApnSetting as ModelApnSetting
import cat.naval.xamanta.models.ApnType as ModelApnType
import cat.naval.xamanta.models.EapInner as ModelEapInner
import cat.naval.xamanta.models.EapOuter as ModelEapOuter
import cat.naval.xamanta.models.EapSettings as ModelEapSettings
import cat.naval.xamanta.models.MacRandomization as ModelMacRandomization
import cat.naval.xamanta.models.NetworkConfiguration as ModelNetworkConfiguration
import cat.naval.xamanta.models.OncCertificate as ModelOncCertificate
import cat.naval.xamanta.models.OncCertificateType as ModelOncCertificateType
import cat.naval.xamanta.models.PreferentialNetworkConfig as ModelPreferentialNetworkConfig
import cat.naval.xamanta.models.PreferentialNetworkId as ModelPreferentialNetworkId
import cat.naval.xamanta.models.WifiRoamingMode as ModelWifiRoamingMode
import cat.naval.xamanta.models.WifiRoamingSetting as ModelWifiRoamingSetting
import cat.naval.xamanta.models.WifiSecurity as ModelWifiSecurity
import cat.naval.xamanta.models.WifiSettings as ModelWifiSettings
import cat.naval.xamanta.models.WifiSsidPolicy as ModelWifiSsidPolicy
import cat.naval.xamanta.models.WifiSsidPolicyType as ModelWifiSsidPolicyType
import cat.naval.xamanta.protos.ApnSetting as ProtoApnSetting
import cat.naval.xamanta.protos.ApnSetting.AlwaysOnSetting as ProtoAlwaysOnSetting
import cat.naval.xamanta.protos.EapSettings as ProtoEapSettings
import cat.naval.xamanta.protos.EapSettings.ClientCertType as ProtoClientCertType
import cat.naval.xamanta.protos.NetworkConfiguration as ProtoNetworkConfiguration
import cat.naval.xamanta.protos.OncCertificate as ProtoOncCertificate
import cat.naval.xamanta.protos.OncCertificate.Type as ProtoOncCertificateType
import cat.naval.xamanta.protos.PreferentialNetworkId as ProtoPreferentialNetworkId
import cat.naval.xamanta.protos.PreferentialNetworkServiceConfig as ProtoPreferentialNetworkServiceConfig
import cat.naval.xamanta.protos.WifiRoamingSetting as ProtoWifiRoamingSetting
import cat.naval.xamanta.protos.WifiSettings as ProtoWifiSettings
import cat.naval.xamanta.protos.WifiSsidPolicy as ProtoWifiSsidPolicy

internal fun ProtoWifiSsidPolicy.toKotlinModel(): ModelWifiSsidPolicy? {
    val type = wifiSsidPolicyType.asModelOrNull<ModelWifiSsidPolicyType>() ?: return null
    return ModelWifiSsidPolicy(type, wifiSsidsList.map { it.wifiSsid })
}

fun ProtoPreferentialNetworkId.toKotlinModel(): ModelPreferentialNetworkId? = when (this) {
    ProtoPreferentialNetworkId.PREFERENTIAL_NETWORK_ID_ONE -> ModelPreferentialNetworkId.ONE
    ProtoPreferentialNetworkId.PREFERENTIAL_NETWORK_ID_TWO -> ModelPreferentialNetworkId.TWO
    ProtoPreferentialNetworkId.PREFERENTIAL_NETWORK_ID_THREE -> ModelPreferentialNetworkId.THREE
    ProtoPreferentialNetworkId.PREFERENTIAL_NETWORK_ID_FOUR -> ModelPreferentialNetworkId.FOUR
    ProtoPreferentialNetworkId.PREFERENTIAL_NETWORK_ID_FIVE -> ModelPreferentialNetworkId.FIVE
    else -> null
}

internal fun ProtoPreferentialNetworkServiceConfig.toKotlinModel(): ModelPreferentialNetworkConfig? {
    val id = preferentialNetworkId.toKotlinModel() ?: return null
    return ModelPreferentialNetworkConfig(
        networkId = id,
        blockNonMatchingNetworks = nonMatchingNetworks ==
                ProtoPreferentialNetworkServiceConfig.NonMatchingNetworks
                    .NON_MATCHING_NETWORKS_DISALLOWED,
        fallbackToDefaultAllowed = fallbackToDefaultConnection !=
                ProtoPreferentialNetworkServiceConfig.FallbackToDefaultConnection
                    .FALLBACK_TO_DEFAULT_CONNECTION_DISALLOWED,
    )
}

internal fun ProtoWifiRoamingSetting.toKotlinModel(): ModelWifiRoamingSetting =
    ModelWifiRoamingSetting(
        ssid = wifiSsid,
        mode = wifiRoamingMode.asModel(ModelWifiRoamingMode.WIFI_ROAMING_DEFAULT),
    )

internal fun ProtoApnSetting.toKotlinModel(): ModelApnSetting? {
    if (apn.isEmpty() || displayName.isEmpty()) return null
    return ModelApnSetting(
        apn = apn,
        displayName = displayName,
        apnTypes = apnTypesList.mapNotNull { it.asModelOrNull<ModelApnType>() },
        protocol = protocol.asModelOrNull<ModelApnProtocol>(),
        roamingProtocol = roamingProtocol.asModelOrNull<ModelApnProtocol>(),
        authType = authType.asModelOrNull<ModelApnAuthType>(),
        username = username.takeIf { it.isNotEmpty() },
        password = password.takeIf { it.isNotEmpty() },
        mmsc = mmsc.takeIf { it.isNotEmpty() },
        mmsProxyAddress = mmsProxyAddress.takeIf { it.isNotEmpty() },
        mmsProxyPort = mmsProxyPort,
        proxyAddress = proxyAddress.takeIf { it.isNotEmpty() },
        proxyPort = proxyPort,
        mvnoType = mvnoType.asModelOrNull<ModelApnMvnoType>(),
        numericOperatorId = numericOperatorId.takeIf { it.isNotEmpty() },
        carrierId = carrierId,
        networkTypes = networkTypesList.mapNotNull { it.asModelOrNull<ModelApnNetworkType>() },
        mtuV4 = mtuV4,
        mtuV6 = mtuV6,
        alwaysOn = alwaysOnSetting == ProtoAlwaysOnSetting.ALWAYS_ON,
    )
}

internal fun ProtoOncCertificate.toKotlinModel(): ModelOncCertificate? {
    if (guid.isEmpty()) return null
    return when (type) {
        ProtoOncCertificateType.SERVER -> x509.takeIf { it.isNotEmpty() }?.let {
            ModelOncCertificate(guid = guid, type = ModelOncCertificateType.SERVER, x509 = it)
        }

        ProtoOncCertificateType.CLIENT -> pkcs12.takeIf { it.isNotEmpty() }?.let {
            ModelOncCertificate(
                guid = guid,
                type = ModelOncCertificateType.CLIENT,
                pkcs12 = it,
                pkcs12Password = pkcs12Password.takeIf { pw -> pw.isNotEmpty() },
            )
        }

        else -> null
    }
}

internal fun ProtoNetworkConfiguration.toKotlinModel(): ModelNetworkConfiguration? {
    if (guid.isEmpty() || !hasWifi()) return null
    val ssid = wifi.resolvedSsid() ?: return null
    val security = wifi.security.asModelOrNull<ModelWifiSecurity>() ?: return null

    val eap = if (security.needsEap) wifi.eap.toKotlinModel() ?: return null else null
    if (security.needsPassphrase && wifi.passphrase.isEmpty()) return null

    return ModelNetworkConfiguration(
        guid = guid,
        name = name,
        wifi = ModelWifiSettings(
            ssid = ssid,
            hiddenSsid = wifi.hiddenSsid,
            security = security,
            autoConnect = wifi.autoConnect,
            passphrase = wifi.passphrase.takeIf { it.isNotEmpty() },
            eap = eap,
            macRandomization =
                wifi.macAddressRandomizationMode.asModelOrNull<ModelMacRandomization>(),
        ),
        proxy = proxySettings.takeIf { hasProxySettings() }?.toKotlinModel(),
    )
}

private val ModelWifiSecurity.needsEap: Boolean
    get() = this == ModelWifiSecurity.WPA_EAP ||
            this == ModelWifiSecurity.WEP_8021X ||
            this == ModelWifiSecurity.WPA3_ENTERPRISE_192

private val ModelWifiSecurity.needsPassphrase: Boolean
    get() = this == ModelWifiSecurity.WEP_PSK || this == ModelWifiSecurity.WPA_PSK

private fun ProtoWifiSettings.resolvedSsid(): String? {
    val fromHex = hexSsid.takeIf { it.isNotEmpty() }?.let { hex ->
        runCatching {
            require(hex.length % 2 == 0)
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
        }.getOrNull() ?: return null
    }
    val fromText = ssid.takeIf { it.isNotEmpty() }
    if (fromText != null && fromHex != null && fromText != fromHex) return null
    return fromText ?: fromHex
}

private fun ProtoEapSettings.toKotlinModel(): ModelEapSettings? {
    val outerMethod = outer.asModelOrNull<ModelEapOuter>() ?: return null
    val domains = domainSuffixMatchList.filter { it.isNotEmpty() }
    if (domains.isEmpty()) return null

    val alias = clientCertKeyPairAlias.takeIf {
        it.isNotEmpty() && clientCertType == ProtoClientCertType.KEY_PAIR_ALIAS
    }
    val ref = clientCertRef.takeIf {
        it.isNotEmpty() && clientCertType == ProtoClientCertType.REF
    }
    if (outerMethod == ModelEapOuter.EAP_TLS && alias == null && ref == null) return null

    return ModelEapSettings(
        outer = outerMethod,
        inner = inner.asModelOrNull<ModelEapInner>(),
        identity = identity.takeIf { it.isNotEmpty() },
        anonymousIdentity = anonymousIdentity.takeIf { it.isNotEmpty() },
        password = password.takeIf { it.isNotEmpty() },
        domainSuffixMatch = domains,
        serverCaRefs = serverCaRefsList.filter { it.isNotEmpty() },
        clientCertRef = ref,
        clientCertKeyPairAlias = alias,
    )
}
