package cat.naval.xamanta.policy.commands.device

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.StatusBarMode
import cat.naval.xamanta.policy.kiosk.KioskPolicy
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.M)
class StatusBarDisabledCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "kioskCustomization.statusBar"
    override val minSdk = Build.VERSION_CODES.M

    override fun execute() {
        val requested = scope.policy.asksStatusBarDisabled()
        val disable = requested || KioskPolicy.isEnabled(scope.policy)
        if (!scope.dpm.setStatusBarDisabled(scope.admin, disable) && requested) {
            throw IllegalStateException("the platform refused to disable the status bar")
        }
    }
}

internal fun DevicePolicy.asksStatusBarDisabled(): Boolean =
    kioskCustomization?.let { it.statusBar != StatusBarMode.NOTIFICATIONS_AND_SYSTEM_INFO_ENABLED } ?: false
