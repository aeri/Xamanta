package cat.naval.xamanta.policy.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.models.ApplicationPolicy

class ApplicationScope(
    val app: ApplicationPolicy,
    val dpm: DevicePolicyManager,
    val admin: ComponentName,
    val packages: ManagedPackages,
)
