package cat.naval.xamanta.enrollment

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cat.naval.xamanta.R
import cat.naval.xamanta.models.ConnectionSettings
import cat.naval.xamanta.models.ConnectionSettings.Companion.DEFAULT_GRPC_PORT
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_ENROLLMENT_TOKEN
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_GRPC_HOST
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_GRPC_PORT
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_SERVER_CA_CERT
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_TOKEN_ENDPOINT
import cat.naval.xamanta.services.PolicyService
import cat.naval.xamanta.system.DeviceIdentity
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.isDeviceOwner
import cat.naval.xamanta.util.OAuthException
import cat.naval.xamanta.util.ServerTrust
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "EnrollmentWorker"

class EnrollmentWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "ENROLLMENT"

        const val KEY_ERROR = "ERROR"

        fun enqueue(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            val request = OneTimeWorkRequestBuilder<EnrollmentWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, policy, request)
        }

        fun begin(
            context: Context,
            tokenEndpoint: String?,
            enrollmentToken: String?,
            host: String?,
            port: Int,
            serverCaCert: String?,
            policy: ExistingWorkPolicy,
        ): Boolean {
            val saved = EnrollmentStore(context).saveProvisioning(
                tokenEndpoint = tokenEndpoint,
                enrollmentToken = enrollmentToken,
                host = host,
                port = port,
                serverCaCert = serverCaCert,
            )
            if (saved) enqueue(context, policy)
            return saved
        }

        fun beginFromProvisioning(context: Context, extras: PersistableBundle?) {
            val saved = extras != null && begin(
                context,
                tokenEndpoint = extras.getString(EXTRA_TOKEN_ENDPOINT),
                enrollmentToken = extras.getString(EXTRA_ENROLLMENT_TOKEN),
                host = extras.getString(EXTRA_GRPC_HOST),
                port = extras.getInt(EXTRA_GRPC_PORT, DEFAULT_GRPC_PORT),
                serverCaCert = extras.getString(EXTRA_SERVER_CA_CERT),
                policy = ExistingWorkPolicy.KEEP,
            )
            if (!saved) {
                Log.e(TAG, "provisioning extras missing or unusable — the worker will report it")
                enqueue(context, ExistingWorkPolicy.KEEP)
            }
        }
    }

    private val store = EnrollmentStore(appContext)

    override suspend fun doWork(): Result {
        val tokenEndpoint = store.tokenEndpoint()
            ?: return fail(R.string.enroll_error_missing_token_endpoint)
        val enrollToken = store.enrollmentToken()
            ?: return if (store.hasEnrollmentToken()) retryWith(R.string.enroll_waiting_secure_storage)
            else fail(R.string.enroll_error_missing_enrollment_token)
        val host = store.grpcHost() ?: return fail(R.string.enroll_error_missing_host)
        val port = store.grpcPort()

        return try {
            if (!appContext.isDeviceOwner()) {
                Log.w(TAG, "Device owner not established yet — deferring enrollment")
                return retryWith(R.string.enroll_waiting_device_owner)
            }

            ensurePhoneStatePermission()

            val androidId = DeviceIdentity.resolveDeviceId(appContext)
            if (androidId.isBlank()) {
                Log.w(TAG, "ANDROID_ID not available yet — deferring enrollment")
                return retryWith(R.string.enroll_waiting_device_identity)
            }
            val metadata = DeviceIdentity.collectMetadata(appContext)

            CaCertInstaller.installIfPresent(appContext)

            val registerEndpoint = RegistrationClient.registerEndpointFrom(tokenEndpoint)
            Log.d(TAG, "Enrolling against $host:$port")

            val registration = RegistrationClient.register(
                registerEndpoint = registerEndpoint,
                enrollmentToken = enrollToken,
                androidId = androidId,
                metadata = metadata,
                clientName = Build.MODEL,
                softwareId = appContext.packageName,
                sslSocketFactory = ServerTrust.socketFactory(store.serverCaCertDer()),
            )

            store.completeEnrollment(
                clientId = registration.clientId,
                clientSecret = registration.clientSecret,
                settings = ConnectionSettings(host, port),
            )

            Log.i(TAG, "Enrollment complete — host=$host:$port")
            PolicyService.start(appContext)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: OAuthException) {
            Log.e(TAG, "Registration failed [${e.statusCode}]", e)
            when (e.statusCode) {
                401 -> fail(R.string.enroll_error_token_rejected)
                400 -> fail(R.string.enroll_error_registration_rejected)
                else -> retryWith(R.string.enroll_error_server, e.statusCode)
            }
        } catch (e: SerializationException) {
            Log.e(TAG, "Malformed registration response", e)
            fail(R.string.enroll_error_malformed_response, e.message.orEmpty())
        } catch (e: IOException) {
            if (ServerTrust.isTrustFailure(e)) {
                Log.e(TAG, "Server certificate rejected by the provisioned CA", e)
                fail(R.string.enroll_error_tls_untrusted, host)
            } else {
                Log.e(TAG, "Network error during enrollment", e)
                retryWith(R.string.enroll_error_network, e.message.orEmpty())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected enrollment error", e)
            fail(R.string.enroll_error_unexpected, e.toString())
        }
    }

    private fun fail(@StringRes message: Int, vararg args: Any): Result =
        Result.failure(workDataOf(KEY_ERROR to appContext.getString(message, *args)))

    private suspend fun retryWith(@StringRes reason: Int, vararg args: Any): Result {
        setProgress(workDataOf(KEY_ERROR to appContext.getString(reason, *args)))
        return Result.retry()
    }

    private fun ensurePhoneStatePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            appContext.dpm.setPermissionGrantState(
                appContext.dpcAdmin, appContext.packageName,
                Manifest.permission.READ_PHONE_STATE,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
        }.onFailure { Log.w(TAG, "Could not pre-grant READ_PHONE_STATE: ${it.message}") }
    }
}
