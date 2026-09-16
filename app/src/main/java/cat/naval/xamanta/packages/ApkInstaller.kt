package cat.naval.xamanta.packages

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import cat.naval.xamanta.models.InstallationFailureReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream

private const val TAG = "ApkInstaller"

sealed interface InstallOutcome {
    object Success : InstallOutcome

    object AwaitingUser : InstallOutcome

    data class Failed(val reason: InstallationFailureReason, val message: String) : InstallOutcome
}

object ApkInstaller {

    const val EXTRA_TARGET_PACKAGE = "cat.naval.xamanta.EXTRA_TARGET_PACKAGE"

    const val MAX_APK_BYTES = 256L * 1024 * 1024

    const val PROMPT_TIMEOUT_MS = 5 * 60_000L

    private var promptShownAt = 0L
    private var queuedConfirmation: Intent? = null

    fun commit(context: Context, packageName: String, apk: InputStream, resultAction: String) {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(params)
        Log.i(TAG, "[$packageName] writing install session $sessionId")
        installer.openSession(sessionId).use { session ->
            session.openWrite(packageName, 0, -1).use { target ->
                apk.copyTo(target)
                session.fsync(target)
            }
            session.commit(resultSender(context, resultAction, sessionId, packageName))
        }
    }

    @Suppress("UnspecifiedImmutableFlag")
    fun resultSender(
        context: Context,
        action: String,
        requestCode: Int,
        packageName: String,
    ): IntentSender {
        val intent = Intent(action)
            .setPackage(context.packageName)
            .putExtra(EXTRA_TARGET_PACKAGE, packageName)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags).intentSender
    }

    @MainThread
    fun readResult(context: Context, intent: Intent): InstallOutcome {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        return when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                promptClosed(context)
                InstallOutcome.Success
            }

            PackageInstaller.STATUS_PENDING_USER_ACTION -> requestUserConfirmation(context, intent)
            else -> {
                promptClosed(context)
                InstallOutcome.Failed(failureReasonOf(status), "status=$status $message")
            }
        }
    }

    private fun requestUserConfirmation(context: Context, intent: Intent): InstallOutcome {
        val confirmation = confirmationIntent(intent)
            ?: return InstallOutcome.Failed(
                InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN,
                "user action required but no confirmation intent was provided",
            )
        if (SystemClock.elapsedRealtime() - promptShownAt < PROMPT_TIMEOUT_MS) {
            Log.w(TAG, "a confirmation is already on screen — queueing this one")
            queuedConfirmation = confirmation
        } else {
            queuedConfirmation = null
            show(context, confirmation)
        }
        return InstallOutcome.AwaitingUser
    }

    private fun show(context: Context, confirmation: Intent) {
        Log.w(TAG, "install needs user confirmation — launching system UI")
        promptShownAt = SystemClock.elapsedRealtime()
        context.startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun promptClosed(context: Context) {
        if (promptShownAt == 0L) return
        promptShownAt = 0L
        queuedConfirmation?.let {
            queuedConfirmation = null
            show(context, it)
        }
    }

    private fun confirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

    private fun failureReasonOf(status: Int): InstallationFailureReason = when (status) {
        PackageInstaller.STATUS_FAILURE_STORAGE ->
            InstallationFailureReason.INSUFFICIENT_STORAGE

        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
            InstallationFailureReason.NOT_COMPATIBLE_WITH_DEVICE

        else -> InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN
    }
}

fun CoroutineScope.armPromptDeadline(stillWaiting: () -> Boolean, refuse: () -> Unit) {
    launch {
        delay(ApkInstaller.PROMPT_TIMEOUT_MS)
        if (stillWaiting()) refuse()
    }
}
