package cat.naval.xamanta.policy.commands.system

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.R)
class LocationEnabledCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "locationEnabled"
    override val minSdk = Build.VERSION_CODES.R

    override fun execute() {
        scope.policy.locationEnabled?.let { scope.dpm.setLocationEnabled(scope.admin, it) }
    }
}
