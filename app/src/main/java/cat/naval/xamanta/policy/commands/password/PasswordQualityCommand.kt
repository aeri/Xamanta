package cat.naval.xamanta.policy.commands.password

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.os.Build
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@SuppressLint("InlinedApi")
@Suppress("DEPRECATION")
class PasswordQualityCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "passwordQuality"

    override var details: List<NonComplianceDetail> = emptyList()
        private set

    override fun execute() {
        val r = scope.policy.passwordRequirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && r.usesComplexityScale()) {
            details = emptyList()
            return
        }

        val quality = r.legacyQuality()
        scope.dpm.setPasswordQuality(scope.admin, quality)

        val dpm = scope.dpm
        val admin = scope.admin
        val numeric = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
        val complex = DevicePolicyManager.PASSWORD_QUALITY_COMPLEX
        val ignored = mutableListOf<NonComplianceDetail>()

        fun minimum(field: String, value: Int, required: Int, set: (Int) -> Unit) {
            when {
                quality >= required -> set(value)
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> set(0)
            }
            if (value > 0 && quality < required) {
                ignored += NonCompliance.invalidValue(field, "$value has no effect with this passwordQuality")
            }
        }

        minimum("passwordMinimumLength", r?.minimumLength ?: 0, numeric) {
            dpm.setPasswordMinimumLength(admin, it)
        }
        minimum("passwordMinimumLetters", r?.minimumLetters ?: 0, complex) {
            dpm.setPasswordMinimumLetters(admin, it)
        }
        minimum("passwordMinimumLowerCase", r?.minimumLowerCase ?: 0, complex) {
            dpm.setPasswordMinimumLowerCase(admin, it)
        }
        minimum("passwordMinimumUpperCase", r?.minimumUpperCase ?: 0, complex) {
            dpm.setPasswordMinimumUpperCase(admin, it)
        }
        minimum("passwordMinimumNonLetter", r?.minimumNonLetter ?: 0, complex) {
            dpm.setPasswordMinimumNonLetter(admin, it)
        }
        minimum("passwordMinimumNumeric", r?.minimumNumeric ?: 0, complex) {
            dpm.setPasswordMinimumNumeric(admin, it)
        }
        minimum("passwordMinimumSymbols", r?.minimumSymbols ?: 0, complex) {
            dpm.setPasswordMinimumSymbols(admin, it)
        }
        details = ignored
    }
}
