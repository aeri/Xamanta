package cat.naval.xamanta.policy.commands.system

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class AccountManagementCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "accountTypesWithManagementDisabled"

    override fun execute() {
        val desired = scope.policy.accountTypesWithManagementDisabled.toSet()
        val current = scope.dpm.accountTypesWithManagementDisabled?.toSet().orEmpty()
        (current - desired).forEach { scope.dpm.setAccountManagementDisabled(scope.admin, it, false) }
        desired.forEach { scope.dpm.setAccountManagementDisabled(scope.admin, it, true) }
    }
}
