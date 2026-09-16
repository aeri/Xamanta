package cat.naval.xamanta.models

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class InstallType {
    INSTALL_TYPE_UNSPECIFIED,
    INSTALLED,
    FORCE_INSTALLED,
    KIOSK,
    BLOCKED,

    AVAILABLE
}

@Serializable
enum class PermissionPolicy {
    PROMPT,
    GRANT,
    DENY
}

@Serializable
enum class SystemUpdateType {
    AUTOMATIC,
    WINDOWED,
    POSTPONE
}

@Serializable
class SystemUpdate(
    val type: SystemUpdateType,
    val startMinutes: Int = 0,
    val endMinutes: Int = 0,
)

@Serializable
class FactoryResetProtection(
    val accountIds: List<String> = emptyList(),
    val disabled: Boolean = false,
)

@Serializable
class PermissionGrant(
    val permission: String,
    val policy: PermissionPolicy = PermissionPolicy.PROMPT,
)

@Serializable
class ManagedProperty(
    val key: String,
    val stringValue: String? = null,
    val boolValue: Boolean? = null,
    val intValue: Int? = null,
    val stringListValue: List<String>? = null,
    val bundleValue: List<ManagedProperty>? = null,
    val bundleArrayValue: List<List<ManagedProperty>>? = null,
)

@Serializable
enum class DelegatedScope {
    CERT_INSTALL,
    MANAGED_CONFIGURATIONS,
    BLOCK_UNINSTALL,
    PERMISSION_GRANT,
    PACKAGE_ACCESS,
    ENABLE_SYSTEM_APP,
    NETWORK_ACTIVITY_LOGS,
    SECURITY_LOGS,
    CERT_SELECTION,
}

@Serializable
enum class UserControlSetting { USER_CONTROL_ALLOWED, USER_CONTROL_DISALLOWED }

@Serializable
enum class PreferentialNetworkId { ONE, TWO, THREE, FOUR, FIVE }

@Serializable
enum class PowerButtonActions {
    POWER_BUTTON_ACTIONS_UNSPECIFIED, POWER_BUTTON_AVAILABLE, POWER_BUTTON_BLOCKED
}

@Serializable
enum class SystemErrorWarnings {
    SYSTEM_ERROR_WARNINGS_UNSPECIFIED, ERROR_AND_WARNINGS_ENABLED, ERROR_AND_WARNINGS_MUTED
}

@Serializable
enum class SystemNavigation {
    SYSTEM_NAVIGATION_UNSPECIFIED, NAVIGATION_ENABLED, NAVIGATION_DISABLED, HOME_BUTTON_ONLY
}

@Serializable
enum class StatusBarMode {
    STATUS_BAR_UNSPECIFIED,
    NOTIFICATIONS_AND_SYSTEM_INFO_ENABLED,
    NOTIFICATIONS_AND_SYSTEM_INFO_DISABLED,
    SYSTEM_INFO_ONLY
}

@Serializable
enum class DeviceSettingsAccess {
    DEVICE_SETTINGS_UNSPECIFIED, SETTINGS_ACCESS_ALLOWED, SETTINGS_ACCESS_BLOCKED
}

@Serializable
class KioskCustomization(
    val powerButtonActions: PowerButtonActions = PowerButtonActions.POWER_BUTTON_ACTIONS_UNSPECIFIED,
    val systemErrorWarnings: SystemErrorWarnings = SystemErrorWarnings.SYSTEM_ERROR_WARNINGS_UNSPECIFIED,
    val systemNavigation: SystemNavigation = SystemNavigation.SYSTEM_NAVIGATION_UNSPECIFIED,
    val statusBar: StatusBarMode = StatusBarMode.STATUS_BAR_UNSPECIFIED,
    val deviceSettings: DeviceSettingsAccess = DeviceSettingsAccess.DEVICE_SETTINGS_UNSPECIFIED,
)

@Serializable
enum class CameraAccess {
    CAMERA_ACCESS_USER_CHOICE,
    CAMERA_ACCESS_DISABLED,
    CAMERA_ACCESS_ENFORCED,
}

@Serializable
enum class MicrophoneAccess {
    MICROPHONE_ACCESS_USER_CHOICE,
    MICROPHONE_ACCESS_DISABLED,
    MICROPHONE_ACCESS_ENFORCED,
}

