package cat.naval.xamanta.proto

import android.os.BatteryManager
import cat.naval.xamanta.models.AutoDateAndTimeZone as ModelAutoDateAndTimeZone
import cat.naval.xamanta.models.CameraAccess as ModelCameraAccess
import cat.naval.xamanta.models.ConfigureWifi as ModelConfigureWifi
import cat.naval.xamanta.models.CredentialProviderPolicyMode as ModelCredentialProviderPolicyMode
import cat.naval.xamanta.models.DeveloperSettings as ModelDeveloperSettings
import cat.naval.xamanta.models.DevicePolicy as ModelDevicePolicy
import cat.naval.xamanta.models.MicrophoneAccess as ModelMicrophoneAccess
import cat.naval.xamanta.models.MtePolicyMode as ModelMtePolicyMode
import cat.naval.xamanta.models.Policy as ModelPolicy
import cat.naval.xamanta.models.PrivateDnsMode as ModelPrivateDnsMode
import cat.naval.xamanta.models.SelfUpdate as ModelSelfUpdate
import cat.naval.xamanta.models.StatusReportingSettings as ModelStatusReportingSettings
import cat.naval.xamanta.models.TetheringSettings as ModelTetheringSettings
import cat.naval.xamanta.models.UntrustedAppsPolicy as ModelUntrustedAppsPolicy
import cat.naval.xamanta.models.UsbDataAccess as ModelUsbDataAccess
import cat.naval.xamanta.models.WifiSecurityLevel as ModelWifiSecurityLevel
import cat.naval.xamanta.protos.AdvancedSecurityOverrides as ProtoAdvancedSecurityOverrides
import cat.naval.xamanta.protos.ApnPolicy as ProtoApnPolicy
import cat.naval.xamanta.protos.AppFunctions as ProtoAppFunctions
import cat.naval.xamanta.protos.AssistContentPolicy as ProtoAssistContentPolicy
import cat.naval.xamanta.protos.BatteryPluggedMode as ProtoBatteryPluggedMode
import cat.naval.xamanta.protos.CredentialProviderPolicyDefault as ProtoCredentialProviderPolicy
import cat.naval.xamanta.protos.DeviceConnectivityManagement as ProtoDeviceConnectivityManagement
import cat.naval.xamanta.protos.DevicePolicy as ProtoDevicePolicy
import cat.naval.xamanta.protos.DeviceRadioState as ProtoDeviceRadioState
import cat.naval.xamanta.protos.DisplaySettings.ScreenBrightnessSettings.ScreenBrightnessMode as ProtoScreenBrightnessMode
import cat.naval.xamanta.protos.DisplaySettings.ScreenTimeoutSettings.ScreenTimeoutMode as ProtoScreenTimeoutMode
import cat.naval.xamanta.protos.EncryptionPolicy as ProtoEncryptionPolicy
import cat.naval.xamanta.protos.LocationMode as ProtoLocationMode
import cat.naval.xamanta.protos.Policy as ProtoPolicy
import cat.naval.xamanta.protos.PreferentialNetworkService as ProtoPreferentialNetworkService
import cat.naval.xamanta.protos.PrintingPolicy as ProtoPrintingPolicy
import cat.naval.xamanta.protos.SelfUpdate as ProtoSelfUpdate
import cat.naval.xamanta.protos.WipeDataFlag as ProtoWipeDataFlag

fun ProtoPolicy.toKotlinModel(): ModelPolicy = ModelPolicy(
    name = name,
    version = version,
    description = description,
    devicePolicy = devicePolicy.toKotlinModel(),
    selfUpdate = selfUpdate.takeIf { hasSelfUpdate() }?.toKotlinModel(),
)

fun ProtoSelfUpdate.toKotlinModel(): ModelSelfUpdate = ModelSelfUpdate(
    versionCode = versionCode,
    downloadUrl = downloadUrl.takeIf { it.isNotEmpty() },
    sha256Sum = sha256Sum.takeIf { it.isNotEmpty() },
)

