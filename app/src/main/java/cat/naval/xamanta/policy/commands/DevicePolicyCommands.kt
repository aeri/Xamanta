package cat.naval.xamanta.policy.commands

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.ConfigureWifi
import cat.naval.xamanta.models.CredentialProviderPolicyMode
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.MtePolicyMode
import cat.naval.xamanta.models.PermissionPolicy
import cat.naval.xamanta.models.PrivateDnsMode
import cat.naval.xamanta.models.StatusBarMode
import cat.naval.xamanta.models.TetheringSettings
import cat.naval.xamanta.models.UsbDataAccess
import cat.naval.xamanta.models.UserControlSetting
import cat.naval.xamanta.models.WifiSecurityLevel
import cat.naval.xamanta.policy.commands.apps.DefaultPermissionPolicyCommand
import cat.naval.xamanta.policy.commands.apps.PermittedAccessibilityServicesCommand
import cat.naval.xamanta.policy.commands.apps.PermittedInputMethodsCommand
import cat.naval.xamanta.policy.commands.apps.PersistentPreferredActivitiesCommand
import cat.naval.xamanta.policy.commands.apps.DefaultApplicationsCommand
import cat.naval.xamanta.policy.commands.connectivity.ApnPolicyCommand
import cat.naval.xamanta.policy.commands.connectivity.OpenNetworkConfigurationCommand
import cat.naval.xamanta.policy.commands.connectivity.PrivateDnsCommand
import cat.naval.xamanta.policy.commands.connectivity.WifiRoamingPolicyCommand
import cat.naval.xamanta.policy.commands.device.MinimumApiLevelCommand
import cat.naval.xamanta.policy.commands.security.AppFunctionsCommand
import cat.naval.xamanta.policy.commands.apps.UserControlDisabledPackagesCommand
import cat.naval.xamanta.policy.commands.connectivity.AlwaysOnVpnPackageCommand
import cat.naval.xamanta.policy.commands.connectivity.MinimumWifiSecurityLevelCommand
import cat.naval.xamanta.policy.commands.connectivity.PreferentialNetworkCommand
import cat.naval.xamanta.policy.commands.connectivity.RecommendedGlobalProxyCommand
import cat.naval.xamanta.policy.commands.connectivity.UsbDataSignalingCommand
import cat.naval.xamanta.policy.commands.connectivity.WifiConfigsLockdownCommand
import cat.naval.xamanta.policy.commands.connectivity.WifiEnabledCommand
import cat.naval.xamanta.policy.commands.connectivity.WifiSsidPolicyCommand
import cat.naval.xamanta.policy.commands.device.CameraDisabledCommand
import cat.naval.xamanta.policy.commands.device.DeviceOwnerLockScreenInfoCommand
import cat.naval.xamanta.policy.commands.device.KeyguardDisabledCommand
import cat.naval.xamanta.policy.commands.device.KeyguardDisabledFeaturesCommand
import cat.naval.xamanta.policy.commands.device.LongSupportMessageCommand
import cat.naval.xamanta.policy.commands.device.MaximumTimeToLockCommand
import cat.naval.xamanta.policy.commands.device.ScreenCaptureDisabledCommand
import cat.naval.xamanta.policy.commands.device.ShortSupportMessageCommand
import cat.naval.xamanta.policy.commands.device.StatusBarDisabledCommand
import cat.naval.xamanta.policy.commands.device.asksStatusBarDisabled
import cat.naval.xamanta.policy.commands.display.ScreenBrightnessCommand
import cat.naval.xamanta.policy.commands.display.ScreenTimeoutCommand
import cat.naval.xamanta.policy.commands.password.MaximumFailedPasswordsForWipeCommand
import cat.naval.xamanta.policy.commands.password.PasswordComplexityCommand
import cat.naval.xamanta.policy.commands.password.PasswordExpirationTimeoutCommand
import cat.naval.xamanta.policy.commands.password.PasswordHistoryLengthCommand
import cat.naval.xamanta.policy.commands.password.PasswordQualityCommand
import cat.naval.xamanta.policy.commands.password.PasswordScopeCommand
import cat.naval.xamanta.policy.commands.password.RequirePasswordUnlockCommand
import cat.naval.xamanta.policy.commands.security.CommonCriteriaModeCommand
import cat.naval.xamanta.policy.commands.security.CredentialProviderPolicyCommand
import cat.naval.xamanta.policy.commands.security.FactoryResetProtectionCommand
import cat.naval.xamanta.policy.commands.security.MtePolicyCommand
import cat.naval.xamanta.policy.commands.security.StorageEncryptionCommand
import cat.naval.xamanta.policy.commands.system.AccountManagementCommand
import cat.naval.xamanta.policy.commands.system.AutoTimeCommand
import cat.naval.xamanta.policy.commands.system.AutoTimeZoneCommand
import cat.naval.xamanta.policy.commands.system.LocationEnabledCommand
import cat.naval.xamanta.policy.commands.system.SkipFirstUseHintsCommand
import cat.naval.xamanta.policy.commands.system.StayOnPluggedModesCommand
import cat.naval.xamanta.policy.commands.system.SystemUpdateCommand
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

