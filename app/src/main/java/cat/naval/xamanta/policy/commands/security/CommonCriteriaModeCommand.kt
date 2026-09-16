package cat.naval.xamanta.policy.commands.security

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.R)
class CommonCriteriaModeCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "commonCriteriaMode"
    override val minSdk = Build.VERSION_CODES.R

    override fun execute() = scope.dpm.setCommonCriteriaModeEnabled(scope.admin, scope.policy.commonCriteriaModeEnabled)
}