@Serializable
enum class AutoDateAndTimeZone {
    AUTO_DATE_AND_TIME_ZONE_USER_CHOICE,
    AUTO_DATE_AND_TIME_ZONE_ENFORCED,
}

@Serializable
enum class DeveloperSettings {
    DEVELOPER_SETTINGS_DISABLED,
    DEVELOPER_SETTINGS_ALLOWED,
}

@Serializable
enum class UntrustedAppsPolicy {
    DISALLOW_INSTALL,
    ALLOW_INSTALL_DEVICE_WIDE,
}

@Serializable
enum class UsbDataAccess {
    ALLOW_USB_DATA_TRANSFER,
    DISALLOW_USB_FILE_TRANSFER,
    DISALLOW_USB_DATA_TRANSFER;

    val blocksFileTransfer: Boolean get() = this != ALLOW_USB_DATA_TRANSFER
}

@Serializable
enum class ConfigureWifi {
    ALLOW_CONFIGURING_WIFI,
    DISALLOW_ADD_WIFI_CONFIG,
    DISALLOW_CONFIGURING_WIFI;

    val blocksAdding: Boolean get() = this != ALLOW_CONFIGURING_WIFI
}

@Serializable
enum class TetheringSettings {
    ALLOW_ALL_TETHERING,
    DISALLOW_WIFI_TETHERING,
    DISALLOW_ALL_TETHERING;

    val blocksWifiTethering: Boolean get() = this != ALLOW_ALL_TETHERING
}

@Serializable
enum class WifiSecurityLevel {
    OPEN_NETWORK_SECURITY,
    PERSONAL_NETWORK_SECURITY,
    ENTERPRISE_NETWORK_SECURITY,
    ENTERPRISE_BIT192_NETWORK_SECURITY,
}

@Serializable
enum class WifiSsidPolicyType { WIFI_SSID_DENYLIST, WIFI_SSID_ALLOWLIST }

@Serializable
class WifiSsidPolicy(
    val type: WifiSsidPolicyType,
    val ssids: List<String> = emptyList(),
)

@Serializable
enum class MtePolicyMode { MTE_USER_CHOICE, MTE_ENABLED, MTE_DISABLED }

@Serializable
enum class CredentialProviderPolicyMode {
    CREDENTIAL_PROVIDER_DISALLOWED,
    CREDENTIAL_PROVIDER_DISALLOWED_EXCEPT_SYSTEM,
    CREDENTIAL_PROVIDER_ALLOWED,
}

@Serializable
class UserFacingMessage(
    val defaultMessage: String = "",
    val localizedMessages: Map<String, String> = emptyMap(),
) {
    fun resolve(): String {
        val locale = Locale.getDefault()
        return localizedMessages[locale.toLanguageTag()]
            ?: localizedMessages[locale.language]
            ?: defaultMessage
    }
}

@Serializable
enum class PrivateDnsMode {
    PRIVATE_DNS_USER_CHOICE,
    PRIVATE_DNS_AUTOMATIC,
    PRIVATE_DNS_SPECIFIED_HOST,
}

@Serializable
enum class WifiRoamingMode { WIFI_ROAMING_DISABLED, WIFI_ROAMING_DEFAULT, WIFI_ROAMING_AGGRESSIVE }

@Serializable
class WifiRoamingSetting(
    val ssid: String,
    val mode: WifiRoamingMode,
)

@Serializable
class PreferentialNetworkConfig(
    val networkId: PreferentialNetworkId,
    val blockNonMatchingNetworks: Boolean = false,
    val fallbackToDefaultAllowed: Boolean = true,
)

@Serializable
enum class DefaultApplicationType {
    DEFAULT_ASSISTANT,
    DEFAULT_BROWSER,
    DEFAULT_CALL_REDIRECTION,
    DEFAULT_CALL_SCREENING,
    DEFAULT_DIALER,
    DEFAULT_HOME,
    DEFAULT_SMS,
    DEFAULT_WALLET,
}

@Serializable
class DefaultApplication(
    val packageName: String,
    val type: DefaultApplicationType,
    val profileScoped: Boolean = false,
)

