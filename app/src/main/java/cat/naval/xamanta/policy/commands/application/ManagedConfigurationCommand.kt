package cat.naval.xamanta.policy.commands.application

import android.os.Bundle
import cat.naval.xamanta.policy.engine.ApplicationScope
import cat.naval.xamanta.policy.engine.PolicyCommand

class ManagedConfigurationCommand(private val scope: ApplicationScope) : PolicyCommand() {
    override val name = "managedConfiguration"

    override fun execute() {
        val desired = ManagedConfiguration.toBundle(scope.app.managedConfiguration)
        val current = scope.dpm.getApplicationRestrictions(scope.admin, scope.app.packageName)
            ?: Bundle.EMPTY
        if (ManagedConfiguration.contentEquals(current, desired)) return
        scope.dpm.setApplicationRestrictions(scope.admin, scope.app.packageName, desired)
    }
}
