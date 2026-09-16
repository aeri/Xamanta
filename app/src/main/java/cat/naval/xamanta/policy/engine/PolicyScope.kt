package cat.naval.xamanta.policy.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import cat.naval.xamanta.models.DevicePolicy

class PolicyScope(
    val context: Context,
    val policy: DevicePolicy,
    val dpm: DevicePolicyManager,
    val admin: ComponentName,
)
