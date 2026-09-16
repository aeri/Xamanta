package cat.naval.xamanta.policy.kiosk

import android.content.Context
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.DeviceSettingsAccess
import cat.naval.xamanta.models.InstallType

object KioskPolicy {

    fun app(policy: DevicePolicy): ApplicationPolicy? =
        policy.applications.firstOrNull { it.installType == InstallType.KIOSK }

    fun isEnabled(policy: DevicePolicy): Boolean = app(policy) != null

    fun allowedPackages(policy: DevicePolicy): List<String> =
        policy.applications
            .filterNot { it.installType == InstallType.BLOCKED }
            .map { it.packageName }

    fun lockTaskAllowlist(context: Context, policy: DevicePolicy): List<String> {
        val settings = KioskManager.settingsPackage(context)
            .takeIf {
                policy.kioskCustomization?.deviceSettings ==
                        DeviceSettingsAccess.SETTINGS_ACCESS_ALLOWED
            }
        return (allowedPackages(policy) + context.packageName + listOfNotNull(settings)).distinct()
    }
}
