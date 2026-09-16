package cat.naval.xamanta

import cat.naval.xamanta.proto.toKotlinModel
import cat.naval.xamanta.models.CredentialProviderPolicyMode
import cat.naval.xamanta.models.MtePolicyMode
import cat.naval.xamanta.models.PasswordQuality
import cat.naval.xamanta.models.PermissionPolicy
import cat.naval.xamanta.models.PrivateDnsMode
import cat.naval.xamanta.models.WifiSecurityLevel
import cat.naval.xamanta.models.WifiRoamingMode
import cat.naval.xamanta.models.WifiSsidPolicyType
import cat.naval.xamanta.protos.AdvancedSecurityOverrides
import cat.naval.xamanta.protos.ApnPolicy
import cat.naval.xamanta.protos.AppFunctions
import cat.naval.xamanta.protos.ApplicationReportingSettings
import cat.naval.xamanta.protos.ApplicationSigningKeyCert
import cat.naval.xamanta.protos.ChoosePrivateKeyRule
import cat.naval.xamanta.protos.DefaultApplication
import cat.naval.xamanta.protos.DefaultApplicationSetting
import cat.naval.xamanta.protos.NetworkConfiguration
import cat.naval.xamanta.protos.OncCertificate
import cat.naval.xamanta.protos.OpenNetworkConfiguration
import cat.naval.xamanta.protos.PreferentialNetworkService
import cat.naval.xamanta.protos.PreferentialNetworkServiceConfig
import cat.naval.xamanta.protos.PreferentialNetworkServiceSettings
import cat.naval.xamanta.protos.PrivateDnsSettings
import cat.naval.xamanta.protos.StatusReportingSettings
import cat.naval.xamanta.protos.UserFacingMessage
import cat.naval.xamanta.protos.WifiRoamingPolicy
import cat.naval.xamanta.protos.WifiRoamingSetting
import cat.naval.xamanta.protos.WifiSettings
import cat.naval.xamanta.protos.WipeDataFlag
import cat.naval.xamanta.protos.ApnSetting as ProtoApnSetting
import cat.naval.xamanta.protos.EapSettings as ProtoEapSettings
import cat.naval.xamanta.protos.ApplicationPolicy
import cat.naval.xamanta.protos.AssistContentPolicy
import cat.naval.xamanta.protos.AutoDateAndTimeZone
import cat.naval.xamanta.protos.CameraAccess
import cat.naval.xamanta.protos.CredentialProviderPolicyDefault
import cat.naval.xamanta.protos.DeviceConnectivityManagement
import cat.naval.xamanta.protos.DevicePolicy
import cat.naval.xamanta.protos.DeviceRadioState
import cat.naval.xamanta.protos.DisplaySettings
import cat.naval.xamanta.protos.EncryptionPolicy
import cat.naval.xamanta.protos.FactoryResetProtection
import cat.naval.xamanta.protos.InstallType
import cat.naval.xamanta.protos.KioskCustomization
import cat.naval.xamanta.protos.LocationMode
import cat.naval.xamanta.protos.ManagedProperty
import cat.naval.xamanta.protos.ManagedPropertyBundle
import cat.naval.xamanta.protos.ManagedPropertyBundleArray
import cat.naval.xamanta.protos.ManagedPropertyStringList
import cat.naval.xamanta.protos.MicrophoneAccess
import cat.naval.xamanta.protos.MtePolicy
import cat.naval.xamanta.protos.PackageNameList
import cat.naval.xamanta.protos.PasswordRequirements
import cat.naval.xamanta.protos.PasswordScope
import cat.naval.xamanta.protos.PermissionGrant
import cat.naval.xamanta.protos.PersistentPreferredActivity
import cat.naval.xamanta.protos.PrintingPolicy
import cat.naval.xamanta.protos.ProxyInfo
import cat.naval.xamanta.protos.RequirePasswordUnlock
import cat.naval.xamanta.protos.SystemUpdate
import cat.naval.xamanta.protos.WifiSsid
import cat.naval.xamanta.protos.WifiSsidPolicy
import cat.naval.xamanta.protos.AlwaysOnVpnPackage as ProtoAlwaysOnVpnPackage
import cat.naval.xamanta.models.ApplicationPolicy as ModelApplicationPolicy
import cat.naval.xamanta.models.AutoDateAndTimeZone as ModelAutoDateAndTimeZone
import cat.naval.xamanta.models.CameraAccess as ModelCameraAccess
import cat.naval.xamanta.models.ConfigureWifi as ModelConfigureWifi
import cat.naval.xamanta.models.DeveloperSettings as ModelDeveloperSettings
import cat.naval.xamanta.models.DevicePolicy as ModelDevicePolicy
import cat.naval.xamanta.models.MicrophoneAccess as ModelMicrophoneAccess
import cat.naval.xamanta.models.TetheringSettings as ModelTetheringSettings
import cat.naval.xamanta.models.UntrustedAppsPolicy as ModelUntrustedAppsPolicy
import cat.naval.xamanta.models.UsbDataAccess as ModelUsbDataAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.google.protobuf.ByteString
import java.lang.reflect.Modifier

