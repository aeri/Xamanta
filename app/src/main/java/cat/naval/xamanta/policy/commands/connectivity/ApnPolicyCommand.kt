package cat.naval.xamanta.policy.commands.connectivity

import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.models.ApnAuthType
import cat.naval.xamanta.models.ApnMvnoType
import cat.naval.xamanta.models.ApnNetworkType
import cat.naval.xamanta.models.ApnProtocol
import cat.naval.xamanta.models.ApnType
import android.telephony.data.ApnSetting as PlatformApnSetting
import cat.naval.xamanta.models.ApnSetting as ModelApnSetting

@RequiresApi(Build.VERSION_CODES.P)
class ApnPolicyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "apnPolicy"
    override val minSdk = Build.VERSION_CODES.P

    override fun execute() {
        val store = PolicyStore(scope.context)
        val previous = store.overrideApnIds()

        val onDevice = runCatching { scope.dpm.getOverrideApns(scope.admin) }.getOrDefault(emptyList())
        onDevice.filter { it.id in previous }.forEach {
            runCatching { scope.dpm.removeOverrideApn(scope.admin, it.id) }
        }

        val added = scope.policy.apnSettings.mapNotNull { setting ->
            scope.dpm.addOverrideApn(scope.admin, setting.toPlatform()).takeIf { it != -1 }
        }
        store.setOverrideApnIds(added.toSet())

        scope.dpm.setOverrideApnsEnabled(scope.admin, scope.policy.overrideApnsEnabled)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private fun ModelApnSetting.toPlatform(): PlatformApnSetting {
    val builder = PlatformApnSetting.Builder()
        .setApnName(apn)
        .setEntryName(displayName)
        .setApnTypeBitmask(apnTypes.fold(0) { mask, type -> mask or type.toPlatform() })
        .setCarrierEnabled(true)

    protocol?.let { builder.setProtocol(it.toPlatform()) }
    roamingProtocol?.let { builder.setRoamingProtocol(it.toPlatform()) }
    authType?.let { builder.setAuthType(it.toPlatform()) }
    mvnoType?.let { builder.setMvnoType(it.toPlatform()) }
    username?.let { builder.setUser(it) }
    password?.let { builder.setPassword(it) }
    mmsc?.let { builder.setMmsc(it.toUri()) }
    numericOperatorId?.let { builder.setOperatorNumeric(it) }
    if (mmsProxyPort > 0) builder.setMmsProxyPort(mmsProxyPort)
    if (proxyPort > 0) builder.setProxyPort(proxyPort)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mmsProxyAddress?.let { builder.setMmsProxyAddress(it) }
        proxyAddress?.let { builder.setProxyAddress(it) }
        if (carrierId > 0) builder.setCarrierId(carrierId)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (mtuV4 > 0) builder.setMtuV4(mtuV4)
        if (mtuV6 > 0) builder.setMtuV6(mtuV6)
        if (networkTypes.isNotEmpty()) builder.setNetworkTypeBitmask(networkTypes.bitmask())
    }
    if (alwaysOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        builder.setAlwaysOn(true)
    }

    return builder.build()
}

@RequiresApi(Build.VERSION_CODES.P)
private fun ApnProtocol.toPlatform(): Int = when (this) {
    ApnProtocol.IP -> PlatformApnSetting.PROTOCOL_IP
    ApnProtocol.IPV4V6 -> PlatformApnSetting.PROTOCOL_IPV4V6
    ApnProtocol.IPV6 -> PlatformApnSetting.PROTOCOL_IPV6
    ApnProtocol.PPP -> PlatformApnSetting.PROTOCOL_PPP
    ApnProtocol.NON_IP ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) PlatformApnSetting.PROTOCOL_NON_IP
        else PlatformApnSetting.PROTOCOL_IP

    ApnProtocol.UNSTRUCTURED ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) PlatformApnSetting.PROTOCOL_UNSTRUCTURED
        else PlatformApnSetting.PROTOCOL_IP
}

