package cat.naval.xamanta.packages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import cat.naval.xamanta.system.PackageIdentity
import cat.naval.xamanta.system.isUserUnlocked
import cat.naval.xamanta.util.downloadTo
import cat.naval.xamanta.models.InstallationFailureReason
import cat.naval.xamanta.models.SelfUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "SelfUpdater"

private const val ACTION_RESULT = "cat.naval.xamanta.SELF_UPDATE_RESULT"
private const val STAGED_APK = "dpc-update.apk"

class SelfUpdater(
    context: Context,
    private val scope: CoroutineScope,
    private val onChange: () -> Unit,
) {

    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    private val inFlight = AtomicBoolean(false)

    private val attempts = AtomicInteger(0)

    @Volatile
    private var failure: InstallationFailureReason? = null

    @Volatile
    private var deferred = false

    private var registered = false

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != ACTION_RESULT) return
            when (val outcome = ApkInstaller.readResult(appContext, intent)) {
                InstallOutcome.Success -> {
                    Log.i(TAG, "self-update installed")
                    inFlight.set(false)
                    onChange()
                }

                InstallOutcome.AwaitingUser -> {
                    val attempt = attempts.get()
                    scope.armPromptDeadline({ inFlight.get() && attempts.get() == attempt }) {
                        fail(
                            InstallationFailureReason.PERMISSIONS_NOT_ACCEPTED,
                            "the confirmation prompt went unanswered",
                        )
                    }
                }

                is InstallOutcome.Failed -> fail(outcome.reason, outcome.message)
            }
        }
    }

    fun register() {
        if (registered) return
        runCatching { File(appContext.cacheDir, STAGED_APK).delete() }
        ContextCompat.registerReceiver(
            appContext,
            resultReceiver,
            IntentFilter(ACTION_RESULT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(resultReceiver) }
        registered = false
    }

    fun isInFlight(): Boolean = inFlight.get()

    fun isDeferred(): Boolean = deferred

    fun failureReason(): InstallationFailureReason? = failure

    fun resetFailures() {
        failure = null
    }

    fun ensure(target: SelfUpdate) {
        if (failure != null) return

        val url = target.downloadUrl
        if (url.isNullOrBlank() || !url.toUri().scheme.equals("https", ignoreCase = true)) {
            fail(InstallationFailureReason.NOT_FOUND, "missing or non-HTTPS download URL")
            return
        }
        if (!appContext.isUserUnlocked()) {
            Log.i(TAG, "self-update deferred — storage is locked until the first unlock")
            deferred = true
            return
        }
        deferred = false
        if (!inFlight.compareAndSet(false, true)) return
        attempts.incrementAndGet()
        Log.i(TAG, "starting self-update to v${target.versionCode}")
        scope.launch(Dispatchers.IO) {
            runUpdate(url, target.sha256Sum?.takeIf { it.isNotBlank() }?.lowercase(), target.versionCode)
        }
    }

    private suspend fun runUpdate(url: String, expectedSha256: String?, targetVersion: Long) {
        val staged = File(appContext.cacheDir, STAGED_APK)
        try {
            val sha256 = downloadTo(url, staged, ApkInstaller.MAX_APK_BYTES)
            if (expectedSha256 != null && expectedSha256 != sha256) {
                fail(
                    InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN,
                    "SHA-256 mismatch: expected=$expectedSha256 actual=$sha256",
                )
                return
            }
            rejectionOf(staged, targetVersion)?.let { (reason, why) ->
                fail(reason, why)
                return
            }
            Log.i(TAG, "verified — committing self-update")
            FileInputStream(staged).use {
                ApkInstaller.commit(appContext, appContext.packageName, it, ACTION_RESULT)
            }
        } catch (e: CancellationException) {
            inFlight.set(false)
            throw e
        } catch (e: IOException) {
            fail(
                InstallationFailureReason.NETWORK_ERROR_UNRELIABLE_CONNECTION,
                "download failed: ${e.message}",
            )
        } catch (t: Throwable) {
            fail(
                InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN,
                "self-update error — ${t::class.simpleName}: ${t.message}",
            )
        } finally {
            runCatching { staged.delete() }
        }
    }

    private fun rejectionOf(
        apk: File,
        targetVersion: Long,
    ): Pair<InstallationFailureReason, String>? {
        val packageName = appContext.packageName
        val archive = pm.getPackageArchiveInfo(apk.path, 0)
            ?: return unknown("APK could not be parsed")
        if (archive.packageName != packageName) {
            return unknown("package mismatch: apk=${archive.packageName} expected=$packageName")
        }

        val apkVersion = PackageIdentity.versionCodeOf(archive)
        val installedVersion = PackageIdentity.installedVersionCode(pm, packageName)
        if (apkVersion <= installedVersion || targetVersion <= installedVersion) {
            return unknown(
                "not an upgrade: apk=v$apkVersion target=v$targetVersion installed=v$installedVersion"
            )
        }

        val installedSigners = PackageIdentity.installedSigners(pm, packageName)
        val apkSigners = PackageIdentity.archiveSigners(pm, apk.path)
        if (installedSigners.isEmpty() || apkSigners.isEmpty()) {
            return InstallationFailureReason.NOT_APPROVED to "could not read the signing certificate"
        }
        if (installedSigners.intersect(apkSigners).isEmpty()) {
            return InstallationFailureReason.NOT_APPROVED to "signer mismatch (untrusted APK)"
        }

        Log.i(TAG, "signer pinned OK (v$installedVersion → v$apkVersion)")
        return null
    }

    private fun unknown(why: String) =
        InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN to why

    private fun fail(reason: InstallationFailureReason, why: String) {
        Log.e(TAG, "self-update failed ($reason) — $why")
        failure = reason
        deferred = false
        inFlight.set(false)
        onChange()
    }
}
