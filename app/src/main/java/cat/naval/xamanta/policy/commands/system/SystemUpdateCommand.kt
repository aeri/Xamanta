package cat.naval.xamanta.policy.commands.system

import android.app.admin.SystemUpdatePolicy
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.SystemUpdate
import cat.naval.xamanta.models.SystemUpdateType

@RequiresApi(Build.VERSION_CODES.M)
class SystemUpdateCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "systemUpdate"
    override val minSdk = Build.VERSION_CODES.M

    override fun execute() =
        scope.dpm.setSystemUpdatePolicy(scope.admin, scope.policy.systemUpdate.toDpmPolicy())
}

@RequiresApi(Build.VERSION_CODES.M)
private fun SystemUpdate?.toDpmPolicy(): SystemUpdatePolicy? = when (this?.type) {
    SystemUpdateType.AUTOMATIC -> SystemUpdatePolicy.createAutomaticInstallPolicy()
    SystemUpdateType.WINDOWED ->
        SystemUpdatePolicy.createWindowedInstallPolicy(startMinutes, endMinutes)

    SystemUpdateType.POSTPONE -> SystemUpdatePolicy.createPostponeInstallPolicy()
    null -> null
}
