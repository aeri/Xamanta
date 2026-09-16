package cat.naval.xamanta.policy.commands.system

import android.provider.Settings
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class SkipFirstUseHintsCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "skipFirstUseHints"

    override fun execute() = scope.dpm.setSecureSetting(
        scope.admin,
        Settings.Secure.SKIP_FIRST_USE_HINTS,
        if (scope.policy.skipFirstUseHintsEnabled) "1" else "0",
    )
}