fun buildDevicePolicyCommands(scope: PolicyScope): List<PolicyCommand> = buildList {
    addAll(lollipopCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) addAll(marshmallowCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) addAll(nougatCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) addAll(oreoCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) addAll(pieCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) addAll(qCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) addAll(rCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) addAll(sCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) addAll(tiramisuCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        addAll(upsideDownCakeCommands(scope))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        addAll(vanillaIceCreamCommands(scope))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) addAll(baklavaCommands(scope))
}

private fun lollipopCommands(scope: PolicyScope): List<PolicyCommand> = buildList {
    addAll(DEVICE_POLICY_SPECS.unsupportedOn(scope.policy))
    add(CameraDisabledCommand(scope))
    add(ScreenCaptureDisabledCommand(scope))
    add(MaximumTimeToLockCommand(scope))
    add(KeyguardDisabledFeaturesCommand(scope))
    add(AutoTimeCommand(scope))
    add(AutoTimeZoneCommand(scope))
    add(SkipFirstUseHintsCommand(scope))
    add(AccountManagementCommand(scope))
    add(PermittedInputMethodsCommand(scope))
    add(PermittedAccessibilityServicesCommand(scope))
    add(PersistentPreferredActivitiesCommand(scope))
    add(DefaultApplicationsCommand(scope))
    add(MinimumApiLevelCommand(scope))
    add(OpenNetworkConfigurationCommand(scope))
    add(WifiEnabledCommand(scope))
    add(RecommendedGlobalProxyCommand(scope))
    add(StorageEncryptionCommand(scope))
    add(PasswordScopeCommand(scope))
    add(MaximumFailedPasswordsForWipeCommand(scope))
    add(PasswordQualityCommand(scope))
    add(PasswordHistoryLengthCommand(scope))
    add(PasswordExpirationTimeoutCommand(scope))
}

