package cat.naval.xamanta.policy.commands.application

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.ApplicationScope
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException

@RequiresApi(Build.VERSION_CODES.O)
class DelegatedScopesCommand(private val scope: ApplicationScope) : PolicyCommand() {
    override val name = "delegatedScopes"
    override val minSdk = Build.VERSION_CODES.O

    override fun execute() {
        val packageName = scope.app.packageName
        require(packageName != scope.admin.packageName) {
            "the DPC cannot delegate scopes to itself"
        }

        val desired = scope.app.delegatedScopes.distinct()
        val (supported, tooNew) = desired.partition { DelegationScopes.isSupported(it) }
        scope.dpm.setDelegatedScopes(
            scope.admin,
            packageName,
            supported.mapNotNull { DelegationScopes.PLATFORM[it] },
        )
        if (tooNew.isNotEmpty()) {
            throw UnsupportedApiLevelException(
                "scopes not available on API ${Build.VERSION.SDK_INT}: ${tooNew.joinToString()}"
            )
        }
    }
}
