package cat.naval.xamanta.system

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.models.DeviceMetadata
import cat.naval.xamanta.protos.SyncRequest

object DeviceIdentity {

    fun buildSyncRequest(context: Context): SyncRequest {
        val pm = context.packageManager
        val pkg = context.packageName
        val info = runCatching { pm.getPackageInfo(pkg, PackageIdentity.signingFlags()) }.getOrNull()

        return SyncRequest.newBuilder()
            .setDeviceId(deviceId(context))
            .setPackageName(pkg)
            .setSigningSha256(info?.let { PackageIdentity.primarySigner(it) } ?: "")
            .setVersionCode(info?.let { PackageIdentity.versionCodeOf(it) } ?: 0L)
            .setVersionName(info?.versionName ?: "")
            .setApiLevel(Build.VERSION.SDK_INT)
            .setAndroidVersion(Build.VERSION.RELEASE ?: "")
            .setSecurityPatch(securityPatch())
            .setModel(Build.MODEL ?: "")
            .setManufacturer(Build.MANUFACTURER ?: "")
            .setBrand(Build.BRAND ?: "")
            .build()
    }

    fun deviceId(context: Context): String =
        EnrollmentStore(context).clientId()?.takeIf { it.isNotBlank() }
            ?: resolveDeviceId(context)

    @SuppressLint("HardwareIds")
    fun resolveDeviceId(context: Context): String =
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()

    fun collectMetadata(context: Context): DeviceMetadata {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return DeviceMetadata(
            imei = tm?.let { imeiOf(it) },
            meid = tm?.let { meidOf(it) },
            serial = serialNumber(),
            esid = enrollmentSpecificId(context),
            manufacturer = Build.MANUFACTURER?.takeIf { it.isNotBlank() },
            brand = Build.BRAND?.takeIf { it.isNotBlank() },
            model = Build.MODEL?.takeIf { it.isNotBlank() },
        )
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun imeiOf(tm: TelephonyManager): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm.imei
        else @Suppress("DEPRECATION") tm.deviceId
    }.getOrNull()?.takeIf { it.isNotBlank() }

    @SuppressLint("HardwareIds", "MissingPermission")
    @Suppress("DEPRECATION")
    private fun meidOf(tm: TelephonyManager): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm.meid else null
    }.getOrNull()?.takeIf { it.isNotBlank() }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun serialNumber(): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Build.getSerial()
        else @Suppress("DEPRECATION") Build.SERIAL
    }.getOrNull()?.takeIf { it.isNotBlank() && it != Build.UNKNOWN }

    private fun enrollmentSpecificId(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return runCatching { context.dpm.enrollmentSpecificId }.getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    internal fun securityPatch(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH ?: "" else ""
}
