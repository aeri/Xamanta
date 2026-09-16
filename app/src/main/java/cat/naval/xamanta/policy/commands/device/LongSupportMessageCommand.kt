package cat.naval.xamanta.policy.commands.device

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.R
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.N)
class LongSupportMessageCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "longSupportMessage"
    override val minSdk = Build.VERSION_CODES.N

    override fun execute() = scope.dpm.setLongSupportMessage(
        scope.admin,
        scope.policy.longSupportMessage?.resolve()
            ?: scope.context.getString(R.string.support_message_long_default),
    )
}
