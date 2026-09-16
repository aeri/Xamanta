package cat.naval.xamanta.policy.commands.connectivity

import android.net.ProxyInfo
import androidx.core.net.toUri
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.models.RecommendedGlobalProxy

class RecommendedGlobalProxyCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "recommendedGlobalProxy"

    override fun execute() =
        scope.dpm.setRecommendedGlobalProxy(scope.admin, scope.policy.recommendedGlobalProxy?.toPlatform())
}

private fun RecommendedGlobalProxy.toPlatform(): ProxyInfo? = when {
    pacUri != null -> ProxyInfo.buildPacProxy(pacUri.toUri())
    host != null -> ProxyInfo.buildDirectProxy(host, port, excludedHosts)
    else -> null
}
