package cat.naval.xamanta.policy.commands.connectivity

import android.app.admin.DevicePolicyManager
import android.os.Build
import android.os.UserManager
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.PrivateDnsMode

@RequiresApi(Build.VERSION_CODES.Q)
class PrivateDnsCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "privateDnsSettings"
    override val minSdk = Build.VERSION_CODES.Q

    override fun execute() {
        when (scope.policy.privateDnsMode) {
            PrivateDnsMode.PRIVATE_DNS_USER_CHOICE, null ->
                scope.dpm.clearUserRestriction(scope.admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)

            PrivateDnsMode.PRIVATE_DNS_AUTOMATIC ->
                enforce(DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC, host = null) {
                    scope.dpm.setGlobalPrivateDnsModeOpportunistic(scope.admin)
                }

            PrivateDnsMode.PRIVATE_DNS_SPECIFIED_HOST -> {
                val host = scope.policy.privateDnsHost
                    ?: throw IllegalStateException("privateDnsHost is required for SPECIFIED_HOST")
                enforce(DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME, host) {
                    scope.dpm.setGlobalPrivateDnsModeSpecifiedHost(scope.admin, host)
                }
            }
        }
    }

    private fun enforce(mode: Int, host: String?, set: () -> Int) {
        if (!alreadyInForce(mode, host)) {
            val result = set()
            if (result != DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                throw IllegalStateException("the platform refused the private DNS mode: $result")
            }
        }
        scope.dpm.addUserRestriction(scope.admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
    }

    private fun alreadyInForce(mode: Int, host: String?): Boolean =
        scope.dpm.getGlobalPrivateDnsMode(scope.admin) == mode &&
                (host == null || scope.dpm.getGlobalPrivateDnsHost(scope.admin) == host)
}
