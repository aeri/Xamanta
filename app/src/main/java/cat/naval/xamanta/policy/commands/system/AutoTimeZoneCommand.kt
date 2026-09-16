package cat.naval.xamanta.policy.commands.system

import android.os.Build
import android.provider.Settings
import cat.naval.xamanta.models.AutoDateAndTimeZone
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class AutoTimeZoneCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "autoTimeZone"

    override fun execute() {
        if (scope.policy.autoDateAndTimeZone != AutoDateAndTimeZone.AUTO_DATE_AND_TIME_ZONE_ENFORCED) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scope.dpm.setAutoTimeZoneEnabled(scope.admin, true)
        } else {
            scope.dpm.setGlobalSetting(scope.admin, Settings.Global.AUTO_TIME_ZONE, "1")
        }
    }
}