internal val DEVICE_POLICY_SPECS = listOf(
    ApiSpec<DevicePolicy>("usbDataAccess", Build.VERSION_CODES.S) {
        it.usbDataAccess == UsbDataAccess.DISALLOW_USB_DATA_TRANSFER
    },
    ApiSpec("configureWifi", Build.VERSION_CODES.M) {
        it.configureWifi == ConfigureWifi.DISALLOW_CONFIGURING_WIFI
    },
    ApiSpec("configureWifi", Build.VERSION_CODES.TIRAMISU) {
        it.configureWifi == ConfigureWifi.DISALLOW_ADD_WIFI_CONFIG
    },
    ApiSpec("tetheringSettings", Build.VERSION_CODES.TIRAMISU) {
        it.tetheringSettings == TetheringSettings.DISALLOW_WIFI_TETHERING
    },
    ApiSpec("wifiSsidPolicy", Build.VERSION_CODES.TIRAMISU) { it.wifiSsidPolicy != null },
    ApiSpec("minimumWifiSecurityLevel", Build.VERSION_CODES.TIRAMISU) {
        it.minimumWifiSecurityLevel != WifiSecurityLevel.OPEN_NETWORK_SECURITY
    },
    ApiSpec("preferentialNetworkId", Build.VERSION_CODES.TIRAMISU) {
        it.preferentialNetworkServiceEnabled ||
                it.defaultPreferentialNetworkId != null ||
                it.preferentialNetworkConfigs.isNotEmpty()
    },
    ApiSpec("wifiRoamingPolicy", Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        it.wifiRoamingSettings.isNotEmpty()
    },
    ApiSpec("apnPolicy", Build.VERSION_CODES.P) {
        it.overrideApnsEnabled || it.apnSettings.isNotEmpty()
    },
    ApiSpec("privateDnsSettings", Build.VERSION_CODES.Q) {
        it.privateDnsMode != null && it.privateDnsMode != PrivateDnsMode.PRIVATE_DNS_USER_CHOICE
    },
    ApiSpec("stayOnPluggedModes", Build.VERSION_CODES.M) { it.stayOnPluggedModes != 0 },
    ApiSpec("keyguardDisabled", Build.VERSION_CODES.M) { it.keyguardDisabled },
    ApiSpec("kioskCustomization.statusBar", Build.VERSION_CODES.M) { it.asksStatusBarDisabled() },
    ApiSpec("systemUpdate", Build.VERSION_CODES.M) { it.systemUpdate != null },
    ApiSpec("defaultPermissionPolicy", Build.VERSION_CODES.M) {
        it.defaultPermissionPolicy != PermissionPolicy.PROMPT
    },
    ApiSpec("shortSupportMessage", Build.VERSION_CODES.N) { it.shortSupportMessage != null },
    ApiSpec("longSupportMessage", Build.VERSION_CODES.N) { it.longSupportMessage != null },
    ApiSpec("deviceOwnerLockScreenInfo", Build.VERSION_CODES.N) {
        it.deviceOwnerLockScreenInfo != null
    },
    ApiSpec("alwaysOnVpnPackage", Build.VERSION_CODES.N) { it.alwaysOnVpnPackage != null },
    ApiSpec("requirePasswordUnlock", Build.VERSION_CODES.O) {
        it.passwordRequirements?.requireEveryDayUnlock == true
    },
    ApiSpec("screenBrightness", Build.VERSION_CODES.P) { it.screenBrightnessAutomatic != null },
    ApiSpec("screenTimeout", Build.VERSION_CODES.P) { it.screenTimeoutMs != null },
    ApiSpec("factoryResetProtection", Build.VERSION_CODES.R) { it.factoryResetProtection != null },
    ApiSpec("userControlDisabledPackages", Build.VERSION_CODES.R) { policy ->
        policy.applications.any { it.userControl == UserControlSetting.USER_CONTROL_DISALLOWED }
    },
    ApiSpec("locationEnabled", Build.VERSION_CODES.R) { it.locationEnabled != null },
    ApiSpec("commonCriteriaMode", Build.VERSION_CODES.R) { it.commonCriteriaModeEnabled },
    ApiSpec("mtePolicy", Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        it.mtePolicy == MtePolicyMode.MTE_ENABLED || it.mtePolicy == MtePolicyMode.MTE_DISABLED
    },
    ApiSpec("credentialProviderPolicy", Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        it.credentialProviderPolicy != null &&
                it.credentialProviderPolicy != CredentialProviderPolicyMode.CREDENTIAL_PROVIDER_ALLOWED
    },
    ApiSpec("appFunctions", Build.VERSION_CODES.BAKLAVA) { it.appFunctionsAllowed == false },
)

@RequiresApi(Build.VERSION_CODES.M)
private fun marshmallowCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    StayOnPluggedModesCommand(scope),
    KeyguardDisabledCommand(scope),
    StatusBarDisabledCommand(scope),
    SystemUpdateCommand(scope),
    DefaultPermissionPolicyCommand(scope),
    WifiConfigsLockdownCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.N)
private fun nougatCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    ShortSupportMessageCommand(scope),
    LongSupportMessageCommand(scope),
    DeviceOwnerLockScreenInfoCommand(scope),
    AlwaysOnVpnPackageCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.O)
private fun oreoCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    RequirePasswordUnlockCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.P)
private fun pieCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    ScreenBrightnessCommand(scope),
    ScreenTimeoutCommand(scope),
    ApnPolicyCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.Q)
private fun qCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    PrivateDnsCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.R)
private fun rCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    FactoryResetProtectionCommand(scope),
    UserControlDisabledPackagesCommand(scope),
    LocationEnabledCommand(scope),
    CommonCriteriaModeCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.S)
private fun sCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    PasswordComplexityCommand(scope),
    UsbDataSignalingCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun tiramisuCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    MinimumWifiSecurityLevelCommand(scope),
    WifiSsidPolicyCommand(scope),
    PreferentialNetworkCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun upsideDownCakeCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    MtePolicyCommand(scope),
    CredentialProviderPolicyCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun vanillaIceCreamCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    WifiRoamingPolicyCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private fun baklavaCommands(scope: PolicyScope): List<PolicyCommand> = listOf(
    AppFunctionsCommand(scope),
)
