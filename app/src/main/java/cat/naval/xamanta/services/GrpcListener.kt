package cat.naval.xamanta.services

import cat.naval.xamanta.commands.CommandOutcome
import cat.naval.xamanta.protos.ApplyPolicyRequest
import cat.naval.xamanta.protos.Command
import cat.naval.xamanta.protos.CommandResult

interface GrpcListener {
    fun onConnected()
    fun onPolicyReceived(request: ApplyPolicyRequest)
    fun onDisconnected()

    fun pendingCommandResults(): List<CommandResult>

    fun onCommandResultSent(id: String)

    suspend fun onCommandReceived(command: Command): CommandOutcome
}
