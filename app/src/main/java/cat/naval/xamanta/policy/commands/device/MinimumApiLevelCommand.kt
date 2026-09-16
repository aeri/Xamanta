package cat.naval.xamanta.policy.commands.device

import android.os.Build
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException

class MinimumApiLevelCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "minimumApiLevel"

    override fun execute() {
        val required = scope.policy.minimumApiLevel
        if (required <= 0 || Build.VERSION.SDK_INT >= required) return
        throw UnsupportedApiLevelException(
            "policy requires API $required, device is on API ${Build.VERSION.SDK_INT}"
        )
    }
}