@RequiresApi(Build.VERSION_CODES.P)
private fun ApnAuthType.toPlatform(): Int = when (this) {
    ApnAuthType.NONE -> PlatformApnSetting.AUTH_TYPE_NONE
    ApnAuthType.PAP -> PlatformApnSetting.AUTH_TYPE_PAP
    ApnAuthType.CHAP -> PlatformApnSetting.AUTH_TYPE_CHAP
    ApnAuthType.PAP_OR_CHAP -> PlatformApnSetting.AUTH_TYPE_PAP_OR_CHAP
}

@RequiresApi(Build.VERSION_CODES.P)
private fun ApnMvnoType.toPlatform(): Int = when (this) {
    ApnMvnoType.GID -> PlatformApnSetting.MVNO_TYPE_GID
    ApnMvnoType.ICCID -> PlatformApnSetting.MVNO_TYPE_ICCID
    ApnMvnoType.IMSI -> PlatformApnSetting.MVNO_TYPE_IMSI
    ApnMvnoType.SPN -> PlatformApnSetting.MVNO_TYPE_SPN
}

@RequiresApi(Build.VERSION_CODES.P)
private fun ApnType.toPlatform(): Int = when (this) {
    ApnType.DEFAULT -> PlatformApnSetting.TYPE_DEFAULT
    ApnType.MMS -> PlatformApnSetting.TYPE_MMS
    ApnType.SUPL -> PlatformApnSetting.TYPE_SUPL
    ApnType.DUN -> PlatformApnSetting.TYPE_DUN
    ApnType.HIPRI -> PlatformApnSetting.TYPE_HIPRI
    ApnType.FOTA -> PlatformApnSetting.TYPE_FOTA
    ApnType.IMS -> PlatformApnSetting.TYPE_IMS
    ApnType.CBS -> PlatformApnSetting.TYPE_CBS
    ApnType.IA -> PlatformApnSetting.TYPE_IA
    ApnType.EMERGENCY -> PlatformApnSetting.TYPE_EMERGENCY
    ApnType.MCX -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) PlatformApnSetting.TYPE_MCX else 0
    ApnType.XCAP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PlatformApnSetting.TYPE_XCAP else 0
    ApnType.BIP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PlatformApnSetting.TYPE_BIP else 0
    ApnType.VSIM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PlatformApnSetting.TYPE_VSIM else 0
    ApnType.ENTERPRISE ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PlatformApnSetting.TYPE_ENTERPRISE else 0

    ApnType.RCS ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) PlatformApnSetting.TYPE_RCS else 0
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun List<ApnNetworkType>.bitmask(): Int =
    fold(0L) { mask, type -> mask or type.toPlatform() }.toInt()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun ApnNetworkType.toPlatform(): Long = when (this) {
    ApnNetworkType.EDGE -> TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
    ApnNetworkType.GPRS -> TelephonyManager.NETWORK_TYPE_BITMASK_GPRS
    ApnNetworkType.GSM -> TelephonyManager.NETWORK_TYPE_BITMASK_GSM
    ApnNetworkType.HSDPA -> TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA
    ApnNetworkType.HSPA -> TelephonyManager.NETWORK_TYPE_BITMASK_HSPA
    ApnNetworkType.HSPAP -> TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP
    ApnNetworkType.HSUPA -> TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA
    ApnNetworkType.IWLAN -> TelephonyManager.NETWORK_TYPE_BITMASK_IWLAN
    ApnNetworkType.LTE -> TelephonyManager.NETWORK_TYPE_BITMASK_LTE
    ApnNetworkType.NR -> TelephonyManager.NETWORK_TYPE_BITMASK_NR
    ApnNetworkType.TD_SCDMA -> TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA
    ApnNetworkType.UMTS -> TelephonyManager.NETWORK_TYPE_BITMASK_UMTS
}
