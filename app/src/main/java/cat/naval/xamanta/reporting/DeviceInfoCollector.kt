package cat.naval.xamanta.reporting

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.HardwarePropertiesManager
import android.os.StatFs
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.StatusReportingSettings
import cat.naval.xamanta.system.DeviceIdentity
import cat.naval.xamanta.system.PackageIdentity
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.util.isoOf
import cat.naval.xamanta.protos.ApplicationReport
import cat.naval.xamanta.protos.DeviceInfo
import cat.naval.xamanta.protos.DeviceSettings
import cat.naval.xamanta.protos.Display
import cat.naval.xamanta.protos.HardwareInfo
import cat.naval.xamanta.protos.MemoryInfo
import cat.naval.xamanta.protos.NetworkInfo
import cat.naval.xamanta.protos.SoftwareInfo
import cat.naval.xamanta.protos.SystemUpdateInfo
import cat.naval.xamanta.protos.TelephonyInfo
import java.util.Locale
import android.view.Display as SysDisplay

object DeviceInfoCollector {

    fun collect(
        context: Context,
        reporting: StatusReportingSettings = StatusReportingSettings(),
    ): DeviceInfo {
        val dpm = context.dpm
        val pkg = context.packageName
        val mode = when {
            dpm.isDeviceOwnerApp(pkg) -> "DEVICE_OWNER"
            dpm.isProfileOwnerApp(pkg) -> "PROFILE_OWNER"
            else -> "MANAGEMENT_MODE_UNSPECIFIED"
        }
        val metadata = section { DeviceIdentity.collectMetadata(context) }

        val report = DeviceInfo.newBuilder()
            .setDeviceId(DeviceIdentity.deviceId(context))
            .setManagementMode(mode)
            .setOwnership(if (mode == "DEVICE_OWNER") "COMPANY_OWNED" else "")
            .setApiLevel(Build.VERSION.SDK_INT)

        if (reporting.softwareInfoEnabled) {
            section { softwareInfo(context, dpm) }?.let { report.setSoftwareInfo(it) }
        }
        if (reporting.hardwareStatusEnabled) {
            section { hardwareInfo(context, metadata?.serial) }?.let { report.setHardwareInfo(it) }
        }
        if (reporting.displayInfoEnabled) {
            section { displays(context) }?.let { report.addAllDisplays(it) }
        }
        if (reporting.applicationReportsEnabled) {
            section { applicationReports(context) }?.let { report.addAllApplicationReports(it) }
        }
        if (reporting.networkInfoEnabled) {
            section { networkInfo(context, metadata?.imei) }?.let { report.setNetworkInfo(it) }
        }
        if (reporting.memoryInfoEnabled) {
            section { memoryInfo(context) }?.let { report.setMemoryInfo(it) }
        }
        if (reporting.deviceSettingsEnabled) {
            section { deviceSettings(context, dpm) }?.let { report.setDeviceSettings(it) }
        }
        if (reporting.systemPropertiesEnabled) {
            section { systemProperties() }?.let { report.putAllSystemProperties(it) }
        }
        return report.build()
    }

