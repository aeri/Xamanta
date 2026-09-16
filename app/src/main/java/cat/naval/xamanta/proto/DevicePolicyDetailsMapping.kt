package cat.naval.xamanta.proto

import cat.naval.xamanta.models.AlwaysOnVpnPackage as ModelAlwaysOnVpnPackage
import cat.naval.xamanta.models.DefaultApplication as ModelDefaultApplication
import cat.naval.xamanta.models.DefaultApplicationType as ModelDefaultApplicationType
import cat.naval.xamanta.models.DeviceSettingsAccess as ModelDeviceSettingsAccess
import cat.naval.xamanta.models.FactoryResetProtection as ModelFactoryResetProtection
import cat.naval.xamanta.models.KioskCustomization as ModelKioskCustomization
import cat.naval.xamanta.models.PasswordQuality as ModelPasswordQuality
import cat.naval.xamanta.models.PasswordRequirements as ModelPasswordRequirements
import cat.naval.xamanta.models.PersistentPreferredActivity as ModelPersistentPreferredActivity
import cat.naval.xamanta.models.PowerButtonActions as ModelPowerButtonActions
import cat.naval.xamanta.models.RecommendedGlobalProxy as ModelRecommendedGlobalProxy
import cat.naval.xamanta.models.StatusBarMode as ModelStatusBarMode
import cat.naval.xamanta.models.StatusReportingSettings as ModelStatusReportingSettings
import cat.naval.xamanta.models.SystemErrorWarnings as ModelSystemErrorWarnings
import cat.naval.xamanta.models.SystemNavigation as ModelSystemNavigation
import cat.naval.xamanta.models.SystemUpdate as ModelSystemUpdate
import cat.naval.xamanta.models.SystemUpdateType as ModelSystemUpdateType
import cat.naval.xamanta.models.ChoosePrivateKeyRule as ModelChoosePrivateKeyRule
import cat.naval.xamanta.models.UserFacingMessage as ModelUserFacingMessage
import cat.naval.xamanta.protos.AlwaysOnVpnPackage as ProtoAlwaysOnVpnPackage
import cat.naval.xamanta.protos.ChoosePrivateKeyRule as ProtoChoosePrivateKeyRule
import cat.naval.xamanta.protos.DefaultApplication as ProtoDefaultApplication
import cat.naval.xamanta.protos.DefaultApplication.DefaultApplicationScope as ProtoDefaultApplicationScope
import cat.naval.xamanta.protos.FactoryResetProtection as ProtoFactoryResetProtection
import cat.naval.xamanta.protos.KioskCustomization as ProtoKioskCustomization
import cat.naval.xamanta.protos.PasswordRequirements as ProtoPasswordRequirements
import cat.naval.xamanta.protos.PasswordScope as ProtoPasswordScope
import cat.naval.xamanta.protos.PersistentPreferredActivity as ProtoPersistentPreferredActivity
import cat.naval.xamanta.protos.ProxyInfo as ProtoProxyInfo
import cat.naval.xamanta.protos.RequirePasswordUnlock as ProtoRequirePasswordUnlock
import cat.naval.xamanta.protos.StatusReportingSettings as ProtoStatusReportingSettings
import cat.naval.xamanta.protos.SystemUpdate as ProtoSystemUpdate
import cat.naval.xamanta.protos.UserFacingMessage as ProtoUserFacingMessage

fun ProtoKioskCustomization.toKotlinModel(): ModelKioskCustomization = ModelKioskCustomization(
    powerButtonActions =
        powerButtonActions.asModel(ModelPowerButtonActions.POWER_BUTTON_ACTIONS_UNSPECIFIED),
    systemErrorWarnings =
        systemErrorWarnings.asModel(ModelSystemErrorWarnings.SYSTEM_ERROR_WARNINGS_UNSPECIFIED),
    systemNavigation =
        systemNavigation.asModel(ModelSystemNavigation.SYSTEM_NAVIGATION_UNSPECIFIED),
    statusBar = statusBar.asModel(ModelStatusBarMode.STATUS_BAR_UNSPECIFIED),
    deviceSettings =
        deviceSettings.asModel(ModelDeviceSettingsAccess.DEVICE_SETTINGS_UNSPECIFIED),
)

fun ProtoSystemUpdate.toKotlinModel(): ModelSystemUpdate? = when (type) {
    ProtoSystemUpdate.SystemUpdateType.AUTOMATIC ->
        ModelSystemUpdate(ModelSystemUpdateType.AUTOMATIC)

    ProtoSystemUpdate.SystemUpdateType.WINDOWED ->
        ModelSystemUpdate(ModelSystemUpdateType.WINDOWED, startMinutes, endMinutes)

    ProtoSystemUpdate.SystemUpdateType.POSTPONE ->
        ModelSystemUpdate(ModelSystemUpdateType.POSTPONE)

    else -> null
}

