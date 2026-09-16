package cat.naval.xamanta.commands.catalogue

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.commands.InvalidParamsException
import cat.naval.xamanta.policy.ResetPasswordToken
import cat.naval.xamanta.protos.ResetPasswordParams

@RequiresApi(Build.VERSION_CODES.O)
internal class ResetPasswordWithTokenCommand(
    private val scope: CommandScope,
    private val params: ResetPasswordParams?,
) : DeviceCommand() {
    override val name = "resetPassword"
    override val minSdk = Build.VERSION_CODES.O

    override suspend fun execute() {
        val newPassword = params?.newPassword
        if (newPassword.isNullOrBlank()) {
            throw InvalidParamsException("reset_password_params.new_password is required")
        }
        val token = ResetPasswordToken.stored(scope.context) ?: run {
            ResetPasswordToken.ensure(scope.context)
            error("no reset password token was provisioned yet")
        }
        val active = runCatching { scope.dpm.isResetPasswordTokenActive(scope.admin) }
            .getOrDefault(false)
        if (!active) {
            ResetPasswordToken.ensure(scope.context)
            error("the reset password token is not active until the user confirms their screen lock")
        }
        if (!scope.dpm.resetPasswordWithToken(scope.admin, newPassword, token, 0)) {
            throw InvalidParamsException("the new password does not meet the password requirements")
        }
    }
}