    private fun softwareInfo(context: Context, dpm: DevicePolicyManager): SoftwareInfo {
        val b = SoftwareInfo.newBuilder()
            .setAndroidVersion(Build.VERSION.RELEASE ?: "")
            .setAndroidBuildNumber(Build.DISPLAY ?: "")
            .setDeviceKernelVersion(System.getProperty("os.version") ?: "")
            .setBootloaderVersion(Build.BOOTLOADER ?: "")
            .setAndroidBuildTime(isoOf(Build.TIME))
            .setSecurityPatchLevel(DeviceIdentity.securityPatch())
            .setPrimaryLanguageCode(Locale.getDefault().toLanguageTag())
            .setDeviceBuildSignature(buildSignature(context))
        section {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pending = dpm.getPendingSystemUpdate(context.dpcAdmin)
                b.setSystemUpdateInfo(
                    SystemUpdateInfo.newBuilder()
                        .setUpdateStatus(if (pending == null) "UP_TO_DATE" else "UPDATE_AVAILABLE")
                        .build()
                )
            }
        }
        return b.build()
    }

    private fun buildSignature(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo("android", PackageIdentity.signingFlags())
        PackageIdentity.primarySigner(info)
    }.getOrDefault("")

    @SuppressLint("MissingPermission")
    private fun hardwareInfo(context: Context, serial: String?): HardwareInfo {
        val b = HardwareInfo.newBuilder()
            .setBrand(Build.BRAND ?: "")
            .setHardware(Build.HARDWARE ?: "")
            .setDeviceBasebandVersion(runCatching { Build.getRadioVersion() }.getOrNull() ?: "")
            .setManufacturer(Build.MANUFACTURER ?: "")
            .setSerialNumber(serial ?: "")
            .setModel(Build.MODEL ?: "")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) addTemperatures(context, b)
        return b.build()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun addTemperatures(context: Context, b: HardwareInfo.Builder) {
        val hpm = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE)
                as? HardwarePropertiesManager ?: return
        fun temps(type: Int, source: Int): List<Float> =
            runCatching { hpm.getDeviceTemperatures(type, source).filter { !it.isNaN() } }
                .getOrDefault(emptyList())

        val shutdown = HardwarePropertiesManager.TEMPERATURE_SHUTDOWN
        val throttling = HardwarePropertiesManager.TEMPERATURE_THROTTLING
        val battery = HardwarePropertiesManager.DEVICE_TEMPERATURE_BATTERY
        val cpu = HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU
        val gpu = HardwarePropertiesManager.DEVICE_TEMPERATURE_GPU
        val skin = HardwarePropertiesManager.DEVICE_TEMPERATURE_SKIN

        b.addAllBatteryShutdownTemperatures(temps(battery, shutdown))
        b.addAllBatteryThrottlingTemperatures(temps(battery, throttling))
        b.addAllCpuShutdownTemperatures(temps(cpu, shutdown))
        b.addAllCpuThrottlingTemperatures(temps(cpu, throttling))
        b.addAllGpuShutdownTemperatures(temps(gpu, shutdown))
        b.addAllGpuThrottlingTemperatures(temps(gpu, throttling))
        b.addAllSkinShutdownTemperatures(temps(skin, shutdown))
        b.addAllSkinThrottlingTemperatures(temps(skin, throttling))
    }

    private fun displays(context: Context): List<Display> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            ?: return emptyList()
        return dm.displays?.toList().orEmpty().mapNotNull { d ->
            section {
                val metrics =
                    DisplayMetrics().also { @Suppress("DEPRECATION") d.getRealMetrics(it) }
                Display.newBuilder()
                    .setName(d.name ?: "")
                    .setRefreshRate(Math.round(d.refreshRate))
                    .setState(displayState(d.state))
                    .setWidth(metrics.widthPixels)
                    .setHeight(metrics.heightPixels)
                    .setDensity(metrics.densityDpi)
                    .build()
            }
        }
    }

    private fun displayState(state: Int): String = when (state) {
        SysDisplay.STATE_ON -> "ON"
        SysDisplay.STATE_OFF -> "OFF"
        SysDisplay.STATE_DOZE, SysDisplay.STATE_DOZE_SUSPEND -> "DOZE"
        else -> "UNKNOWN"
    }

    private fun applicationReports(context: Context): List<ApplicationReport> {
        val pm = context.packageManager

        @Suppress("DEPRECATION")
        val packages: List<PackageInfo> =
            runCatching { pm.getInstalledPackages(PackageIdentity.signingFlags()) }.getOrDefault(
                emptyList()
            )
        val launcherPkgs = launcherPackages(pm)

        return packages.mapNotNull { pi ->
            section {
                val app = pi.applicationInfo
                val installer = installerOf(pm, pi.packageName)
                ApplicationReport.newBuilder()
                    .setPackageName(pi.packageName)
                    .setVersionName(pi.versionName ?: "")
                    .setVersionCode(PackageIdentity.versionCodeOf(pi))
                    .setDisplayName(app?.let { pm.getApplicationLabel(it).toString() }
                        ?: pi.packageName)
                    .addAllSigningKeyCertFingerprints(PackageIdentity.fingerprintsSha1(pi))
                    .setInstallerPackageName(installer ?: "")
                    .setApplicationSource(applicationSource(app, installer))
                    .setState(if (app?.enabled == false) "DISABLED" else "INSTALLED")
                    .setUserFacingType(if (pi.packageName in launcherPkgs) "USER_FACING" else "NOT_USER_FACING")
                    .build()
            }
        }
    }

    private fun launcherPackages(pm: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return runCatching { pm.queryIntentActivities(intent, 0) }
            .getOrDefault(emptyList())
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private fun installerOf(pm: PackageManager, pkg: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) pm.getInstallSourceInfo(pkg).installingPackageName
        else @Suppress("DEPRECATION") pm.getInstallerPackageName(pkg)
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun applicationSource(app: ApplicationInfo?, installer: String?): String {
        val flags = app?.flags ?: 0
        return when {
            flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 -> "SYSTEM_APP_UPDATED_VERSION"
            flags and ApplicationInfo.FLAG_SYSTEM != 0 -> "SYSTEM_APP_FACTORY_VERSION"
            installer == "com.android.vending" -> "INSTALLED_FROM_PLAY_STORE"
            else -> ""
        }
    }

    @SuppressLint("MissingPermission")
    private fun networkInfo(context: Context, imei: String?): NetworkInfo {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val b = NetworkInfo.newBuilder()
            .setImei(imei ?: "")
            .setNetworkOperatorName(tm?.networkOperatorName ?: "")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            section {
                val sm =
                    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                sm?.activeSubscriptionInfoList?.forEach { sub ->
                    b.addTelephonyInfos(
                        TelephonyInfo.newBuilder()
                            .setCarrierName(sub.carrierName?.toString() ?: "")
                            .setIccId(sub.iccId ?: "")
                            .build()
                    )
                }
            }
        }
        return b.build()
    }

    private fun memoryInfo(context: Context): MemoryInfo {
        val b = MemoryInfo.newBuilder()
        section {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            b.setTotalRam(mi.totalMem)
        }
        section {
            b.setTotalInternalStorage(StatFs(Environment.getDataDirectory().path).totalBytes)
        }
        return b.build()
    }

    @Suppress("DEPRECATION")
    private fun deviceSettings(context: Context, dpm: DevicePolicyManager): DeviceSettings {
        val cr = context.contentResolver
        val status = runCatching { dpm.storageEncryptionStatus }
            .getOrDefault(-1)
        return DeviceSettings.newBuilder()
            .setUnknownSourcesEnabled(
                Settings.Secure.getInt(cr, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
            )
            .setDevelopmentSettingsEnabled(
                Settings.Global.getInt(cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
            )
            .setAdbEnabled(Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1)
            .setVerifyAppsEnabled(Settings.Global.getInt(cr, "package_verifier_enable", 1) == 1)
            .setIsEncrypted(status in ENCRYPTED_STATUSES)
            .setEncryptionStatus(encryptionStatus(status))
            .build()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    private fun encryptionStatus(status: Int): String = when (status) {
        DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> "UNSUPPORTED"
        DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "INACTIVE"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> "ACTIVATING"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> "ACTIVE"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> "ACTIVE_DEFAULT_KEY"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "ACTIVE_PER_USER"
        else -> "ENCRYPTION_STATUS_UNSPECIFIED"
    }

    private fun systemProperties(): Map<String, String> {
        val firstApi = systemProperty("ro.product.first_api_level")
        return if (firstApi.isNullOrBlank()) emptyMap()
        else mapOf("ro.product.first_api_level" to firstApi)
    }

    @SuppressLint("PrivateApi")
    private fun systemProperty(key: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private inline fun <T> section(block: () -> T): T? = runCatching(block).getOrNull()

    @SuppressLint("InlinedApi")
    private val ENCRYPTED_STATUSES = setOf(
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY,
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER,
    )
}
