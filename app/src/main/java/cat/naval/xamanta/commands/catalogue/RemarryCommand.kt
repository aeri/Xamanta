package cat.naval.xamanta.commands.catalogue

import android.util.Log
import androidx.work.ExistingWorkPolicy
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.commands.InvalidParamsException
import cat.naval.xamanta.enrollment.CaCertInstaller
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.enrollment.EnrollmentWorker
import cat.naval.xamanta.enrollment.provisioningProblem
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.protos.RemarryParams

private const val TAG = "RemarryCommand"

internal class RemarryCommand(
    private val scope: CommandScope,
    private val params: RemarryParams?,
) : DeviceCommand() {
    override val name = "remarry"

    override suspend fun execute() {
        if (params == null ||
            params.tokenEndpoint.isBlank() ||
            params.host.isBlank() ||
            params.enrollmentToken.isBlank()
        ) {
            throw InvalidParamsException(
                "remarry_params.token_endpoint, host and enrollment_token are required"
            )
        }
        provisioningProblem(params.tokenEndpoint, params.port, params.serverCaCert)?.let {
            throw InvalidParamsException("remarry_params: $it")
        }
    }

    override val afterReported: suspend () -> Unit = {
        val settings = checkNotNull(params)
        Log.w(TAG, "remarrying to ${settings.host}:${settings.port}")
        CaCertInstaller.removeIfPresent(scope.context)
        EnrollmentStore(scope.context).forgetServer()
        PolicyStore(scope.context).forgetPolicy()
        EnrollmentWorker.begin(
            scope.context,
            tokenEndpoint = settings.tokenEndpoint,
            enrollmentToken = settings.enrollmentToken,
            host = settings.host,
            port = settings.port,
            serverCaCert = settings.serverCaCert,
            policy = ExistingWorkPolicy.REPLACE,
        )
    }
}
