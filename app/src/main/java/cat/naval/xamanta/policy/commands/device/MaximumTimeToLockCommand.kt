package cat.naval.xamanta.policy.commands.device

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class MaximumTimeToLockCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "maximumTimeToLockMs"

    override fun execute() = scope.dpm.setMaximumTimeToLock(scope.admin, scope.policy.maximumTimeToLockMs)
}
