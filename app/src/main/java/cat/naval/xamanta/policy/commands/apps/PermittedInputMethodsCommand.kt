package cat.naval.xamanta.policy.commands.apps

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class PermittedInputMethodsCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "permittedInputMethods"

    override fun execute() {
        if (!scope.dpm.setPermittedInputMethods(scope.admin, scope.policy.permittedInputMethods)) {
            throw IllegalStateException("rejected: ${scope.policy.permittedInputMethods}")
        }
    }
}
