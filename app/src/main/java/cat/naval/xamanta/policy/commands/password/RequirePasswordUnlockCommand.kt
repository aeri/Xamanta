package cat.naval.xamanta.policy.commands.password

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.O)
class RequirePasswordUnlockCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "requirePasswordUnlock"
    override val minSdk = Build.VERSION_CODES.O

    override fun execute() {
        val everyDay = scope.policy.passwordRequirements?.requireEveryDayUnlock == true
        scope.dpm.setRequiredStrongAuthTimeout(scope.admin, if (everyDay) ONE_DAY_MS else 0L)
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60 * 60 * 1000
    }
}
