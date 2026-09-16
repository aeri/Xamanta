package cat.naval.xamanta.policy.commands.device

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.N)
class DeviceOwnerLockScreenInfoCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "deviceOwnerLockScreenInfo"
    override val minSdk = Build.VERSION_CODES.N

    override fun execute() = scope.dpm.setDeviceOwnerLockScreenInfo(
        scope.admin,
        scope.policy.deviceOwnerLockScreenInfo?.resolve(),
    )
}
