package cat.naval.xamanta.policy.commands.apps

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.PermissionPolicy

@RequiresApi(Build.VERSION_CODES.M)
class DefaultPermissionPolicyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "defaultPermissionPolicy"
    override val minSdk = Build.VERSION_CODES.M

    override fun execute() = scope.dpm.setPermissionPolicy(
        scope.admin,
        when (scope.policy.defaultPermissionPolicy) {
            PermissionPolicy.GRANT -> DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT
            PermissionPolicy.DENY -> DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY
            PermissionPolicy.PROMPT -> DevicePolicyManager.PERMISSION_POLICY_PROMPT
        },
    )
}
