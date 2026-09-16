package cat.naval.xamanta.policy.commands.apps

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.InstallType
import cat.naval.xamanta.models.UserControlSetting

@RequiresApi(Build.VERSION_CODES.R)
class UserControlDisabledPackagesCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "userControlDisabledPackages"
    override val minSdk = Build.VERSION_CODES.R

    override fun execute() {
        val protectedPackages = buildList {
            add(scope.admin.packageName)
            scope.policy.applications
                .filter { it.userControlDisabled() }
                .forEach { add(it.packageName) }
        }.distinct()
        scope.dpm.setUserControlDisabledPackages(scope.admin, protectedPackages)
    }
}

private fun ApplicationPolicy.userControlDisabled(): Boolean = when (userControl) {
    UserControlSetting.USER_CONTROL_DISALLOWED -> true
    UserControlSetting.USER_CONTROL_ALLOWED -> false
    null -> installType == InstallType.KIOSK || installType == InstallType.FORCE_INSTALLED
}
