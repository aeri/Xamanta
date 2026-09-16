package cat.naval.xamanta.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import cat.naval.xamanta.receivers.XamantaAdminReceiver

val Context.dpm: DevicePolicyManager
    get() = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

val Context.dpcAdmin: ComponentName
    get() = ComponentName(applicationContext, XamantaAdminReceiver::class.java)

fun Context.isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(packageName)
