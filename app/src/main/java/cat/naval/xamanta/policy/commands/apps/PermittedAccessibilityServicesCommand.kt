package cat.naval.xamanta.policy.commands.apps

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class PermittedAccessibilityServicesCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "permittedAccessibilityServices"

    override fun execute() {
        val permitted = scope.policy.permittedAccessibilityServices
        if (!scope.dpm.setPermittedAccessibilityServices(scope.admin, permitted)) {
            throw IllegalStateException("rejected: $permitted")
        }
    }
}