class PolicyMappingTest {

    @Test
    fun `every model field is populated by the mapper`() {
        val mapped = fullyPopulatedProto().toKotlinModel()
        val defaults = ModelDevicePolicy(id = "")

        val untouched = ModelDevicePolicy::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .filter { field ->
                field.isAccessible = true
                field.get(mapped) == field.get(defaults)
            }
            .map { it.name }

        assertEquals(
            "these fields came out of the mapper at their default — is the mapping missing?",
            emptyList<String>(),
            untouched,
        )
    }

    @Test
    fun `camera and microphone access decide on their own`() {
        val disabled = DevicePolicy.newBuilder()
            .setCameraAccess(CameraAccess.CAMERA_ACCESS_DISABLED)
            .setMicrophoneAccess(MicrophoneAccess.MICROPHONE_ACCESS_DISABLED)
            .build()
            .toKotlinModel()
        assertEquals(ModelCameraAccess.CAMERA_ACCESS_DISABLED, disabled.cameraAccess)
        assertEquals(ModelMicrophoneAccess.MICROPHONE_ACCESS_DISABLED, disabled.microphoneAccess)

        val enforced = DevicePolicy.newBuilder()
            .setCameraAccess(CameraAccess.CAMERA_ACCESS_ENFORCED)
            .setMicrophoneAccess(MicrophoneAccess.MICROPHONE_ACCESS_ENFORCED)
            .build()
            .toKotlinModel()
        assertEquals(ModelCameraAccess.CAMERA_ACCESS_ENFORCED, enforced.cameraAccess)
        assertEquals(ModelMicrophoneAccess.MICROPHONE_ACCESS_ENFORCED, enforced.microphoneAccess)
    }

    @Test
    fun `an unspecified enum asks for nothing`() {
        val silent = DevicePolicy.getDefaultInstance().toKotlinModel()

        assertEquals(ModelCameraAccess.CAMERA_ACCESS_USER_CHOICE, silent.cameraAccess)
        assertEquals(ModelMicrophoneAccess.MICROPHONE_ACCESS_USER_CHOICE, silent.microphoneAccess)
        assertEquals(
            ModelAutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_USER_CHOICE,
            silent.autoDateAndTimeZone,
        )
        assertEquals(ModelConfigureWifi.ALLOW_CONFIGURING_WIFI, silent.configureWifi)
        assertEquals(ModelTetheringSettings.ALLOW_ALL_TETHERING, silent.tetheringSettings)
        assertFalse(silent.airplaneModeDisabled)
        assertFalse(silent.shareLocationDisabled)

        assertEquals(ModelUntrustedAppsPolicy.DISALLOW_INSTALL, silent.untrustedAppsPolicy)
        assertEquals(ModelDeveloperSettings.DEVELOPER_SETTINGS_DISABLED, silent.developerSettings)
        assertEquals(ModelUsbDataAccess.DISALLOW_USB_FILE_TRANSFER, silent.usbDataAccess)
    }

    @Test
    fun `an unknown security override stays restrictive`() {
        val mapped = DevicePolicy.newBuilder()
            .setAdvancedSecurityOverrides(
                AdvancedSecurityOverrides.newBuilder()
                    .setUntrustedAppsPolicyValue(9999)
                    .setDeveloperSettingsValue(9999)
            )
            .build()
            .toKotlinModel()

        assertEquals(ModelUntrustedAppsPolicy.DISALLOW_INSTALL, mapped.untrustedAppsPolicy)
        assertEquals(ModelDeveloperSettings.DEVELOPER_SETTINGS_DISABLED, mapped.developerSettings)
    }

    @Test
    fun `an empty proto maps to a policy that asks for nothing`() {
        val empty = DevicePolicy.getDefaultInstance().toKotlinModel()

        assertNull(empty.locationEnabled)
        assertNull(empty.wifiEnabled)
        assertNull(empty.verifyAppsEnforced)
        assertNull(empty.mtePolicy)
        assertNull(empty.credentialProviderPolicy)
        assertNull(empty.screenBrightnessAutomatic)
        assertNull(empty.screenBrightness)
        assertNull(empty.screenTimeoutMs)
        assertNull(empty.wifiSsidPolicy)
        assertNull(empty.alwaysOnVpnPackage)
        assertNull(empty.recommendedGlobalProxy)
        assertNull(empty.passwordRequirements)
        assertTrue(empty.accountTypesWithManagementDisabled.isEmpty())
        assertTrue(empty.persistentPreferredActivities.isEmpty())
        assertNotEquals(ModelUsbDataAccess.DISALLOW_USB_DATA_TRANSFER, empty.usbDataAccess)
    }

