package cat.naval.xamanta.policy.commands.security

import android.app.admin.PackagePolicy
import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.CredentialProviderPolicyMode

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialProviderPolicyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "credentialProviderPolicy"
    override val minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    override fun execute() = scope.dpm.setCredentialManagerPolicy(
        when (scope.policy.credentialProviderPolicy) {
            CredentialProviderPolicyMode.CREDENTIAL_PROVIDER_DISALLOWED ->
                PackagePolicy(PackagePolicy.PACKAGE_POLICY_ALLOWLIST, allowedPackages())

            CredentialProviderPolicyMode.CREDENTIAL_PROVIDER_DISALLOWED_EXCEPT_SYSTEM ->
                PackagePolicy(PackagePolicy.PACKAGE_POLICY_ALLOWLIST_AND_SYSTEM, allowedPackages())

            CredentialProviderPolicyMode.CREDENTIAL_PROVIDER_ALLOWED, null -> null
        },
    )

    private fun allowedPackages(): Set<String> = scope.policy.applications
        .filter { it.credentialProviderAllowed }
        .map { it.packageName }
        .toSet()
}
