package cat.naval.xamanta.policy.commands.security

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class AppFunctionsCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "appFunctions"
    override val minSdk = Build.VERSION_CODES.BAKLAVA

    override fun execute() = scope.dpm.setAppFunctionsPolicy(
        when (scope.policy.appFunctionsAllowed) {
            false -> DevicePolicyManager.APP_FUNCTIONS_DISABLED
            true, null -> DevicePolicyManager.APP_FUNCTIONS_NOT_CONTROLLED_BY_POLICY
        },
    )
}
