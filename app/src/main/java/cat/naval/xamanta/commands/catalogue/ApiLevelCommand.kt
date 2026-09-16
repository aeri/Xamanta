package cat.naval.xamanta.commands.catalogue

import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException

internal class ApiLevelCommand(
    override val name: String,
    private val reason: String,
) : DeviceCommand() {
    override suspend fun execute(): Unit = throw UnsupportedApiLevelException(reason)
}
