package cat.naval.xamanta.commands.catalogue

import android.os.Build
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.commands.InvalidParamsException
import cat.naval.xamanta.protos.ResetPasswordParams

internal class ResetPasswordCommand(
    private val scope: CommandScope,
    private val params: ResetPasswordParams?,
) : DeviceCommand() {
    override val name = "resetPassword"
    override val maxSdk = Build.VERSION_CODES.N_MR1

    override suspend fun execute() {
        val newPassword = params?.newPassword
        if (newPassword.isNullOrBlank()) {
            throw InvalidParamsException("reset_password_params.new_password is required")
        }
        @Suppress("DEPRECATION")
        if (!scope.dpm.resetPassword(newPassword, 0)) error("resetPassword returned false")
    }
}