@Serializable
class StatusReportingSettings(
    val applicationReportsEnabled: Boolean = true,
    val deviceSettingsEnabled: Boolean = true,
    val softwareInfoEnabled: Boolean = true,
    val memoryInfoEnabled: Boolean = true,
    val networkInfoEnabled: Boolean = true,
    val displayInfoEnabled: Boolean = true,
    val powerManagementEventsEnabled: Boolean = true,
    val hardwareStatusEnabled: Boolean = true,
    val systemPropertiesEnabled: Boolean = true,
    val commonCriteriaModeEnabled: Boolean = true,
    val defaultApplicationInfoReportingEnabled: Boolean = true,
    val includeRemovedApps: Boolean = false,
)

@Serializable
class ChoosePrivateKeyRule(
    val urlPattern: String? = null,
    val privateKeyAlias: String? = null,
    val packageNames: List<String> = emptyList(),
)

@Serializable
class AlwaysOnVpnPackage(
    val packageName: String,
    val lockdownEnabled: Boolean = false,
)

@Serializable
class RecommendedGlobalProxy(
    val host: String? = null,
    val port: Int = 0,
    val excludedHosts: List<String> = emptyList(),
    val pacUri: String? = null,
)

@Serializable
class PersistentPreferredActivity(
    val receiverActivity: String,
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
)

@Serializable
enum class PasswordQuality {
    PASSWORD_QUALITY_UNSPECIFIED,
    SOMETHING,
    NUMERIC,
    NUMERIC_COMPLEX,
    ALPHABETIC,
    ALPHANUMERIC,
    COMPLEX,
    COMPLEXITY_LOW,
    COMPLEXITY_MEDIUM,
    COMPLEXITY_HIGH;

    val isComplexityBased: Boolean
        get() = this == COMPLEXITY_LOW || this == COMPLEXITY_MEDIUM || this == COMPLEXITY_HIGH
}

@Serializable
class PasswordRequirements(
    val quality: PasswordQuality = PasswordQuality.PASSWORD_QUALITY_UNSPECIFIED,
    val minimumLength: Int = 0,
    val minimumLetters: Int = 0,
    val minimumLowerCase: Int = 0,
    val minimumUpperCase: Int = 0,
    val minimumNonLetter: Int = 0,
    val minimumNumeric: Int = 0,
    val minimumSymbols: Int = 0,
    val historyLength: Int = 0,
    val maximumFailedPasswordsForWipe: Int = 0,
    val expirationTimeoutMs: Long = 0,
    val requireEveryDayUnlock: Boolean = false,
    val profileScoped: Boolean = false,
)

@Serializable
enum class WifiSecurity { NONE, WEP_PSK, WPA_PSK, WPA_EAP, WEP_8021X, WPA3_ENTERPRISE_192 }

@Serializable
enum class MacRandomization { HARDWARE, AUTOMATIC }

@Serializable
enum class EapOuter { EAP_AKA, EAP_TLS, EAP_TTLS, EAP_SIM, EAP_PWD, PEAP }

@Serializable
enum class EapInner { MSCHAPV2, PAP }

@Serializable
class EapSettings(
    val outer: EapOuter,
    val inner: EapInner? = null,
    val identity: String? = null,
    val anonymousIdentity: String? = null,
    val password: String? = null,
    val domainSuffixMatch: List<String> = emptyList(),
    val serverCaRefs: List<String> = emptyList(),
    val clientCertRef: String? = null,
    val clientCertKeyPairAlias: String? = null,
)

@Serializable
class WifiSettings(
    val ssid: String,
    val hiddenSsid: Boolean = false,
    val security: WifiSecurity = WifiSecurity.NONE,
    val autoConnect: Boolean = true,
    val passphrase: String? = null,
    val eap: EapSettings? = null,
    val macRandomization: MacRandomization? = null,
)

@Serializable
class NetworkConfiguration(
    val guid: String,
    val name: String = "",
    val wifi: WifiSettings,
    val proxy: RecommendedGlobalProxy? = null,
)

@Serializable
enum class OncCertificateType { SERVER, CLIENT }

@Serializable
class OncCertificate(
    val guid: String,
    val type: OncCertificateType,
    val x509: String? = null,
    val pkcs12: String? = null,
    val pkcs12Password: String? = null,
)

@Serializable
enum class ApnProtocol { IP, IPV4V6, IPV6, NON_IP, PPP, UNSTRUCTURED }

@Serializable
enum class ApnAuthType { NONE, PAP, CHAP, PAP_OR_CHAP }

@Serializable
enum class ApnMvnoType { GID, ICCID, IMSI, SPN }

