package cat.naval.xamanta.policy.commands

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.policy.commands.application.DelegatedScopesCommand
import cat.naval.xamanta.policy.commands.application.ManagedConfigurationCommand
import cat.naval.xamanta.policy.commands.application.PermissionGrantsCommand
import cat.naval.xamanta.policy.engine.ApplicationScope
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.applyAll
import cat.naval.xamanta.policy.reconcile.ApplicationSettingsReconciler

fun buildApplicationCommands(scope: ApplicationScope): List<PolicyCommand> = buildList {
    addAll(APPLICATION_SPECS.unsupportedOn(scope.app))
    add(ManagedConfigurationCommand(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) addAll(marshmallowCommands(scope))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) addAll(oreoCommands(scope))
}

internal val APPLICATION_SPECS = listOf(
    ApiSpec<ApplicationPolicy>("permissionGrants", Build.VERSION_CODES.M) {
        it.permissionGrants.isNotEmpty() || it.defaultPermissionPolicy != null
    },
    ApiSpec("delegatedScopes", Build.VERSION_CODES.O) { it.delegatedScopes.isNotEmpty() },
)

@RequiresApi(Build.VERSION_CODES.M)
private fun marshmallowCommands(scope: ApplicationScope): List<PolicyCommand> = listOf(
    PermissionGrantsCommand(scope),
)

@RequiresApi(Build.VERSION_CODES.O)
private fun oreoCommands(scope: ApplicationScope): List<PolicyCommand> = listOf(
    DelegatedScopesCommand(scope),
)
