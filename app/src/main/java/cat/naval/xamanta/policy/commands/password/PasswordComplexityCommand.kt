package cat.naval.xamanta.policy.commands.password

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.PasswordQuality
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.S)
class PasswordComplexityCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "passwordComplexity"
    override val minSdk = Build.VERSION_CODES.S

    override var details: List<NonComplianceDetail> = emptyList()
        private set

    override fun execute() {
        val r = scope.policy.passwordRequirements
        if (!r.usesComplexityScale()) {
            details = emptyList()
            return
        }
        scope.dpm.setRequiredPasswordComplexity(
            when (r?.quality) {
                PasswordQuality.COMPLEXITY_LOW -> DevicePolicyManager.PASSWORD_COMPLEXITY_LOW
                PasswordQuality.COMPLEXITY_MEDIUM -> DevicePolicyManager.PASSWORD_COMPLEXITY_MEDIUM
                PasswordQuality.COMPLEXITY_HIGH -> DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH
                else -> DevicePolicyManager.PASSWORD_COMPLEXITY_NONE
            },
        )
        details = r?.minimums().orEmpty()
            .filter { (_, value) -> value > 0 }
            .map { (field, value) ->
                NonCompliance.invalidValue(field, "$value has no effect with a COMPLEXITY_* quality")
            }
    }
}
