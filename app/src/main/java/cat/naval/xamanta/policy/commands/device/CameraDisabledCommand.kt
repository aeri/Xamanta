package cat.naval.xamanta.policy.commands.device

import cat.naval.xamanta.models.CameraAccess
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class CameraDisabledCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "cameraAccess"

    override fun execute() = scope.dpm.setCameraDisabled(
        scope.admin,
        scope.policy.cameraAccess == CameraAccess.CAMERA_ACCESS_DISABLED,
    )
}