    @Test
    fun `an empty ssid allowlist is preserved for explicit rejection`() {
        val allowNothing = DevicePolicy.newBuilder()
            .setDeviceConnectivityManagement(
                DeviceConnectivityManagement.newBuilder().setWifiSsidPolicy(
                    WifiSsidPolicy.newBuilder()
                        .setWifiSsidPolicyType(WifiSsidPolicy.WifiSsidPolicyType.WIFI_SSID_ALLOWLIST)
                )
            )
            .build()
            .toKotlinModel()

        assertEquals(WifiSsidPolicyType.WIFI_SSID_ALLOWLIST, allowNothing.wifiSsidPolicy!!.type)
        assertTrue(allowNothing.wifiSsidPolicy!!.ssids.isEmpty())
    }

    @Test
    fun `private dns user choice is preserved for non-compliance reporting`() {
        val mapped = DevicePolicy.newBuilder()
            .setDeviceConnectivityManagement(
                DeviceConnectivityManagement.newBuilder().setPrivateDnsSettings(
                    PrivateDnsSettings.newBuilder().setPrivateDnsMode(
                        PrivateDnsSettings.PrivateDnsMode.PRIVATE_DNS_USER_CHOICE
                    )
                )
            )
            .build()
            .toKotlinModel()

        assertEquals(PrivateDnsMode.PRIVATE_DNS_USER_CHOICE, mapped.privateDnsMode)
    }

    @Test
    fun `unspecified roaming mode resets the named SSID to platform default`() {
        val mapped = DevicePolicy.newBuilder()
            .setDeviceConnectivityManagement(
                DeviceConnectivityManagement.newBuilder().setWifiRoamingPolicy(
                    WifiRoamingPolicy.newBuilder().addWifiRoamingSettings(
                        WifiRoamingSetting.newBuilder().setWifiSsid("corp-wifi")
                    )
                )
            )
            .build()
            .toKotlinModel()

        assertEquals(1, mapped.wifiRoamingSettings.size)
        assertEquals(WifiRoamingMode.WIFI_ROAMING_DEFAULT, mapped.wifiRoamingSettings.single().mode)
    }

    @Test
    fun `a persistent preferred activity without an action is dropped`() {
        val incomplete = DevicePolicy.newBuilder()
            .addPersistentPreferredActivities(
                PersistentPreferredActivity.newBuilder().setReceiverActivity("a.b/.C")
            )
            .build()
            .toKotlinModel()

        assertTrue(incomplete.persistentPreferredActivities.isEmpty())
    }

    @Test
    fun `password requirements survive the round trip`() {
        val mapped = fullyPopulatedProto().toKotlinModel().passwordRequirements!!

        assertEquals(PasswordQuality.COMPLEXITY_HIGH, mapped.quality)
        assertEquals(8, mapped.minimumLength)
        assertEquals(3, mapped.historyLength)
        assertEquals(10, mapped.maximumFailedPasswordsForWipe)
        assertEquals(90L * 24 * 60 * 60 * 1000, mapped.expirationTimeoutMs)
        assertTrue(mapped.requireEveryDayUnlock)
        assertTrue(mapped.profileScoped)
        assertTrue(mapped.quality.isComplexityBased)
    }

    @Test
    fun `flattened groups keep their AMAPI meaning`() {
        val mapped = fullyPopulatedProto().toKotlinModel()

        assertEquals(false, mapped.locationEnabled)
        assertTrue(mapped.locationConfigDisabled)
        assertTrue(mapped.shareLocationDisabled)

        assertEquals(false, mapped.wifiEnabled)
        assertTrue(mapped.changeWifiStateDisabled)

        assertEquals(ModelUsbDataAccess.DISALLOW_USB_DATA_TRANSFER, mapped.usbDataAccess)
        assertTrue(mapped.usbDataAccess.blocksFileTransfer)

        assertEquals(ModelConfigureWifi.DISALLOW_CONFIGURING_WIFI, mapped.configureWifi)
        assertTrue(mapped.configureWifi.blocksAdding)

        assertEquals(ModelTetheringSettings.DISALLOW_ALL_TETHERING, mapped.tetheringSettings)
        assertTrue(mapped.tetheringSettings.blocksWifiTethering)

        assertEquals(ModelDeveloperSettings.DEVELOPER_SETTINGS_ALLOWED, mapped.developerSettings)

        assertEquals(WifiSecurityLevel.ENTERPRISE_BIT192_NETWORK_SECURITY, mapped.minimumWifiSecurityLevel)
        assertEquals(MtePolicyMode.MTE_DISABLED, mapped.mtePolicy)
        assertEquals(
            CredentialProviderPolicyMode.CREDENTIAL_PROVIDER_DISALLOWED_EXCEPT_SYSTEM,
            mapped.credentialProviderPolicy,
        )
        assertEquals(WifiSsidPolicyType.WIFI_SSID_ALLOWLIST, mapped.wifiSsidPolicy!!.type)
        assertEquals(listOf("corp-wifi"), mapped.wifiSsidPolicy!!.ssids)
    }

