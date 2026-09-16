package cat.naval.xamanta.commands

import android.os.Build
import cat.naval.xamanta.commands.catalogue.ApiLevelCommand
import cat.naval.xamanta.commands.catalogue.ClearAppDataCommand
import cat.naval.xamanta.commands.catalogue.DecommissionCommand
import cat.naval.xamanta.commands.catalogue.LockCommand
import cat.naval.xamanta.commands.catalogue.RebootCommand
import cat.naval.xamanta.commands.catalogue.RemarryCommand
import cat.naval.xamanta.commands.catalogue.RequestDeviceInfoCommand
import cat.naval.xamanta.commands.catalogue.ResetPasswordCommand
import cat.naval.xamanta.commands.catalogue.ResetPasswordWithTokenCommand
import cat.naval.xamanta.commands.catalogue.WipeCommand
import cat.naval.xamanta.protos.Command
import cat.naval.xamanta.protos.CommandType

fun buildCommand(scope: CommandScope, command: Command): DeviceCommand? = when (command.type) {
    CommandType.LOCK -> LockCommand(scope)

    CommandType.RESET_PASSWORD -> {
        val params = command.resetPasswordParams.orNullUnless(command.hasResetPasswordParams())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ResetPasswordWithTokenCommand(scope, params)
        else ResetPasswordCommand(scope, params)
    }

    CommandType.REBOOT ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) RebootCommand(scope)
        else ApiLevelCommand("reboot", "REBOOT requires API 24+")

    CommandType.WIPE ->
        WipeCommand(scope, command.wipeParams.orNullUnless(command.hasWipeParams()))

    CommandType.REQUEST_DEVICE_INFO -> RequestDeviceInfoCommand(scope)

    CommandType.CLEAR_APP_DATA ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ClearAppDataCommand(scope, command.clearAppDataParams.orNullUnless(command.hasClearAppDataParams()))
        } else {
            ApiLevelCommand("clearAppData", "CLEAR_APP_DATA requires API 28+")
        }

    CommandType.REMARRY ->
        RemarryCommand(scope, command.remarryParams.orNullUnless(command.hasRemarryParams()))

    CommandType.DECOMMISSION ->
        DecommissionCommand(scope)

    else -> null
}

private fun <T> T.orNullUnless(present: Boolean): T? = if (present) this else null