@Serializable
enum class ApnType {
    ENTERPRISE, BIP, CBS, DEFAULT, DUN, EMERGENCY, FOTA, HIPRI,
    IA, IMS, MCX, MMS, RCS, SUPL, VSIM, XCAP,
}

@Serializable
enum class ApnNetworkType {
    EDGE, GPRS, GSM, HSDPA, HSPA, HSPAP, HSUPA, IWLAN, LTE, NR, TD_SCDMA, UMTS,
}

@Serializable
class ApnSetting(
    val apn: String,
    val displayName: String,
    val apnTypes: List<ApnType> = emptyList(),
    val protocol: ApnProtocol? = null,
    val roamingProtocol: ApnProtocol? = null,
    val authType: ApnAuthType? = null,
    val username: String? = null,
    val password: String? = null,
    val mmsc: String? = null,
    val mmsProxyAddress: String? = null,
    val mmsProxyPort: Int = 0,
    val proxyAddress: String? = null,
    val proxyPort: Int = 0,
    val mvnoType: ApnMvnoType? = null,
    val numericOperatorId: String? = null,
    val carrierId: Int = 0,
    val networkTypes: List<ApnNetworkType> = emptyList(),
    val mtuV4: Int = 0,
    val mtuV6: Int = 0,
    val alwaysOn: Boolean = false,
)

@Serializable
class Policy(
    val name: String,
    val version: Int,
    val description: String,
    val devicePolicy: DevicePolicy,
    val selfUpdate: SelfUpdate? = null,
)

@Serializable
class SelfUpdate(
    val versionCode: Long = 0,
    val downloadUrl: String? = null,
    val sha256Sum: String? = null,
)

