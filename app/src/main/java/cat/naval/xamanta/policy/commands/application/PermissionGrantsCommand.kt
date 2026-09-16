package cat.naval.xamanta.policy.commands.application

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.ApplicationScope
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.PermissionGrant
import cat.naval.xamanta.models.PermissionPolicy

@RequiresApi(Build.VERSION_CODES.M)
class PermissionGrantsCommand(private val scope: ApplicationScope) : PolicyCommand() {
    override val name = "permissionGrants"
    override val minSdk = Build.VERSION_CODES.M

    override var details: List<NonComplianceDetail> = emptyList()
        private set

    override fun execute() {
        val packageName = scope.app.packageName
        val explicit = scope.app.permissionGrants.associateBy { it.permission }
        val blanket = scope.app.defaultPermissionPolicy ?: PermissionPolicy.PROMPT

        val declared = scope.packages.dangerousPermissions(packageName)
        val desired = declared.map { permission ->
            explicit[permission] ?: PermissionGrant(permission, blanket)
        }
        val extra = explicit.values.filterNot { it.permission in declared }

        details = (desired + extra)
            .filterNot { scope.packages.applyPermissionGrant(packageName, it) }
            .map { NonCompliance.permissionGrantRejected(packageName, it.permission) }
    }
}
