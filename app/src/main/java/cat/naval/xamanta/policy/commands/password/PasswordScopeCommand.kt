package cat.naval.xamanta.policy.commands.password

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.UnsupportedManagementModeException

class PasswordScopeCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "passwordScope"

    override fun execute() {
        if (scope.policy.passwordRequirements?.profileScoped == true) {
            throw UnsupportedManagementModeException("SCOPE_PROFILE requires a work profile")
        }
    }
}
