package cat.naval.xamanta.policy.commands

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.os.UserManager
import cat.naval.xamanta.models.AutoDateAndTimeZone
import cat.naval.xamanta.models.CameraAccess
import cat.naval.xamanta.models.ConfigureWifi
import cat.naval.xamanta.models.DeveloperSettings
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.InstallType
import cat.naval.xamanta.models.MicrophoneAccess
import cat.naval.xamanta.models.SystemErrorWarnings
import cat.naval.xamanta.models.TetheringSettings
import cat.naval.xamanta.models.UntrustedAppsPolicy
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.system.dpm

class UserRestrictionCommand(
    override val name: String,
    private val restriction: String,
    private val enabled: Boolean,
    override val minSdk: Int = Build.VERSION_CODES.LOLLIPOP,
    private val dpm: DevicePolicyManager,
    private val admin: ComponentName
) : PolicyCommand() {
    override val asked: Boolean get() = enabled

    override fun execute() {
        if (enabled) dpm.addUserRestriction(admin, restriction)
        else dpm.clearUserRestriction(admin, restriction)
    }
}

internal class RestrictionSpec(
    val name: String,
    val restriction: String,
    val minSdk: Int = Build.VERSION_CODES.LOLLIPOP,
    val desired: (DevicePolicy) -> Boolean,
)

