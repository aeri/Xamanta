package cat.naval.xamanta.enrollment

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import cat.naval.xamanta.system.getJson
import cat.naval.xamanta.system.getSecret
import cat.naval.xamanta.system.getSecretJson
import cat.naval.xamanta.system.policyPrefs
import cat.naval.xamanta.system.putJson
import cat.naval.xamanta.system.putSecret
import cat.naval.xamanta.system.putSecretJson
import cat.naval.xamanta.util.ServerTrust
import cat.naval.xamanta.util.decodeBase64
import cat.naval.xamanta.models.AuthToken
import cat.naval.xamanta.models.ConnectionSettings
import cat.naval.xamanta.models.ConnectionSettings.Companion.DEFAULT_GRPC_PORT
import cat.naval.xamanta.models.PreferenceConstants.AUTH_TOKEN
import cat.naval.xamanta.models.PreferenceConstants.CLIENT_SECRET
import cat.naval.xamanta.models.PreferenceConstants.CONNECTION_SETTINGS
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_ENROLLMENT_TOKEN
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_GRPC_HOST
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_GRPC_PORT
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_SERVER_CA_CERT
import cat.naval.xamanta.models.PreferenceConstants.EXTRA_TOKEN_ENDPOINT
import cat.naval.xamanta.models.PreferenceConstants.LAST_AUTH_ERROR
import cat.naval.xamanta.models.PreferenceConstants.NEEDS_REENROLLMENT
import cat.naval.xamanta.models.PreferenceConstants.OAUTH_CLIENT_ID
import java.net.URL

private const val TAG = "EnrollmentStore"

class EnrollmentStore(context: Context) {

    private val prefs = context.applicationContext.policyPrefs()

    fun saveProvisioning(
        tokenEndpoint: String?,
        enrollmentToken: String?,
        host: String?,
        port: Int,
        serverCaCert: String? = null,
    ): Boolean {
        if (tokenEndpoint.isNullOrBlank() || enrollmentToken.isNullOrBlank() || host.isNullOrBlank()) {
            Log.e(TAG, "required enrollment extras missing — nothing saved")
            return false
        }
        provisioningProblem(tokenEndpoint, port, serverCaCert)?.let { problem ->
            Log.e(TAG, "$problem — nothing saved")
            return false
        }
        prefs.edit {
            putString(EXTRA_TOKEN_ENDPOINT, tokenEndpoint)
            putSecret(EXTRA_ENROLLMENT_TOKEN, enrollmentToken)
            putString(EXTRA_GRPC_HOST, host)
            putInt(EXTRA_GRPC_PORT, port)
            if (serverCaCert.isNullOrBlank()) remove(EXTRA_SERVER_CA_CERT)
            else putString(EXTRA_SERVER_CA_CERT, serverCaCert)
        }
        Log.d(TAG, "enrollment data saved (host=$host:$port)")
        return true
    }

    fun tokenEndpoint(): String? = prefs.getString(EXTRA_TOKEN_ENDPOINT, null)

    fun hasEnrollmentToken(): Boolean = prefs.contains(EXTRA_ENROLLMENT_TOKEN)

    fun enrollmentToken(): String? = prefs.getSecret(EXTRA_ENROLLMENT_TOKEN)

    fun grpcHost(): String? = prefs.getString(EXTRA_GRPC_HOST, null)

    fun grpcPort(): Int = prefs.getInt(EXTRA_GRPC_PORT, DEFAULT_GRPC_PORT)

    fun serverCaCert(): String? = prefs.getString(EXTRA_SERVER_CA_CERT, null)

    fun serverCaCertDer(): ByteArray? = serverCaCert()?.takeIf { it.isNotBlank() }?.decodeBase64()

    fun isEnrolled(): Boolean = prefs.contains(CONNECTION_SETTINGS)

    fun connectionSettings(): ConnectionSettings? = prefs.getJson(CONNECTION_SETTINGS)

    fun clientId(): String? = prefs.getString(OAUTH_CLIENT_ID, null)

    fun clientSecret(): String? = prefs.getSecret(CLIENT_SECRET)

    fun completeEnrollment(clientId: String, clientSecret: String, settings: ConnectionSettings) {
        prefs.edit {
            putString(OAUTH_CLIENT_ID, clientId)
            putSecret(CLIENT_SECRET, clientSecret)
            putJson(CONNECTION_SETTINGS, settings)
            remove(EXTRA_ENROLLMENT_TOKEN)
        }
    }

    fun authToken(): AuthToken? = prefs.getSecretJson<AuthToken>(AUTH_TOKEN)

    fun saveAuthToken(token: AuthToken) = prefs.edit { putSecretJson(AUTH_TOKEN, token) }

    fun clearAuthToken() = prefs.edit { remove(AUTH_TOKEN) }

    fun needsReenrollment(): Boolean = prefs.getBoolean(NEEDS_REENROLLMENT, false)

    fun lastAuthError(): String? = prefs.getString(LAST_AUTH_ERROR, null)

    fun markNeedsReenrollment(reason: String) {
        Log.e(TAG, "device needs re-enrollment: $reason")
        prefs.edit {
            putBoolean(NEEDS_REENROLLMENT, true)
            putString(LAST_AUTH_ERROR, reason)
        }
    }

    fun clearAuthError() = prefs.edit {
        remove(NEEDS_REENROLLMENT)
        remove(LAST_AUTH_ERROR)
    }

    fun forgetServer() = prefs.edit {
        remove(CONNECTION_SETTINGS)
        remove(AUTH_TOKEN)
        remove(CLIENT_SECRET)
        remove(OAUTH_CLIENT_ID)
        remove(NEEDS_REENROLLMENT)
        remove(LAST_AUTH_ERROR)
    }
}

internal fun provisioningProblem(tokenEndpoint: String, port: Int, serverCaCert: String?): String? =
    when {
        !isHttpsUrl(tokenEndpoint) -> "token endpoint is not an https URL"
        port !in 1..65535 -> "gRPC port $port is out of range"
        !serverCaCert.isNullOrBlank() && !isUsableCaCert(serverCaCert) ->
            "server CA cert is not a valid X.509 certificate"
        else -> null
    }

private fun isHttpsUrl(endpoint: String): Boolean {
    val url = runCatching { URL(endpoint) }.getOrNull() ?: return false
    return url.protocol == "https" && url.host.isNotBlank()
}

private fun isUsableCaCert(base64: String): Boolean {
    val der = base64.decodeBase64() ?: return false
    return runCatching { ServerTrust.trustManager(der) }.isSuccess
}
