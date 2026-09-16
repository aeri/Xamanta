package cat.naval.xamanta.policy.commands.display

import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.P)
class ScreenTimeoutCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "screenTimeout"
    override val minSdk = Build.VERSION_CODES.P

    override fun execute() {
        scope.policy.screenTimeoutMs?.let {
            scope.dpm.setSystemSetting(scope.admin, Settings.System.SCREEN_OFF_TIMEOUT, it.toString())
        }
    }
}
