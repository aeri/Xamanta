package cat.naval.xamanta.commands.catalogue

import android.util.Log
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.policy.DecommissionCleanup

private const val TAG = "DecommissionCommand"

internal class DecommissionCommand(private val scope: CommandScope) : DeviceCommand() {
    override val name = "decommission"

    override suspend fun execute() {
        DecommissionCleanup.run(scope.context)

        val cleared = runCatching {
            @Suppress("DEPRECATION")
            scope.dpm.clearDeviceOwnerApp(scope.context.packageName)
        }.onFailure { Log.e(TAG, "clearDeviceOwnerApp threw: ${it.message}", it) }.isSuccess

        if (!cleared) error("clearDeviceOwnerApp failed")
        Log.w(TAG, "device owner cleared by remote command")
    }
}
