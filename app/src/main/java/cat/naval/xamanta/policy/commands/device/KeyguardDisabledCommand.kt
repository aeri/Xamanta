package cat.naval.xamanta.policy.commands.device

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.kiosk.KioskPolicy
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.M)
class KeyguardDisabledCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "keyguardDisabled"
    override val minSdk = Build.VERSION_CODES.M

    override fun execute() {
        val requested = scope.policy.keyguardDisabled
        val disable = requested || KioskPolicy.isEnabled(scope.policy)
        if (!scope.dpm.setKeyguardDisabled(scope.admin, disable) && requested) {
            throw IllegalStateException("the keyguard cannot be disabled while a screen lock is set")
        }
    }
}
