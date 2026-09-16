package cat.naval.xamanta.policy.engine

import cat.naval.xamanta.models.InstallationFailureReason
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.NonComplianceReason

object NonCompliance {

    const val APPLICATIONS = "applications"
    const val DEVICE_OWNER = "deviceOwner"
    const val SELF_UPDATE = "selfUpdate"
    const val LOCK_TASK_FEATURES = "lockTaskFeatures"

    fun notDeviceOwner() = NonComplianceDetail(
        settingName = DEVICE_OWNER,
        nonComplianceReason = NonComplianceReason.MANAGEMENT_MODE,
    )

    fun apiLevel(settingName: String, currentValue: String) = NonComplianceDetail(
        settingName = settingName,
        nonComplianceReason = NonComplianceReason.API_LEVEL,
        currentValue = currentValue,
    )

    fun invalidValue(settingName: String, currentValue: String) = NonComplianceDetail(
        settingName = settingName,
        nonComplianceReason = NonComplianceReason.INVALID_VALUE,
        currentValue = currentValue,
    )

    fun userAction(settingName: String, currentValue: String) = NonComplianceDetail(
        settingName = settingName,
        nonComplianceReason = NonComplianceReason.USER_ACTION,
        currentValue = currentValue,
    )

    fun unsupported(settingName: String, fieldPath: String, currentValue: String = "") =
        NonComplianceDetail(
            settingName = settingName,
            nonComplianceReason = NonComplianceReason.UNSUPPORTED,
            fieldPath = fieldPath,
            currentValue = currentValue,
        )

    fun appStillInstalled(packageName: String) = NonComplianceDetail(
        settingName = APPLICATIONS,
        nonComplianceReason = NonComplianceReason.APP_INSTALLED,
        packageName = packageName,
    )

    fun appNotInstalled(
        packageName: String,
        reason: InstallationFailureReason =
            InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNSPECIFIED,
        settingName: String = APPLICATIONS,
    ) = NonComplianceDetail(
        settingName = settingName,
        nonComplianceReason = NonComplianceReason.APP_NOT_INSTALLED,
        packageName = packageName,
        installationFailureReason = reason,
    )

    fun appNotUpdated(
        packageName: String,
        reason: InstallationFailureReason,
        settingName: String = APPLICATIONS,
    ) = NonComplianceDetail(
        settingName = settingName,
        nonComplianceReason = NonComplianceReason.APP_NOT_UPDATED,
        packageName = packageName,
        installationFailureReason = reason,
    )

    fun permissionGrantRejected(packageName: String, permission: String) =
        applicationSetting(
            packageName = packageName,
            fieldPath = "permissionGrants.$permission",
            reason = NonComplianceReason.INVALID_VALUE,
        )

    fun applicationSetting(
        packageName: String,
        fieldPath: String,
        reason: NonComplianceReason,
        currentValue: String = "",
    ) = NonComplianceDetail(
        settingName = APPLICATIONS,
        nonComplianceReason = reason,
        packageName = packageName,
        fieldPath = fieldPath,
        currentValue = currentValue,
    )

    fun forFailure(
        command: PolicyCommand,
        failure: Throwable,
        packageName: String? = null,
    ): NonComplianceDetail {
        val currentValue = failure.message.orEmpty()
        val reason = when (failure) {
            is UnsupportedManagementModeException -> NonComplianceReason.MANAGEMENT_MODE
            is UnsupportedApiLevelException -> NonComplianceReason.API_LEVEL
            is UnsupportedDeviceException -> NonComplianceReason.UNSUPPORTED
            is MissingPackageException -> NonComplianceReason.APP_NOT_INSTALLED
            else -> NonComplianceReason.INVALID_VALUE
        }
        if (packageName != null) {
            return applicationSetting(packageName, command.name, reason, currentValue)
        }
        if (failure is MissingPackageException) {
            return appNotInstalled(
                packageName = failure.packageName,
                settingName = command.name,
            )
        }
        return NonComplianceDetail(
            settingName = command.name,
            nonComplianceReason = reason,
            currentValue = currentValue,
        )
    }

    fun kioskAppCrashLooping(packageName: String) = NonComplianceDetail(
        settingName = APPLICATIONS,
        nonComplianceReason = NonComplianceReason.APP_INCOMPATIBLE,
        packageName = packageName,
        fieldPath = "kiosk",
        currentValue = "crash loop",
    )

    fun kioskNotPinned(packageName: String) = NonComplianceDetail(
        settingName = APPLICATIONS,
        nonComplianceReason = NonComplianceReason.APP_INCOMPATIBLE,
        packageName = packageName,
        fieldPath = "kiosk",
        currentValue = "not in lock task",
    )
}