    @Test
    fun `a pac uri wins over host and port`() {
        val mapped = DevicePolicy.newBuilder()
            .setRecommendedGlobalProxy(
                ProxyInfo.newBuilder().setPacUri("https://proxy/pac").setHost("ignored").setPort(1)
            )
            .build()
            .toKotlinModel()
            .recommendedGlobalProxy!!

        assertEquals("https://proxy/pac", mapped.pacUri)
        assertNull(mapped.host)
    }

    @Test
    fun `a proxy with neither pac uri nor host is dropped`() {
        val mapped = DevicePolicy.newBuilder()
            .setRecommendedGlobalProxy(ProxyInfo.newBuilder().setPort(8080))
            .build()
            .toKotlinModel()

        assertNull(mapped.recommendedGlobalProxy)
    }

    @Test
    fun `the mapped screen brightness is clamped to what the platform accepts`() {
        val mapped = DevicePolicy.newBuilder()
            .setDisplaySettings(
                DisplaySettings.newBuilder().setScreenBrightnessSettings(
                    DisplaySettings.ScreenBrightnessSettings.newBuilder()
                        .setScreenBrightnessMode(
                            DisplaySettings.ScreenBrightnessSettings
                                .ScreenBrightnessMode.BRIGHTNESS_FIXED
                        )
                        .setScreenBrightness(9000)
                )
            )
            .build()
            .toKotlinModel()

        assertEquals(255, mapped.screenBrightness)
    }

    @Test
    fun `a proto enum this build does not know falls back instead of throwing`() {
        val mapped = DevicePolicy.newBuilder()
            .setDefaultPermissionPolicyValue(9999)
            .setLocationModeValue(9999)
            .setMtePolicyValue(9999)
            .build()
            .toKotlinModel()

        assertEquals(PermissionPolicy.PROMPT, mapped.defaultPermissionPolicy)
        assertNull(mapped.locationEnabled)
        assertNull(mapped.mtePolicy)
    }

    @Test
    fun `every application field is populated by the mapper`() {
        val mapped = fullyPopulatedApplicationProto().toKotlinModel()
        val defaults = ModelApplicationPolicy(packageName = "")

        val untouched = ModelApplicationPolicy::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .filter { field ->
                field.isAccessible = true
                field.get(mapped) == field.get(defaults)
            }
            .map { it.name }

        assertEquals(
            "these application fields came out of the mapper at their default — mapping missing?",
            emptyList<String>(),
            untouched,
        )
    }

    @Test
    fun `an unspecified application permission policy is no opinion, not PROMPT`() {
        val unspecified = ApplicationPolicy.newBuilder()
            .setPackageName("com.example.app")
            .build()
            .toKotlinModel()
        assertNull(unspecified.defaultPermissionPolicy)

        val prompt = ApplicationPolicy.newBuilder()
            .setPackageName("com.example.app")
            .setDefaultPermissionPolicy(cat.naval.xamanta.protos.PermissionPolicy.PROMPT)
            .build()
            .toKotlinModel()
        assertEquals(PermissionPolicy.PROMPT, prompt.defaultPermissionPolicy)
    }

    @Test
    fun `no preferential network maps to no opinion`() {
        val none = ApplicationPolicy.newBuilder()
            .setPackageName("com.example.app")
            .setPreferentialNetworkId(cat.naval.xamanta.protos.PreferentialNetworkId.NO_PREFERENTIAL_NETWORK)
            .build()
            .toKotlinModel()
        assertNull(none.preferentialNetworkId)
    }

    @Test
    fun `every managed property branch survives the mapping`() {
        val mapped = ApplicationPolicy.newBuilder()
            .setPackageName("com.example.app")
            .addManagedConfiguration(stringProperty("text", "hello"))
            .addManagedConfiguration(
                ManagedProperty.newBuilder().setKey("flag").setBoolValue(true)
            )
            .addManagedConfiguration(ManagedProperty.newBuilder().setKey("count").setIntValue(3))
            .addManagedConfiguration(
                ManagedProperty.newBuilder().setKey("list").setStringListValue(
                    ManagedPropertyStringList.newBuilder().addValues("a").addValues("b")
                )
            )
            .addManagedConfiguration(
                ManagedProperty.newBuilder().setKey("nested").setBundleValue(
                    ManagedPropertyBundle.newBuilder().addProperties(stringProperty("inner", "x"))
                )
            )
            .addManagedConfiguration(
                ManagedProperty.newBuilder().setKey("nestedArray").setBundleArrayValue(
                    ManagedPropertyBundleArray.newBuilder().addBundles(
                        ManagedPropertyBundle.newBuilder()
                            .addProperties(stringProperty("deep", "y"))
                    )
                )
            )
            .build()
            .toKotlinModel()
            .managedConfiguration
            .associateBy { it.key }

        assertEquals("hello", mapped.getValue("text").stringValue)
        assertEquals(true, mapped.getValue("flag").boolValue)
        assertEquals(3, mapped.getValue("count").intValue)
        assertEquals(listOf("a", "b"), mapped.getValue("list").stringListValue)
        assertEquals("x", mapped.getValue("nested").bundleValue?.single()?.stringValue)
        assertEquals(
            "y",
            mapped.getValue("nestedArray").bundleArrayValue?.single()?.single()?.stringValue,
        )
        assertNull(mapped.getValue("text").boolValue)
    }

