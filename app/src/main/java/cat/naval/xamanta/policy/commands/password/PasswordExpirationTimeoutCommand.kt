package cat.naval.xamanta.policy.commands.password

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class PasswordExpirationTimeoutCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "passwordExpirationTimeout"

    override fun execute() = scope.dpm.setPasswordExpirationTimeout(
        scope.admin,
        scope.policy.passwordRequirements?.expirationTimeoutMs ?: 0L,
    )
}
