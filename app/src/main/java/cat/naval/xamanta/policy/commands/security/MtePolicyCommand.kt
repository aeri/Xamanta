package cat.naval.xamanta.policy.commands.security

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.MtePolicyMode

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class MtePolicyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "mtePolicy"
    override val minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    override fun execute() {
        val mode = when (scope.policy.mtePolicy) {
            MtePolicyMode.MTE_ENABLED -> DevicePolicyManager.MTE_ENABLED
            MtePolicyMode.MTE_DISABLED -> DevicePolicyManager.MTE_DISABLED
            MtePolicyMode.MTE_USER_CHOICE, null -> DevicePolicyManager.MTE_NOT_CONTROLLED_BY_POLICY
        }
        try {
            scope.dpm.setMtePolicy(mode)
        } catch (failure: UnsupportedOperationException) {
            deviceCannot(
                reason = failure.message.orEmpty(),
                asked = mode != DevicePolicyManager.MTE_NOT_CONTROLLED_BY_POLICY,
            )
        }
    }
}
