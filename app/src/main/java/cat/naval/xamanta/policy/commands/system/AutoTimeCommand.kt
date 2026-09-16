package cat.naval.xamanta.policy.commands.system

import android.os.Build
import cat.naval.xamanta.models.AutoDateAndTimeZone
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class AutoTimeCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "autoTime"

    override fun execute() {
        val enforced = scope.policy.autoDateAndTimeZone ==
                AutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_ENFORCED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (enforced) scope.dpm.setAutoTimeEnabled(scope.admin, true)
        } else {
            @Suppress("DEPRECATION")
            scope.dpm.setAutoTimeRequired(scope.admin, enforced)
        }
    }
}
