package cat.naval.xamanta.policy.commands.password

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class MaximumFailedPasswordsForWipeCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "maximumFailedPasswordsForWipe"

    override fun execute() = scope.dpm.setMaximumFailedPasswordsForWipe(
        scope.admin,
        scope.policy.passwordRequirements?.maximumFailedPasswordsForWipe ?: 0,
    )
}
