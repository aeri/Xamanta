package cat.naval.xamanta.commands.catalogue

import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand

internal class LockCommand(private val scope: CommandScope) : DeviceCommand() {
    override val name = "lock"
    override suspend fun execute() = scope.dpm.lockNow()
}
