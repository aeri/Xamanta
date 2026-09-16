package cat.naval.xamanta.commands.catalogue

import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.models.StatusReportingSettings
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.protos.DeviceInfo
import cat.naval.xamanta.reporting.DeviceInfoCollector

internal class RequestDeviceInfoCommand(private val scope: CommandScope) : DeviceCommand() {
    override val name = "requestDeviceInfo"

    override var deviceInfo: DeviceInfo? = null
        private set

    override suspend fun execute() {
        val reporting = PolicyStore(scope.context).load()?.devicePolicy?.statusReporting
            ?: StatusReportingSettings()
        deviceInfo = DeviceInfoCollector.collect(scope.context, reporting)
    }
}
