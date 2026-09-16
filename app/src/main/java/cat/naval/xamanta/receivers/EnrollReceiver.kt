package cat.naval.xamanta.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import cat.naval.xamanta.activities.ProvisioningSuccessActivity
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.enrollment.EnrollmentWorker
import cat.naval.xamanta.models.ConnectionSettings.Companion.DEFAULT_GRPC_PORT
import cat.naval.xamanta.models.PreferenceConstants

class EnrollReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ENROLL) {
            Log.w(TAG, "ignoring unexpected action '${intent.action}'")
            return
        }

        if (EnrollmentStore(context).isEnrolled()) {
            Log.w(TAG, "Device already enrolled — ignoring ADB enroll command")
            return
        }

        val saved = EnrollmentWorker.begin(
            context,
            tokenEndpoint = intent.getStringExtra(PreferenceConstants.EXTRA_TOKEN_ENDPOINT),
            enrollmentToken = intent.getStringExtra(PreferenceConstants.EXTRA_ENROLLMENT_TOKEN),
            host = intent.getStringExtra(PreferenceConstants.EXTRA_GRPC_HOST),
            port = intent.getIntExtra(PreferenceConstants.EXTRA_GRPC_PORT, DEFAULT_GRPC_PORT),
            serverCaCert = intent.getStringExtra(PreferenceConstants.EXTRA_SERVER_CA_CERT),
            policy = ExistingWorkPolicy.REPLACE,
        )

        if (!saved) {
            Log.e(
                TAG,
                "Invalid enrollment command — need --es ${PreferenceConstants.EXTRA_TOKEN_ENDPOINT}, " +
                    "--es ${PreferenceConstants.EXTRA_GRPC_HOST} and --es ${PreferenceConstants.EXTRA_ENROLLMENT_TOKEN}"
            )
            return
        }

        showProgressScreen(context)
    }

    private fun showProgressScreen(context: Context) {
        val intent = Intent(context, ProvisioningSuccessActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not show enrollment screen: ${it.message}") }
    }

    private companion object {
        const val TAG = "EnrollReceiver"

        const val ACTION_ENROLL = "cat.naval.xamanta.ENROLL"
    }
}
