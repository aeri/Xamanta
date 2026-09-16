package cat.naval.xamanta.policy.commands.apps

import android.content.ComponentName
import android.content.IntentFilter
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.models.DefaultApplicationType
import cat.naval.xamanta.models.PersistentPreferredActivity

class PersistentPreferredActivitiesCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "persistentPreferredActivities"

    override fun execute() {
        val store = PolicyStore(scope.context)
        val own = scope.admin.packageName
        val desired = scope.policy.persistentPreferredActivities.mapNotNull { it.toComponent() }
        val defaultAppPackages = scope.policy.defaultApplications
            .filter {
                it.type == DefaultApplicationType.DEFAULT_HOME ||
                        it.type == DefaultApplicationType.DEFAULT_BROWSER
            }
            .map { it.packageName }
        val desiredPackages = desired.map { it.first.packageName }.toSet() + defaultAppPackages

        (store.preferredActivityPackages() - desiredPackages)
            .filterNot { it == own }
            .forEach { scope.dpm.clearPackagePersistentPreferredActivities(scope.admin, it) }

        desired.forEach { (component, filter) ->
            scope.dpm.addPersistentPreferredActivity(scope.admin, filter, component)
        }
        store.setPreferredActivityPackages(desiredPackages.filterNot { it == own }.toSet())
    }
}

private fun PersistentPreferredActivity.toComponent(): Pair<ComponentName, IntentFilter>? {
    val component = ComponentName.unflattenFromString(receiverActivity) ?: return null
    val filter = IntentFilter().apply {
        actions.forEach { addAction(it) }
        categories.forEach { addCategory(it) }
    }
    return component to filter
}
