package cat.naval.xamanta.policy.commands.device

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class KeyguardDisabledFeaturesCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "keyguardDisabledFeatures"

    override fun execute() = scope.dpm.setKeyguardDisabledFeatures(scope.admin, scope.policy.keyguardDisabledFeatures)
}
