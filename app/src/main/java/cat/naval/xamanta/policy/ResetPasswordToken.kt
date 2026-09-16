package cat.naval.xamanta.policy

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import cat.naval.xamanta.models.PreferenceConstants.RESET_PASSWORD_TOKEN
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.getSecret
import cat.naval.xamanta.system.policyPrefs
import cat.naval.xamanta.system.putSecret
import cat.naval.xamanta.util.decodeBase64
import java.security.SecureRandom

private const val TAG = "ResetPasswordToken"

private const val TOKEN_BYTES = 32

@RequiresApi(Build.VERSION_CODES.O)
object ResetPasswordToken {

    fun ensure(context: Context) {
        runCatching { provision(context.applicationContext) }
            .onFailure { Log.e(TAG, "could not provision the reset password token: ${it.message}") }
    }

    fun stored(context: Context): ByteArray? =
        context.applicationContext.policyPrefs().getSecret(RESET_PASSWORD_TOKEN)?.decodeBase64()

    fun clear(context: Context) {
        val app = context.applicationContext
        runCatching { app.dpm.clearResetPasswordToken(app.dpcAdmin) }
            .onFailure { Log.w(TAG, "could not clear the reset password token: ${it.message}") }
        app.policyPrefs().edit(commit = true) { remove(RESET_PASSWORD_TOKEN) }
    }

    private fun provision(app: Context) {
        val prefs = app.policyPrefs()
        val stored = stored(app)
        if (stored == null && prefs.contains(RESET_PASSWORD_TOKEN)) return

        val known = runCatching { app.dpm.isResetPasswordTokenActive(app.dpcAdmin) }.isSuccess
        if (stored != null && known) return

        val token = stored ?: ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        if (stored == null) {
            prefs.edit(commit = true) {
                putSecret(RESET_PASSWORD_TOKEN, Base64.encodeToString(token, Base64.NO_WRAP))
            }
        }
        if (!app.dpm.setResetPasswordToken(app.dpcAdmin, token)) {
            Log.w(TAG, "the platform refused the reset password token")
        }
    }
}