fun ProtoUserFacingMessage.toKotlinModel(): ModelUserFacingMessage = ModelUserFacingMessage(
    defaultMessage = defaultMessage,
    localizedMessages = localizedMessagesMap.filterValues { it.isNotEmpty() },
)

internal fun ProtoFactoryResetProtection.toKotlinModel(): ModelFactoryResetProtection =
    ModelFactoryResetProtection(
        accountIds = accountIdsList.toList(),
        disabled = disabled,
    )

internal fun ProtoAlwaysOnVpnPackage.toKotlinModel(): ModelAlwaysOnVpnPackage? =
    packageName.takeIf { it.isNotEmpty() }?.let {
        ModelAlwaysOnVpnPackage(packageName = it, lockdownEnabled = lockdownEnabled)
    }

internal fun ProtoProxyInfo.toKotlinModel(): ModelRecommendedGlobalProxy? {
    val pac = pacUri.takeIf { it.isNotEmpty() }
    if (pac != null) return ModelRecommendedGlobalProxy(pacUri = pac)
    val host = host.takeIf { it.isNotEmpty() } ?: return null
    return ModelRecommendedGlobalProxy(
        host = host,
        port = port,
        excludedHosts = excludedHostsList.toList(),
    )
}

internal fun ProtoPersistentPreferredActivity.toKotlinModel(): ModelPersistentPreferredActivity? {
    if (receiverActivity.isEmpty() || actionsList.isEmpty()) return null
    return ModelPersistentPreferredActivity(
        receiverActivity = receiverActivity,
        actions = actionsList.toList(),
        categories = categoriesList.toList(),
    )
}

internal fun ProtoPasswordRequirements.toKotlinModel(): ModelPasswordRequirements =
    ModelPasswordRequirements(
        quality = passwordQuality.asModel(ModelPasswordQuality.PASSWORD_QUALITY_UNSPECIFIED),
        minimumLength = passwordMinimumLength,
        minimumLetters = passwordMinimumLetters,
        minimumLowerCase = passwordMinimumLowerCase,
        minimumUpperCase = passwordMinimumUpperCase,
        minimumNonLetter = passwordMinimumNonLetter,
        minimumNumeric = passwordMinimumNumeric,
        minimumSymbols = passwordMinimumSymbols,
        historyLength = passwordHistoryLength,
        maximumFailedPasswordsForWipe = maximumFailedPasswordsForWipe,
        expirationTimeoutMs = passwordExpirationTimeoutMs,
        requireEveryDayUnlock =
            requirePasswordUnlock == ProtoRequirePasswordUnlock.REQUIRE_EVERY_DAY,
        profileScoped = passwordScope == ProtoPasswordScope.SCOPE_PROFILE,
    )

internal fun ProtoStatusReportingSettings.toKotlinModel(): ModelStatusReportingSettings =
    ModelStatusReportingSettings(
        applicationReportsEnabled = applicationReportsEnabled,
        deviceSettingsEnabled = deviceSettingsEnabled,
        softwareInfoEnabled = softwareInfoEnabled,
        memoryInfoEnabled = memoryInfoEnabled,
        networkInfoEnabled = networkInfoEnabled,
        displayInfoEnabled = displayInfoEnabled,
        powerManagementEventsEnabled = powerManagementEventsEnabled,
        hardwareStatusEnabled = hardwareStatusEnabled,
        systemPropertiesEnabled = systemPropertiesEnabled,
        commonCriteriaModeEnabled = commonCriteriaModeEnabled,
        defaultApplicationInfoReportingEnabled = defaultApplicationInfoReportingEnabled,
        includeRemovedApps = applicationReportingSettings.includeRemovedApps,
    )

internal fun ProtoChoosePrivateKeyRule.toKotlinModel(): ModelChoosePrivateKeyRule? {
    if (privateKeyAlias.isEmpty() && urlPattern.isEmpty() && packageNamesList.isEmpty()) return null
    return ModelChoosePrivateKeyRule(
        urlPattern = urlPattern.takeIf { it.isNotEmpty() },
        privateKeyAlias = privateKeyAlias.takeIf { it.isNotEmpty() },
        packageNames = packageNamesList.toList(),
    )
}

internal fun ProtoDefaultApplication.toKotlinModel(): ModelDefaultApplication? {
    if (packageName.isEmpty()) return null
    val type = defaultApplicationType.asModelOrNull<ModelDefaultApplicationType>() ?: return null
    val profileScoped = defaultApplicationScopesList.any {
        it == ProtoDefaultApplicationScope.SCOPE_WORK_PROFILE ||
                it == ProtoDefaultApplicationScope.SCOPE_PERSONAL_PROFILE
    }
    return ModelDefaultApplication(
        packageName = packageName,
        type = type,
        profileScoped = profileScoped,
    )
}
