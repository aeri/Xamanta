package cat.naval.xamanta.policy.commands.password

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import cat.naval.xamanta.models.PasswordQuality
import cat.naval.xamanta.models.PasswordRequirements

internal fun PasswordRequirements.minimums(): List<Pair<String, Int>> = listOf(
    "passwordMinimumLength" to minimumLength,
    "passwordMinimumLetters" to minimumLetters,
    "passwordMinimumLowerCase" to minimumLowerCase,
    "passwordMinimumUpperCase" to minimumUpperCase,
    "passwordMinimumNonLetter" to minimumNonLetter,
    "passwordMinimumNumeric" to minimumNumeric,
    "passwordMinimumSymbols" to minimumSymbols,
)

internal fun PasswordRequirements?.usesComplexityScale(): Boolean {
    if (this == null || quality.isComplexityBased) return true
    return quality == PasswordQuality.PASSWORD_QUALITY_UNSPECIFIED && minimums().all { it.second == 0 }
}

@SuppressLint("InlinedApi")
internal fun PasswordRequirements?.legacyQuality(): Int = when (this?.quality) {
    null, PasswordQuality.PASSWORD_QUALITY_UNSPECIFIED ->
        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED

    PasswordQuality.SOMETHING, PasswordQuality.COMPLEXITY_LOW ->
        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING

    PasswordQuality.NUMERIC -> DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
    PasswordQuality.NUMERIC_COMPLEX, PasswordQuality.COMPLEXITY_MEDIUM ->
        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX

    PasswordQuality.ALPHABETIC -> DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
    PasswordQuality.ALPHANUMERIC, PasswordQuality.COMPLEXITY_HIGH ->
        DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC

    PasswordQuality.COMPLEX -> DevicePolicyManager.PASSWORD_QUALITY_COMPLEX
}