    private fun stringProperty(key: String, value: String): ManagedProperty.Builder =
        ManagedProperty.newBuilder().setKey(key).setStringValue(value)

    private fun signingKeyCert(fingerprint: ByteArray): ApplicationSigningKeyCert.Builder =
        ApplicationSigningKeyCert.newBuilder()
            .setSigningKeyCertFingerprintSha256(ByteString.copyFrom(fingerprint))

    @Test
    fun `signing key certs map to lowercase hex, and a malformed one stays unmatchable`() {
        val mapped = ApplicationPolicy.newBuilder()
            .setPackageName("com.example")
            .addSigningKeyCerts(signingKeyCert(ByteArray(32) { 0xAB.toByte() }))
            .addSigningKeyCerts(signingKeyCert(ByteArray(4) { 0x01 }))
            .build()
            .toKotlinModel()

        assertEquals(listOf("ab".repeat(32), "01010101"), mapped.signingKeyCerts)
        assertNotEquals(64, mapped.signingKeyCerts.last().length)
    }

    @Test
    fun `an application policy with no signing key certs maps to an empty list`() {
        val mapped = ApplicationPolicy.newBuilder()
            .setPackageName("com.example")
            .build()
            .toKotlinModel()

        assertEquals(emptyList<String>(), mapped.signingKeyCerts)
    }

    private fun fullyPopulatedApplicationProto(): ApplicationPolicy = ApplicationPolicy.newBuilder()
        .setPackageName("com.example.kiosk")
        .setInstallType(InstallType.KIOSK)
        .setDownloadUrl("https://example.test/app.apk")
        .setSha256Sum("abc")
        .addSigningKeyCerts(signingKeyCert(ByteArray(32) { 0xAB.toByte() }))
        .setMinimumVersionCode(7)
        .addPermissionGrants(
            PermissionGrant.newBuilder()
                .setPermission("android.permission.CAMERA")
                .setPolicy(cat.naval.xamanta.protos.PermissionPolicy.GRANT)
        )
        .setDefaultPermissionPolicy(cat.naval.xamanta.protos.PermissionPolicy.DENY)
        .addManagedConfiguration(stringProperty("server", "https://example.test"))
        .addDelegatedScopes(ApplicationPolicy.DelegatedScope.MANAGED_CONFIGURATIONS)
        .setAlwaysOnVpnLockdownExemption(
            ApplicationPolicy.AlwaysOnVpnLockdownExemption.VPN_LOCKDOWN_EXEMPTION
        )
        .setCredentialProviderPolicy(
            ApplicationPolicy.CredentialProviderPolicy.CREDENTIAL_PROVIDER_ALLOWED
        )
        .setUserControlSettings(ApplicationPolicy.UserControlSettings.USER_CONTROL_DISALLOWED)
        .setPreferentialNetworkId(cat.naval.xamanta.protos.PreferentialNetworkId.PREFERENTIAL_NETWORK_ID_TWO)
        .build()