@Serializable
class DevicePolicy(

    val id: String,
    val applications: List<ApplicationPolicy> = emptyList(),
    val addUserDisabled: Boolean = false,
    val adjustVolumeDisabled: Boolean = false,
    val factoryResetDisabled: Boolean = false,
    val installAppsDisabled: Boolean = false,
    val mountPhysicalMediaDisabled: Boolean = false,
    val modifyAccountsDisabled: Boolean = false,
    val uninstallAppsDisabled: Boolean = false,
    val keyguardDisabled: Boolean = false,
    val bluetoothConfigDisabled: Boolean = false,
    val cellBroadcastsConfigDisabled: Boolean = false,
    val credentialsConfigDisabled: Boolean = false,
    val mobileNetworksConfigDisabled: Boolean = false,
    val vpnConfigDisabled: Boolean = false,
    val createWindowsDisabled: Boolean = false,
    val networkResetDisabled: Boolean = false,
    val outgoingBeamDisabled: Boolean = false,
    val outgoingCallsDisabled: Boolean = false,
    val removeUserDisabled: Boolean = false,
    val shareLocationDisabled: Boolean = false,
    val smsDisabled: Boolean = false,
    val funDisabled: Boolean = false,
    val screenCaptureDisabled: Boolean = false,
    val airplaneModeDisabled: Boolean = false,

    val kioskCustomization: KioskCustomization? = null,
    val maximumTimeToLockMs: Long = 0,
    val keyguardDisabledFeatures: Int = 0,
    val permittedInputMethods: List<String>? = null,
    val permittedAccessibilityServices: List<String>? = null,
    val systemUpdate: SystemUpdate? = null,
    val stayOnPluggedModes: Int = 0,
    val deviceOwnerLockScreenInfo: UserFacingMessage? = null,
    val shortSupportMessage: UserFacingMessage? = null,
    val longSupportMessage: UserFacingMessage? = null,
    val factoryResetProtection: FactoryResetProtection? = null,
    val defaultPermissionPolicy: PermissionPolicy = PermissionPolicy.PROMPT,

    val bluetoothDisabled: Boolean = false,
    val bluetoothContactSharingDisabled: Boolean = false,
    val dataRoamingDisabled: Boolean = false,
    val setUserIconDisabled: Boolean = false,
    val setWallpaperDisabled: Boolean = false,
    val skipFirstUseHintsEnabled: Boolean = false,
    val printingDisabled: Boolean = false,
    val assistContentDisabled: Boolean = false,

    val locationEnabled: Boolean? = null,
    val locationConfigDisabled: Boolean = false,

    val cameraAccess: CameraAccess = CameraAccess.CAMERA_ACCESS_USER_CHOICE,
    val microphoneAccess: MicrophoneAccess = MicrophoneAccess.MICROPHONE_ACCESS_USER_CHOICE,
    val autoDateAndTimeZone: AutoDateAndTimeZone =
        AutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_USER_CHOICE,

    val storageEncryptionRequired: Boolean = false,

    val untrustedAppsPolicy: UntrustedAppsPolicy = UntrustedAppsPolicy.DISALLOW_INSTALL,
    val developerSettings: DeveloperSettings = DeveloperSettings.DEVELOPER_SETTINGS_DISABLED,
    val verifyAppsEnforced: Boolean? = null,
    val commonCriteriaModeEnabled: Boolean = false,
    val mtePolicy: MtePolicyMode? = null,
    val credentialProviderPolicy: CredentialProviderPolicyMode? = null,

    val wifiEnabled: Boolean? = null,
    val changeWifiStateDisabled: Boolean = false,
    val ultraWidebandDisabled: Boolean = false,
    val cellularTwoGDisabled: Boolean = false,
    val minimumWifiSecurityLevel: WifiSecurityLevel = WifiSecurityLevel.OPEN_NETWORK_SECURITY,

    val usbDataAccess: UsbDataAccess = UsbDataAccess.ALLOW_USB_DATA_TRANSFER,
    val configureWifi: ConfigureWifi = ConfigureWifi.ALLOW_CONFIGURING_WIFI,
    val tetheringSettings: TetheringSettings = TetheringSettings.ALLOW_ALL_TETHERING,
    val wifiDirectDisabled: Boolean = false,
    val wifiSsidPolicy: WifiSsidPolicy? = null,

    val screenBrightnessAutomatic: Boolean? = null,
    val screenBrightness: Int? = null,
    val brightnessConfigDisabled: Boolean = false,
    val screenTimeoutMs: Long? = null,
    val screenTimeoutConfigDisabled: Boolean = false,

    val alwaysOnVpnPackage: AlwaysOnVpnPackage? = null,
    val recommendedGlobalProxy: RecommendedGlobalProxy? = null,
    val accountTypesWithManagementDisabled: List<String> = emptyList(),
    val persistentPreferredActivities: List<PersistentPreferredActivity> = emptyList(),
    val passwordRequirements: PasswordRequirements? = null,

    val bluetoothSharingDisabled: Boolean = false,

    val privateDnsMode: PrivateDnsMode? = null,
    val privateDnsHost: String? = null,

    val wifiRoamingSettings: List<WifiRoamingSetting> = emptyList(),

    val overrideApnsEnabled: Boolean = false,
    val apnSettings: List<ApnSetting> = emptyList(),

    val preferentialNetworkServiceEnabled: Boolean = false,
    val defaultPreferentialNetworkId: PreferentialNetworkId? = null,
    val preferentialNetworkConfigs: List<PreferentialNetworkConfig> = emptyList(),

    val defaultApplications: List<DefaultApplication> = emptyList(),

    val appFunctionsAllowed: Boolean? = null,

    val wipeEsims: Boolean = false,

    val minimumApiLevel: Int = 0,

    val statusReporting: StatusReportingSettings = StatusReportingSettings(),

    val choosePrivateKeyRules: List<ChoosePrivateKeyRule> = emptyList(),

    val networkConfigurations: List<NetworkConfiguration> = emptyList(),
    val oncCertificates: List<OncCertificate> = emptyList(),
)

@Serializable
class ApplicationPolicy(
    val packageName: String,
    val downloadUrl: String? = null,
    val sha256Sum: String? = null,
    val signingKeyCerts: List<String> = emptyList(),
    val installType: InstallType = InstallType.INSTALL_TYPE_UNSPECIFIED,
    val permissionGrants: List<PermissionGrant> = emptyList(),
    val minimumVersionCode: Long = 0,
    val defaultPermissionPolicy: PermissionPolicy? = null,
    val managedConfiguration: List<ManagedProperty> = emptyList(),
    val delegatedScopes: List<DelegatedScope> = emptyList(),
    val alwaysOnVpnLockdownExempt: Boolean = false,
    val credentialProviderAllowed: Boolean = false,
    val userControl: UserControlSetting? = null,
    val preferentialNetworkId: PreferentialNetworkId? = null,
)