@SuppressLint("InlinedApi")
internal val RESTRICTION_SPECS = listOf(
    RestrictionSpec("systemErrorDialogsDisabled", UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS, Build.VERSION_CODES.P) { p ->
        val c = p.kioskCustomization
        c != null &&
                p.applications.any { it.installType == InstallType.KIOSK } &&
                c.systemErrorWarnings != SystemErrorWarnings.ERROR_AND_WARNINGS_ENABLED
    },
    RestrictionSpec("addWifiConfigDisabled", UserManager.DISALLOW_ADD_WIFI_CONFIG, Build.VERSION_CODES.TIRAMISU) { it.configureWifi.blocksAdding },
    RestrictionSpec("assistContentDisabled", UserManager.DISALLOW_ASSIST_CONTENT, Build.VERSION_CODES.VANILLA_ICE_CREAM) { it.assistContentDisabled },
    RestrictionSpec("bluetoothSharingDisabled", UserManager.DISALLOW_BLUETOOTH_SHARING, Build.VERSION_CODES.O) {
        it.bluetoothContactSharingDisabled || it.bluetoothSharingDisabled
    },
    RestrictionSpec("bluetoothDisabled", UserManager.DISALLOW_BLUETOOTH, Build.VERSION_CODES.O) { it.bluetoothDisabled },
    RestrictionSpec("brightnessConfigDisabled", UserManager.DISALLOW_CONFIG_BRIGHTNESS, Build.VERSION_CODES.P) { it.brightnessConfigDisabled },
    RestrictionSpec("cameraToggleDisabled", UserManager.DISALLOW_CAMERA_TOGGLE, Build.VERSION_CODES.S) { it.cameraAccess == CameraAccess.CAMERA_ACCESS_ENFORCED },
    RestrictionSpec("cellularTwoGDisabled", UserManager.DISALLOW_CELLULAR_2G, Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { it.cellularTwoGDisabled },
    RestrictionSpec("changeWifiStateDisabled", UserManager.DISALLOW_CHANGE_WIFI_STATE, Build.VERSION_CODES.TIRAMISU) { it.changeWifiStateDisabled },
    RestrictionSpec("dataRoamingDisabled", UserManager.DISALLOW_DATA_ROAMING, Build.VERSION_CODES.N) { it.dataRoamingDisabled },
    RestrictionSpec("dateTimeConfigDisabled", UserManager.DISALLOW_CONFIG_DATE_TIME, Build.VERSION_CODES.P) { it.autoDateAndTimeZone == AutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_ENFORCED },
    RestrictionSpec("installUnknownSourcesGloballyDisabled", UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, Build.VERSION_CODES.Q) { it.untrustedAppsPolicy == UntrustedAppsPolicy.DISALLOW_INSTALL },
    RestrictionSpec("locationConfigDisabled", UserManager.DISALLOW_CONFIG_LOCATION, Build.VERSION_CODES.P) { it.locationConfigDisabled },
    RestrictionSpec("microphoneToggleDisabled", UserManager.DISALLOW_MICROPHONE_TOGGLE, Build.VERSION_CODES.S) { it.microphoneAccess == MicrophoneAccess.MICROPHONE_ACCESS_ENFORCED },
    RestrictionSpec("printingDisabled", UserManager.DISALLOW_PRINTING, Build.VERSION_CODES.P) { it.printingDisabled },
    RestrictionSpec("screenTimeoutConfigDisabled", UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT, Build.VERSION_CODES.P) { it.screenTimeoutConfigDisabled },
    RestrictionSpec("setUserIconDisabled", UserManager.DISALLOW_SET_USER_ICON, Build.VERSION_CODES.N) { it.setUserIconDisabled },
    RestrictionSpec("setWallpaperDisabled", UserManager.DISALLOW_SET_WALLPAPER, Build.VERSION_CODES.N) { it.setWallpaperDisabled },
    RestrictionSpec("ultraWidebandDisabled", UserManager.DISALLOW_ULTRA_WIDEBAND_RADIO, Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { it.ultraWidebandDisabled },
    RestrictionSpec("wifiDirectDisabled", UserManager.DISALLOW_WIFI_DIRECT, Build.VERSION_CODES.TIRAMISU) { it.wifiDirectDisabled },
    RestrictionSpec("wifiTetheringDisabled", UserManager.DISALLOW_WIFI_TETHERING, Build.VERSION_CODES.TIRAMISU) { it.tetheringSettings.blocksWifiTethering },
    RestrictionSpec("addUserDisabled", UserManager.DISALLOW_ADD_USER) { it.addUserDisabled },
    RestrictionSpec("adjustVolumeDisabled", UserManager.DISALLOW_ADJUST_VOLUME) { it.adjustVolumeDisabled },
    RestrictionSpec("airplaneModeDisabled", UserManager.DISALLOW_AIRPLANE_MODE, Build.VERSION_CODES.P) { it.airplaneModeDisabled },
    RestrictionSpec("bluetoothConfigDisabled", UserManager.DISALLOW_CONFIG_BLUETOOTH) { it.bluetoothConfigDisabled },
    RestrictionSpec("cellBroadcastsConfigDisabled", UserManager.DISALLOW_CONFIG_CELL_BROADCASTS) { it.cellBroadcastsConfigDisabled },
    RestrictionSpec("createWindowsDisabled", UserManager.DISALLOW_CREATE_WINDOWS) { it.createWindowsDisabled },
    RestrictionSpec("credentialsConfigDisabled", UserManager.DISALLOW_CONFIG_CREDENTIALS) { it.credentialsConfigDisabled },
    RestrictionSpec("debuggingFeaturesDisabled", UserManager.DISALLOW_DEBUGGING_FEATURES) { it.developerSettings == DeveloperSettings.DEVELOPER_SETTINGS_DISABLED },
    RestrictionSpec("factoryResetDisabled", UserManager.DISALLOW_FACTORY_RESET) { it.factoryResetDisabled },
    RestrictionSpec("funDisabled", UserManager.DISALLOW_FUN, Build.VERSION_CODES.M) { it.funDisabled },
    RestrictionSpec("installAppsDisabled", UserManager.DISALLOW_INSTALL_APPS) { it.installAppsDisabled },
    RestrictionSpec("installUnknownSourcesDisabled", UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES) { it.untrustedAppsPolicy == UntrustedAppsPolicy.DISALLOW_INSTALL },
    RestrictionSpec("mobileNetworksConfigDisabled", UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS) { it.mobileNetworksConfigDisabled },
    RestrictionSpec("modifyAccountsDisabled", UserManager.DISALLOW_MODIFY_ACCOUNTS) { it.modifyAccountsDisabled },
    RestrictionSpec("mountPhysicalMediaDisabled", UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA) { it.mountPhysicalMediaDisabled },
    RestrictionSpec("networkResetDisabled", UserManager.DISALLOW_NETWORK_RESET, Build.VERSION_CODES.M) { it.networkResetDisabled },
    RestrictionSpec("outgoingBeamDisabled", UserManager.DISALLOW_OUTGOING_BEAM, Build.VERSION_CODES.LOLLIPOP_MR1) { it.outgoingBeamDisabled },
    RestrictionSpec("outgoingCallsDisabled", UserManager.DISALLOW_OUTGOING_CALLS) { it.outgoingCallsDisabled },
    RestrictionSpec("removeUserDisabled", UserManager.DISALLOW_REMOVE_USER) { it.removeUserDisabled },
    RestrictionSpec("safeBootDisabled", UserManager.DISALLOW_SAFE_BOOT, Build.VERSION_CODES.M) { it.developerSettings == DeveloperSettings.DEVELOPER_SETTINGS_DISABLED },
    RestrictionSpec("shareLocationDisabled", UserManager.DISALLOW_SHARE_LOCATION) { it.shareLocationDisabled },
    RestrictionSpec("smsDisabled", UserManager.DISALLOW_SMS) { it.smsDisabled },
    RestrictionSpec("tetheringConfigDisabled", UserManager.DISALLOW_CONFIG_TETHERING) { it.tetheringSettings == TetheringSettings.DISALLOW_ALL_TETHERING },
    RestrictionSpec("uninstallAppsDisabled", UserManager.DISALLOW_UNINSTALL_APPS) { it.uninstallAppsDisabled },
    RestrictionSpec("unmuteMicrophoneDisabled", UserManager.DISALLOW_UNMUTE_MICROPHONE) { it.microphoneAccess == MicrophoneAccess.MICROPHONE_ACCESS_DISABLED },
    RestrictionSpec("usbFileTransferDisabled", UserManager.DISALLOW_USB_FILE_TRANSFER) { it.usbDataAccess.blocksFileTransfer },
    RestrictionSpec("verifyApps", UserManager.ENSURE_VERIFY_APPS) { it.verifyAppsEnforced != false },
    RestrictionSpec("vpnConfigDisabled", UserManager.DISALLOW_CONFIG_VPN) { it.vpnConfigDisabled },
    RestrictionSpec("wifiConfigDisabled", UserManager.DISALLOW_CONFIG_WIFI) { it.configureWifi == ConfigureWifi.DISALLOW_CONFIGURING_WIFI },
)

fun buildUserRestrictionCommands(scope: PolicyScope): List<PolicyCommand> =
    RESTRICTION_SPECS.map { spec ->
        UserRestrictionCommand(
            spec.name,
            spec.restriction,
            spec.desired(scope.policy),
            spec.minSdk,
            scope.dpm,
            scope.admin,
        )
    }

fun managedUserRestrictions(): List<String> = RESTRICTION_SPECS.map { it.restriction }