    private fun fullyPopulatedProto(): DevicePolicy = DevicePolicy.newBuilder()
        .setId("policy-id")
        .addApplications(
            ApplicationPolicy.newBuilder()
                .setPackageName("com.example.kiosk")
                .setInstallType(InstallType.KIOSK)
                .setDownloadUrl("https://example.test/app.apk")
                .setSha256Sum("abc")
                .setMinimumVersionCode(7)
                .addPermissionGrants(
                    PermissionGrant.newBuilder()
                        .setPermission("android.permission.CAMERA")
                        .setPolicy(cat.naval.xamanta.protos.PermissionPolicy.GRANT)
                )
        )
        .setAddUserDisabled(true)
        .setAdjustVolumeDisabled(true)
        .setFactoryResetDisabled(true)
        .setInstallAppsDisabled(true)
        .setMountPhysicalMediaDisabled(true)
        .setModifyAccountsDisabled(true)
        .setUninstallAppsDisabled(true)
        .setKeyguardDisabled(true)
        .setBluetoothConfigDisabled(true)
        .setCellBroadcastsConfigDisabled(true)
        .setCredentialsConfigDisabled(true)
        .setMobileNetworksConfigDisabled(true)
        .setVpnConfigDisabled(true)
        .setCreateWindowsDisabled(true)
        .setNetworkResetDisabled(true)
        .setOutgoingBeamDisabled(true)
        .setOutgoingCallsDisabled(true)
        .setRemoveUserDisabled(true)
        .setSmsDisabled(true)
        .setFunDisabled(true)
        .setScreenCaptureDisabled(true)
        .setMaximumTimeToLockMs(60_000)
        .setKeyguardDisabledFeatures(1)
        .setPermittedInputMethods(PackageNameList.newBuilder().addPackageNames("com.example.ime"))
        .setPermittedAccessibilityServices(
            PackageNameList.newBuilder().addPackageNames("com.example.a11y")
        )
        .setSystemUpdate(
            SystemUpdate.newBuilder().setType(SystemUpdate.SystemUpdateType.WINDOWED)
                .setStartMinutes(120).setEndMinutes(240)
        )
        .addStayOnPluggedModes(cat.naval.xamanta.protos.BatteryPluggedMode.AC)
        .setDeviceOwnerLockScreenInfo(
            UserFacingMessage.newBuilder().setDefaultMessage("Property of Xamanta")
        )
        .setShortSupportMessage(
            UserFacingMessage.newBuilder()
                .setDefaultMessage("short")
                .putLocalizedMessages("es", "corto")
        )
        .setLongSupportMessage(UserFacingMessage.newBuilder().setDefaultMessage("long"))
        .setFactoryResetProtection(
            FactoryResetProtection.newBuilder().addAccountIds("admin@example.test")
        )
        .setDefaultPermissionPolicy(cat.naval.xamanta.protos.PermissionPolicy.GRANT)
        .setKioskCustomization(
            KioskCustomization.newBuilder()
                .setPowerButtonActions(KioskCustomization.PowerButtonActions.POWER_BUTTON_BLOCKED)
                .setStatusBar(
                    KioskCustomization.StatusBar.NOTIFICATIONS_AND_SYSTEM_INFO_DISABLED
                )
        )
        .setBluetoothDisabled(true)
        .setBluetoothContactSharingDisabled(true)
        .setDataRoamingDisabled(true)
        .setSetUserIconDisabled(true)
        .setSetWallpaperDisabled(true)
        .setSkipFirstUseHintsEnabled(true)
        .setPrintingPolicy(PrintingPolicy.PRINTING_DISALLOWED)
        .setAssistContentPolicy(AssistContentPolicy.ASSIST_CONTENT_DISALLOWED)
        .setLocationMode(LocationMode.LOCATION_DISABLED)
        .setCameraAccess(CameraAccess.CAMERA_ACCESS_ENFORCED)
        .setMicrophoneAccess(MicrophoneAccess.MICROPHONE_ACCESS_ENFORCED)
        .setAutoDateAndTimeZone(AutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_ENFORCED)
        .setEncryptionPolicy(EncryptionPolicy.ENABLED_WITH_PASSWORD)
        .setMtePolicy(MtePolicy.MTE_DISABLED)
        .setCredentialProviderPolicyDefault(
            CredentialProviderPolicyDefault.CREDENTIAL_PROVIDER_DEFAULT_DISALLOWED_EXCEPT_SYSTEM
        )
        .setAdvancedSecurityOverrides(
            AdvancedSecurityOverrides.newBuilder()
                .setUntrustedAppsPolicy(
                    AdvancedSecurityOverrides.UntrustedAppsPolicy.ALLOW_INSTALL_DEVICE_WIDE
                )
                .setGooglePlayProtectVerifyApps(
                    AdvancedSecurityOverrides.GooglePlayProtectVerifyApps.VERIFY_APPS_ENFORCED
                )
                .setDeveloperSettings(
                    AdvancedSecurityOverrides.DeveloperSettings.DEVELOPER_SETTINGS_ALLOWED
                )
                .setCommonCriteriaMode(
                    AdvancedSecurityOverrides.CommonCriteriaMode.COMMON_CRITERIA_MODE_ENABLED
                )
        )
        .setDeviceRadioState(
            DeviceRadioState.newBuilder()
                .setWifiState(DeviceRadioState.WifiState.WIFI_DISABLED)
                .setAirplaneModeState(DeviceRadioState.AirplaneModeState.AIRPLANE_MODE_DISABLED)
                .setUltraWidebandState(
                    DeviceRadioState.UltraWidebandState.ULTRA_WIDEBAND_DISABLED
                )
                .setCellularTwoGState(
                    DeviceRadioState.CellularTwoGState.CELLULAR_TWO_G_DISABLED
                )
                .setMinimumWifiSecurityLevel(
                    DeviceRadioState.MinimumWifiSecurityLevel.ENTERPRISE_BIT192_NETWORK_SECURITY
                )
        )
        .setDeviceConnectivityManagement(
            DeviceConnectivityManagement.newBuilder()
                .setUsbDataAccess(
                    DeviceConnectivityManagement.UsbDataAccess.DISALLOW_USB_DATA_TRANSFER
                )
                .setConfigureWifi(
                    DeviceConnectivityManagement.ConfigureWifi.DISALLOW_CONFIGURING_WIFI
                )
                .setWifiDirectSettings(
                    DeviceConnectivityManagement.WifiDirectSettings.DISALLOW_WIFI_DIRECT
                )
                .setTetheringSettings(
                    DeviceConnectivityManagement.TetheringSettings.DISALLOW_ALL_TETHERING
                )
                .setWifiSsidPolicy(
                    WifiSsidPolicy.newBuilder()
                        .setWifiSsidPolicyType(
                            WifiSsidPolicy.WifiSsidPolicyType.WIFI_SSID_ALLOWLIST
                        )
                        .addWifiSsids(WifiSsid.newBuilder().setWifiSsid("corp-wifi"))
                )
                .setBluetoothSharing(
                    DeviceConnectivityManagement.BluetoothSharing.BLUETOOTH_SHARING_DISALLOWED
                )
                .setPrivateDnsSettings(
                    PrivateDnsSettings.newBuilder()
                        .setPrivateDnsMode(
                            PrivateDnsSettings.PrivateDnsMode.PRIVATE_DNS_SPECIFIED_HOST
                        )
                        .setPrivateDnsHost("dns.example.test")
                )
                .setWifiRoamingPolicy(
                    WifiRoamingPolicy.newBuilder().addWifiRoamingSettings(
                        WifiRoamingSetting.newBuilder()
                            .setWifiSsid("corp-wifi")
                            .setWifiRoamingMode(
                                WifiRoamingSetting.WifiRoamingMode.WIFI_ROAMING_AGGRESSIVE
                            )
                    )
                )
                .setApnPolicy(
                    ApnPolicy.newBuilder()
                        .setOverrideApns(ApnPolicy.OverrideApns.OVERRIDE_APNS_ENABLED)
                        .addApnSettings(
                            ProtoApnSetting.newBuilder()
                                .setApn("corp.apn")
                                .setDisplayName("Corp")
                                .addApnTypes(ProtoApnSetting.ApnType.DEFAULT)
                                .setProtocol(ProtoApnSetting.Protocol.IPV4V6)
                                .setRoamingProtocol(ProtoApnSetting.Protocol.IP)
                                .setAuthType(ProtoApnSetting.AuthType.PAP)
                                .setUsername("user")
                                .setPassword("secret")
                                .setMmsc("http://mms.example.test")
                                .setMmsProxyAddress("mmsproxy.example.test")
                                .setMmsProxyPort(80)
                                .setProxyAddress("proxy.example.test")
                                .setProxyPort(8080)
                                .setMvnoType(ProtoApnSetting.MvnoType.SPN)
                                .setNumericOperatorId("21401")
                                .setCarrierId(7)
                                .addNetworkTypes(ProtoApnSetting.NetworkType.LTE)
                                .setMtuV4(1400)
                                .setMtuV6(1420)
                                .setAlwaysOnSetting(ProtoApnSetting.AlwaysOnSetting.ALWAYS_ON)
                        )
                )
                .setPreferentialNetworkServiceSettings(
                    PreferentialNetworkServiceSettings.newBuilder()
                        .setDefaultPreferentialNetworkId(
                            cat.naval.xamanta.protos.PreferentialNetworkId
                                .PREFERENTIAL_NETWORK_ID_THREE
                        )
                        .addPreferentialNetworkServiceConfigs(
                            PreferentialNetworkServiceConfig.newBuilder()
                                .setPreferentialNetworkId(
                                    cat.naval.xamanta.protos.PreferentialNetworkId
                                        .PREFERENTIAL_NETWORK_ID_THREE
                                )
                                .setNonMatchingNetworks(
                                    PreferentialNetworkServiceConfig.NonMatchingNetworks
                                        .NON_MATCHING_NETWORKS_DISALLOWED
                                )
                                .setFallbackToDefaultConnection(
                                    PreferentialNetworkServiceConfig.FallbackToDefaultConnection
                                        .FALLBACK_TO_DEFAULT_CONNECTION_DISALLOWED
                                )
                        )
                )
        )
        .setPreferentialNetworkService(
            PreferentialNetworkService.PREFERENTIAL_NETWORK_SERVICE_ENABLED
        )
        .setDisplaySettings(
            DisplaySettings.newBuilder()
                .setScreenBrightnessSettings(
                    DisplaySettings.ScreenBrightnessSettings.newBuilder()
                        .setScreenBrightnessMode(
                            DisplaySettings.ScreenBrightnessSettings
                                .ScreenBrightnessMode.BRIGHTNESS_FIXED
                        )
                        .setScreenBrightness(120)
                )
                .setScreenTimeoutSettings(
                    DisplaySettings.ScreenTimeoutSettings.newBuilder()
                        .setScreenTimeoutMode(
                            DisplaySettings.ScreenTimeoutSettings
                                .ScreenTimeoutMode.SCREEN_TIMEOUT_ENFORCED
                        )
                        .setScreenTimeoutMs(30_000)
                )
        )
        .setAlwaysOnVpnPackage(
            ProtoAlwaysOnVpnPackage.newBuilder()
                .setPackageName("com.example.vpn")
                .setLockdownEnabled(true)
        )
        .setRecommendedGlobalProxy(
            ProxyInfo.newBuilder().setHost("proxy.example.test").setPort(3128)
                .addExcludedHosts("localhost")
        )
        .addAccountTypesWithManagementDisabled("com.google")
        .addPersistentPreferredActivities(
            PersistentPreferredActivity.newBuilder()
                .setReceiverActivity("com.example/.MainActivity")
                .addActions("android.intent.action.VIEW")
                .addCategories("android.intent.category.DEFAULT")
        )
        .addPasswordPolicies(
            PasswordRequirements.newBuilder()
                .setPasswordQuality(cat.naval.xamanta.protos.PasswordQuality.COMPLEXITY_HIGH)
                .setPasswordMinimumLength(8)
                .setPasswordMinimumLetters(2)
                .setPasswordMinimumLowerCase(1)
                .setPasswordMinimumUpperCase(1)
                .setPasswordMinimumNonLetter(1)
                .setPasswordMinimumNumeric(1)
                .setPasswordMinimumSymbols(1)
                .setPasswordHistoryLength(3)
                .setMaximumFailedPasswordsForWipe(10)
                .setPasswordExpirationTimeoutMs(90L * 24 * 60 * 60 * 1000)
                .setRequirePasswordUnlock(RequirePasswordUnlock.REQUIRE_EVERY_DAY)
                .setPasswordScope(PasswordScope.SCOPE_PROFILE)
        )
        .addDefaultApplicationSettings(
            DefaultApplicationSetting.newBuilder().addDefaultApplications(
                DefaultApplication.newBuilder()
                    .setPackageName("com.example.browser")
                    .setDefaultApplicationType(
                        DefaultApplication.DefaultApplicationType.DEFAULT_BROWSER
                    )
                    .addDefaultApplicationScopes(
                        DefaultApplication.DefaultApplicationScope.SCOPE_WORK_PROFILE
                    )
            )
        )
        .setAppFunctions(AppFunctions.APP_FUNCTIONS_DISALLOWED)
        .addWipeDataFlags(WipeDataFlag.WIPE_ESIMS)
        .setMinimumApiLevel(30)
        .setStatusReportingSettings(
            StatusReportingSettings.newBuilder()
                .setApplicationReportsEnabled(true)
                .setDeviceSettingsEnabled(true)
                .setSoftwareInfoEnabled(true)
                .setMemoryInfoEnabled(true)
                .setNetworkInfoEnabled(true)
                .setDisplayInfoEnabled(true)
                .setPowerManagementEventsEnabled(true)
                .setHardwareStatusEnabled(true)
                .setSystemPropertiesEnabled(true)
                .setCommonCriteriaModeEnabled(true)
                .setDefaultApplicationInfoReportingEnabled(true)
                .setApplicationReportingSettings(
                    ApplicationReportingSettings.newBuilder().setIncludeRemovedApps(true)
                )
        )
        .addChoosePrivateKeyRules(
            ChoosePrivateKeyRule.newBuilder()
                .setUrlPattern(""".*\.example\.test""")
                .setPrivateKeyAlias("corp-client")
                .addPackageNames("com.example.app")
        )
        .setOpenNetworkConfiguration(
            OpenNetworkConfiguration.newBuilder()
                .addNetworkConfigurations(
                    NetworkConfiguration.newBuilder()
                        .setGuid("net-1")
                        .setName("Corp")
                        .setWifi(
                            WifiSettings.newBuilder()
                                .setSsid("corp-wifi")
                                .setSecurity(WifiSettings.Security.WPA_EAP)
                                .setHiddenSsid(true)
                                .setAutoConnect(true)
                                .setMacAddressRandomizationMode(
                                    WifiSettings.MacAddressRandomizationMode.AUTOMATIC
                                )
                                .setEap(
                                    ProtoEapSettings.newBuilder()
                                        .setOuter(ProtoEapSettings.Outer.EAP_TTLS)
                                        .setInner(ProtoEapSettings.Inner.MSCHAPV2)
                                        .setIdentity("user@example.test")
                                        .setAnonymousIdentity("anon@example.test")
                                        .setPassword("secret")
                                        .addDomainSuffixMatch("example.test")
                                        .addServerCaRefs("ca-1")
                                        .setClientCertType(ProtoEapSettings.ClientCertType.REF)
                                        .setClientCertRef("client-1")
                                )
                        )
                        .setProxySettings(
                            ProxyInfo.newBuilder().setHost("proxy.example.test").setPort(3128)
                        )
                )
                .addCertificates(
                    OncCertificate.newBuilder()
                        .setGuid("ca-1")
                        .setType(OncCertificate.Type.SERVER)
                        .setX509("TWFuIGlz")
                )
                .addCertificates(
                    OncCertificate.newBuilder()
                        .setGuid("client-1")
                        .setType(OncCertificate.Type.CLIENT)
                        .setPkcs12("TWFuIGlz")
                        .setPkcs12Password("pfx")
                )
        )
        .build()
}
