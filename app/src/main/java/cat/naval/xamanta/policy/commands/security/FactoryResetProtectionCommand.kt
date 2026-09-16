package cat.naval.xamanta.policy.commands.security

import android.app.admin.FactoryResetProtectionPolicy
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.R)
class FactoryResetProtectionCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "factoryResetProtection"
    override val minSdk = Build.VERSION_CODES.R

    override fun execute() {
        val frp = scope.policy.factoryResetProtection
        if (frp == null) {
            try {
                scope.dpm.setFactoryResetProtectionPolicy(scope.admin, null)
            } catch (_: UnsupportedOperationException) {
            }
            return
        }
        scope.dpm.setFactoryResetProtectionPolicy(
            scope.admin,
            FactoryResetProtectionPolicy.Builder()
                .setFactoryResetProtectionAccounts(frp.accountIds)
                .setFactoryResetProtectionEnabled(!frp.disabled)
                .build(),
        )
    }
}