fun ProtoDevicePolicy.toKotlinModel(): ModelDevicePolicy = ModelDevicePolicy(
    id = id,
    applications = applicationsList.map { it.toKotlinModel() },
    addUserDisabled = addUserDisabled,
    adjustVolumeDisabled = adjustVolumeDisabled,
    factoryResetDisabled = factoryResetDisabled,
    installAppsDisabled = installAppsDisabled,
    mountPhysicalMediaDisabled = mountPhysicalMediaDisabled,
    modifyAccountsDisabled = modifyAccountsDisabled,
    uninstallAppsDisabled = uninstallAppsDisabled,
    keyguardDisabled = keyguardDisabled,
    bluetoothConfigDisabled = bluetoothConfigDisabled,
    cellBroadcastsConfigDisabled = cellBroadcastsConfigDisabled,
    credentialsConfigDisabled = credentialsConfigDisabled,
    mobileNetworksConfigDisabled = mobileNetworksConfigDisabled,
    vpnConfigDisabled = vpnConfigDisabled,
    createWindowsDisabled = createWindowsDisabled,
    networkResetDisabled = networkResetDisabled,
    outgoingBeamDisabled = outgoingBeamDisabled,
    outgoingCallsDisabled = outgoingCallsDisabled,
    removeUserDisabled = removeUserDisabled,
    shareLocationDisabled = locationSharingBlocked,
    smsDisabled = smsDisabled,
    funDisabled = funDisabled,
    screenCaptureDisabled = screenCaptureDisabled,
    airplaneModeDisabled = airplaneModeBlocked,
    kioskCustomization = kioskCustomization.takeIf { hasKioskCustomization() }?.toKotlinModel(),
    maximumTimeToLockMs = maximumTimeToLockMs,
    keyguardDisabledFeatures = keyguardDisabledFeatures,
    permittedInputMethods = permittedInputMethods
        .takeIf { hasPermittedInputMethods() }?.packageNamesList?.toList(),
    permittedAccessibilityServices = permittedAccessibilityServices
        .takeIf { hasPermittedAccessibilityServices() }?.packageNamesList?.toList(),
    systemUpdate = systemUpdate.takeIf { hasSystemUpdate() }?.toKotlinModel(),
    stayOnPluggedModes = stayOnPluggedModesList.fold(0) { mask, mode ->
        mask or when (mode) {
            ProtoBatteryPluggedMode.AC -> BatteryManager.BATTERY_PLUGGED_AC
            ProtoBatteryPluggedMode.USB -> BatteryManager.BATTERY_PLUGGED_USB
            ProtoBatteryPluggedMode.WIRELESS -> BatteryManager.BATTERY_PLUGGED_WIRELESS
            else -> 0
        }
    },
    deviceOwnerLockScreenInfo = deviceOwnerLockScreenInfo
        .takeIf { hasDeviceOwnerLockScreenInfo() }?.toKotlinModel(),
    shortSupportMessage = shortSupportMessage.takeIf { hasShortSupportMessage() }?.toKotlinModel(),
    longSupportMessage = longSupportMessage.takeIf { hasLongSupportMessage() }?.toKotlinModel(),
    factoryResetProtection = factoryResetProtection
        .takeIf { hasFactoryResetProtection() }?.toKotlinModel(),
    bluetoothDisabled = bluetoothDisabled,
    bluetoothContactSharingDisabled = bluetoothContactSharingDisabled,
    dataRoamingDisabled = dataRoamingDisabled,
    setUserIconDisabled = setUserIconDisabled,
    setWallpaperDisabled = setWallpaperDisabled,
    skipFirstUseHintsEnabled = skipFirstUseHintsEnabled,
    printingDisabled = printingPolicy == ProtoPrintingPolicy.PRINTING_DISALLOWED,
    assistContentDisabled =
        assistContentPolicy == ProtoAssistContentPolicy.ASSIST_CONTENT_DISALLOWED,
    locationEnabled = when (locationMode) {
        ProtoLocationMode.LOCATION_ENFORCED -> true
        ProtoLocationMode.LOCATION_DISABLED -> false
        else -> null
    },
    locationConfigDisabled = locationMode == ProtoLocationMode.LOCATION_ENFORCED ||
            locationMode == ProtoLocationMode.LOCATION_DISABLED,
    cameraAccess = cameraAccess.asModel(ModelCameraAccess.CAMERA_ACCESS_USER_CHOICE),
    microphoneAccess =
        microphoneAccess.asModel(ModelMicrophoneAccess.MICROPHONE_ACCESS_USER_CHOICE),
    autoDateAndTimeZone = autoDateAndTimeZone
        .asModel(ModelAutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_USER_CHOICE),
    storageEncryptionRequired =
        encryptionPolicy != ProtoEncryptionPolicy.ENCRYPTION_POLICY_UNSPECIFIED,
    untrustedAppsPolicy = advancedSecurityOverrides.untrustedAppsPolicy
        .asModel(ModelUntrustedAppsPolicy.DISALLOW_INSTALL),
    developerSettings = advancedSecurityOverrides.developerSettings
        .asModel(ModelDeveloperSettings.DEVELOPER_SETTINGS_DISABLED),
    verifyAppsEnforced = when (advancedSecurityOverrides.googlePlayProtectVerifyApps) {
        ProtoAdvancedSecurityOverrides.GooglePlayProtectVerifyApps.VERIFY_APPS_ENFORCED -> true
        ProtoAdvancedSecurityOverrides.GooglePlayProtectVerifyApps.VERIFY_APPS_USER_CHOICE -> false
        else -> null
    },
    commonCriteriaModeEnabled = advancedSecurityOverrides.commonCriteriaMode ==
            ProtoAdvancedSecurityOverrides.CommonCriteriaMode.COMMON_CRITERIA_MODE_ENABLED,
    mtePolicy = mtePolicy.asModelOrNull<ModelMtePolicyMode>(),
    credentialProviderPolicy = when (credentialProviderPolicyDefault) {
        ProtoCredentialProviderPolicy.CREDENTIAL_PROVIDER_DEFAULT_DISALLOWED ->
            ModelCredentialProviderPolicyMode.CREDENTIAL_PROVIDER_DISALLOWED

        ProtoCredentialProviderPolicy.CREDENTIAL_PROVIDER_DEFAULT_DISALLOWED_EXCEPT_SYSTEM ->
            ModelCredentialProviderPolicyMode.CREDENTIAL_PROVIDER_DISALLOWED_EXCEPT_SYSTEM

        ProtoCredentialProviderPolicy.CREDENTIAL_PROVIDER_DEFAULT_ALLOWED ->
            ModelCredentialProviderPolicyMode.CREDENTIAL_PROVIDER_ALLOWED

        else -> null
    },
    wifiEnabled = when (deviceRadioState.wifiState) {
        ProtoDeviceRadioState.WifiState.WIFI_ENABLED -> true
        ProtoDeviceRadioState.WifiState.WIFI_DISABLED -> false
        else -> null
    },
    changeWifiStateDisabled =
        deviceRadioState.wifiState == ProtoDeviceRadioState.WifiState.WIFI_ENABLED ||
                deviceRadioState.wifiState == ProtoDeviceRadioState.WifiState.WIFI_DISABLED,
    ultraWidebandDisabled = deviceRadioState.ultraWidebandState ==
            ProtoDeviceRadioState.UltraWidebandState.ULTRA_WIDEBAND_DISABLED,
    cellularTwoGDisabled = deviceRadioState.cellularTwoGState ==
            ProtoDeviceRadioState.CellularTwoGState.CELLULAR_TWO_G_DISABLED,
    minimumWifiSecurityLevel = deviceRadioState.minimumWifiSecurityLevel
        .asModel(ModelWifiSecurityLevel.OPEN_NETWORK_SECURITY),
    usbDataAccess = deviceConnectivityManagement.usbDataAccess
        .asModel(ModelUsbDataAccess.DISALLOW_USB_FILE_TRANSFER),
    configureWifi = deviceConnectivityManagement.configureWifi
        .asModel(ModelConfigureWifi.ALLOW_CONFIGURING_WIFI),
    tetheringSettings = deviceConnectivityManagement.tetheringSettings
        .asModel(ModelTetheringSettings.ALLOW_ALL_TETHERING),
    wifiDirectDisabled = deviceConnectivityManagement.wifiDirectSettings ==
            ProtoDeviceConnectivityManagement.WifiDirectSettings.DISALLOW_WIFI_DIRECT,
    wifiSsidPolicy = deviceConnectivityManagement.wifiSsidPolicy
        .takeIf { deviceConnectivityManagement.hasWifiSsidPolicy() }?.toKotlinModel(),
    screenBrightnessAutomatic =
        when (displaySettings.screenBrightnessSettings.screenBrightnessMode) {
            ProtoScreenBrightnessMode.BRIGHTNESS_AUTOMATIC -> true
            ProtoScreenBrightnessMode.BRIGHTNESS_FIXED -> false
            else -> null
        },
    screenBrightness = displaySettings.screenBrightnessSettings.screenBrightness
        .takeIf {
            displaySettings.screenBrightnessSettings.screenBrightnessMode ==
                    ProtoScreenBrightnessMode.BRIGHTNESS_FIXED
        }
        ?.coerceIn(MIN_SCREEN_BRIGHTNESS, MAX_SCREEN_BRIGHTNESS),
    brightnessConfigDisabled = displaySettings.screenBrightnessSettings.screenBrightnessMode
        .let {
            it == ProtoScreenBrightnessMode.BRIGHTNESS_AUTOMATIC ||
                    it == ProtoScreenBrightnessMode.BRIGHTNESS_FIXED
        },
    screenTimeoutMs = displaySettings.screenTimeoutSettings.screenTimeoutMs
        .takeIf { screenTimeoutEnforced },
    screenTimeoutConfigDisabled = screenTimeoutEnforced,
    alwaysOnVpnPackage = alwaysOnVpnPackage.takeIf { hasAlwaysOnVpnPackage() }?.toKotlinModel(),
    recommendedGlobalProxy = recommendedGlobalProxy
        .takeIf { hasRecommendedGlobalProxy() }?.toKotlinModel(),
    accountTypesWithManagementDisabled = accountTypesWithManagementDisabledList.toList(),
    persistentPreferredActivities =
        persistentPreferredActivitiesList.mapNotNull { it.toKotlinModel() },
    passwordRequirements = passwordPoliciesList.firstOrNull()?.toKotlinModel(),
    defaultPermissionPolicy = defaultPermissionPolicy.toKotlinModel(),
    bluetoothSharingDisabled = deviceConnectivityManagement.bluetoothSharing ==
            ProtoDeviceConnectivityManagement.BluetoothSharing.BLUETOOTH_SHARING_DISALLOWED,
    privateDnsMode = deviceConnectivityManagement.privateDnsSettings.privateDnsMode
        .asModelOrNull<ModelPrivateDnsMode>(),
    privateDnsHost = deviceConnectivityManagement.privateDnsSettings.privateDnsHost
        .takeIf { it.isNotEmpty() },
    wifiRoamingSettings = deviceConnectivityManagement.wifiRoamingPolicy
        .wifiRoamingSettingsList.mapNotNull { it.toKotlinModel() },
    overrideApnsEnabled = deviceConnectivityManagement.apnPolicy.overrideApns ==
            ProtoApnPolicy.OverrideApns.OVERRIDE_APNS_ENABLED,
    apnSettings = deviceConnectivityManagement.apnPolicy.apnSettingsList
        .mapNotNull { it.toKotlinModel() },
    preferentialNetworkServiceEnabled = preferentialNetworkService ==
            ProtoPreferentialNetworkService.PREFERENTIAL_NETWORK_SERVICE_ENABLED,
    defaultPreferentialNetworkId = deviceConnectivityManagement
        .preferentialNetworkServiceSettings.defaultPreferentialNetworkId.toKotlinModel(),
    preferentialNetworkConfigs = deviceConnectivityManagement
        .preferentialNetworkServiceSettings.preferentialNetworkServiceConfigsList
        .mapNotNull { it.toKotlinModel() },
    defaultApplications = defaultApplicationSettingsList
        .flatMap { it.defaultApplicationsList }
        .mapNotNull { it.toKotlinModel() },
    appFunctionsAllowed = when (appFunctions) {
        ProtoAppFunctions.APP_FUNCTIONS_ALLOWED -> true
        ProtoAppFunctions.APP_FUNCTIONS_DISALLOWED -> false
        else -> null
    },
    wipeEsims = wipeDataFlagsList.contains(ProtoWipeDataFlag.WIPE_ESIMS),
    minimumApiLevel = minimumApiLevel,
    statusReporting = statusReportingSettings.takeIf { hasStatusReportingSettings() }
        ?.toKotlinModel() ?: ModelStatusReportingSettings(),
    choosePrivateKeyRules = choosePrivateKeyRulesList.mapNotNull { it.toKotlinModel() },
    networkConfigurations = openNetworkConfiguration.networkConfigurationsList
        .mapNotNull { it.toKotlinModel() },
    oncCertificates = openNetworkConfiguration.certificatesList.mapNotNull { it.toKotlinModel() },
)

private const val MIN_SCREEN_BRIGHTNESS = 1
private const val MAX_SCREEN_BRIGHTNESS = 255

private val ProtoDevicePolicy.airplaneModeBlocked: Boolean
    get() = deviceRadioState.airplaneModeState ==
            ProtoDeviceRadioState.AirplaneModeState.AIRPLANE_MODE_DISABLED

private val ProtoDevicePolicy.locationSharingBlocked: Boolean
    get() = locationMode == ProtoLocationMode.LOCATION_DISABLED

private val ProtoDevicePolicy.screenTimeoutEnforced: Boolean
    get() = displaySettings.screenTimeoutSettings.screenTimeoutMode ==
            ProtoScreenTimeoutMode.SCREEN_TIMEOUT_ENFORCED
