package cat.naval.xamanta.policy.commands.system

import android.os.Build
import android.provider.Settings
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class StayOnPluggedModesCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "stayOnPluggedModes"
    override val minSdk = Build.VERSION_CODES.M

    override fun execute() {
        val modes = scope.policy.stayOnPluggedModes
        require(modes == 0 || scope.policy.maximumTimeToLockMs <= 0) {
            "ignored by the platform while maximumTimeToLockMs is set"
        }
        scope.dpm.setGlobalSetting(
            scope.admin,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            modes.toString(),
        )
    }
}
