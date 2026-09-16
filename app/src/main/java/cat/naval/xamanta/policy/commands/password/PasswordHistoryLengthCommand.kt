package cat.naval.xamanta.policy.commands.password

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class PasswordHistoryLengthCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "passwordHistoryLength"

    override fun execute() = scope.dpm.setPasswordHistoryLength(
        scope.admin,
        scope.policy.passwordRequirements?.historyLength ?: 0,
    )
}
