package cat.naval.xamanta.enrollment

import android.content.Context
import android.util.Base64
import android.util.Log
import cat.naval.xamanta.util.OAuthException
import cat.naval.xamanta.util.json
import cat.naval.xamanta.util.ServerTrust
import cat.naval.xamanta.util.postForJson
import cat.naval.xamanta.models.AuthToken
import cat.naval.xamanta.models.OAuthTokenResponse
import kotlinx.coroutines.CancellationException
import javax.net.ssl.SSLSocketFactory

private const val TAG = "TokenManager"

private const val CLIENT_CREDENTIALS_FORM = "grant_type=client_credentials"

private const val RENEWAL_MARGIN_MS = 5 * 60_000L

sealed class TokenResult {
    data class Available(val token: String) : TokenResult()
    object Transient : TokenResult()
    object NeedsReenrollment : TokenResult()
}

interface TokenSource {
    suspend fun token(): TokenResult

    fun invalidate()
}

class TokenManager(context: Context) : TokenSource {

    private val store = EnrollmentStore(context)

    private fun sslSocketFactory(): SSLSocketFactory? =
        ServerTrust.socketFactory(store.serverCaCertDer())

    override fun invalidate() = store.clearAuthToken()

    override suspend fun token(): TokenResult {
        if (store.needsReenrollment()) {
            Log.w(TAG, "NEEDS_REENROLLMENT is set — skipping HTTP call")
            return TokenResult.NeedsReenrollment
        }

        val cached = store.authToken()
        if (cached != null && !cached.isExpired) {
            return TokenResult.Available(cached.accessToken)
        }

        val tokenEndpoint = store.tokenEndpoint() ?: return TokenResult.Transient
        val clientId = store.clientId() ?: return TokenResult.Transient
        val clientSecret = store.clientSecret() ?: return TokenResult.Transient

        Log.d(TAG, "Access token due for renewal — re-authenticating")
        return try {
            TokenResult.Available(requestToken(tokenEndpoint, clientId, clientSecret).accessToken)
        } catch (e: OAuthException) {
            if (e.statusCode == 400 || e.statusCode == 401) {
                store.markNeedsReenrollment("Credentials rejected: HTTP ${e.statusCode} ${e.message}")
                TokenResult.NeedsReenrollment
            } else {
                Log.e(TAG, "Authentication transient failure [${e.statusCode}]: ${e.message}")
                TokenResult.Transient
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Authentication error (transient): ${e.message}")
            TokenResult.Transient
        }
    }

    private suspend fun requestToken(
        tokenEndpoint: String,
        clientId: String,
        clientSecret: String
    ): AuthToken {
        Log.d(TAG, "client_credentials grant at $tokenEndpoint")
        val credentials = "$clientId:$clientSecret".toByteArray(Charsets.UTF_8)
        val response = json.decodeFromString<OAuthTokenResponse>(
            postForJson(
                endpoint = tokenEndpoint,
                body = CLIENT_CREDENTIALS_FORM,
                contentType = "application/x-www-form-urlencoded",
                authorization = "Basic ${Base64.encodeToString(credentials, Base64.NO_WRAP)}",
                sslSocketFactory = sslSocketFactory(),
            )
        )
        val lifetimeMs = response.expiresIn * 1_000L
        val expiresAt = System.currentTimeMillis() + lifetimeMs
        val token = AuthToken(
            accessToken = response.accessToken,
            expiresAt = expiresAt,
            renewAt = expiresAt - RENEWAL_MARGIN_MS.coerceAtMost(lifetimeMs / 2),
        )
        store.saveAuthToken(token)
        store.clearAuthError()
        return token
    }
}
