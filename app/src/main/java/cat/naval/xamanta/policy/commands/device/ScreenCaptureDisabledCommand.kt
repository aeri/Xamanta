package cat.naval.xamanta.policy.commands.device

import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class ScreenCaptureDisabledCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "screenCaptureDisabled"

    override fun execute() = scope.dpm.setScreenCaptureDisabled(scope.admin, scope.policy.screenCaptureDisabled)
}
