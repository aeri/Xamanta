package cat.naval.xamanta.packages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import cat.naval.xamanta.system.PackageIdentity
import cat.naval.xamanta.system.isUserUnlocked
import cat.naval.xamanta.util.downloadTo
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.InstallationFailureReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PkgInstallCoord"

private const val ACTION_INSTALL_COMPLETE = "cat.naval.xamanta.INSTALL_COMPLETE"
private const val ACTION_UNINSTALL_COMPLETE = "cat.naval.xamanta.UNINSTALL_COMPLETE"

class PackageInstallerCoordinator(
    context: Context,
    private val scope: CoroutineScope,
    private val onProgress: () -> Unit,
) {

    private class Download(
        val expectedSha256: String?,
        val allowedSigners: Set<String>,
        val installAboveVersion: Long?,
    )

    private val appContext = context.applicationContext

    private val inFlight = ConcurrentHashMap<String, Download>()
    private val failures = ConcurrentHashMap<String, InstallationFailureReason>()

    private val uninstalling: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val uninstallFailures: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private var registered = false

    private val installResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action != ACTION_INSTALL_COMPLETE && action != ACTION_UNINSTALL_COMPLETE) return
            val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
                ?: intent.getStringExtra(ApkInstaller.EXTRA_TARGET_PACKAGE)
            onInstallResult(action, packageName, ApkInstaller.readResult(appContext, intent))
        }
    }

    fun register() {
        if (registered) return
        ContextCompat.registerReceiver(
            appContext,
            installResultReceiver,
            IntentFilter(ACTION_INSTALL_COMPLETE).apply { addAction(ACTION_UNINSTALL_COMPLETE) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(installResultReceiver) }
        registered = false
    }

    fun canStageDownloads(): Boolean = appContext.isUserUnlocked()

    fun ensureDownload(app: ApplicationPolicy, installAboveVersion: Long? = null): Boolean {
        val packageName = app.packageName
        if (failures.containsKey(packageName)) return false
        if (!canStageDownloads()) {
            Log.i(TAG, "[$packageName] download deferred — storage is locked until the first unlock")
            return false
        }

        val url = app.downloadUrl
        if (url.isNullOrBlank()) {
            latchFailure(packageName, InstallationFailureReason.NOT_FOUND, "no downloadUrl")
            return false
        }
        if (!url.toUri().scheme.equals("https", ignoreCase = true)) {
            latchFailure(
                packageName,
                InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN,
                "refusing non-HTTPS download URL (scheme=${url.toUri().scheme})",
            )
            return false
        }

        val download = Download(
            expectedSha256 = app.sha256Sum?.takeIf { it.isNotBlank() }?.lowercase(),
            allowedSigners = app.signingKeyCerts.map { it.lowercase() }.toSet(),
            installAboveVersion = installAboveVersion,
        )
        if (inFlight.putIfAbsent(packageName, download) != null) return false

        Log.i(TAG, "[$packageName] downloading from $url")
        scope.launch(Dispatchers.IO) { fetchAndInstall(packageName, url, download) }
        return true
    }

    fun isInFlight(packageName: String): Boolean = inFlight.containsKey(packageName)

    fun failureReason(packageName: String): InstallationFailureReason? = failures[packageName]

    fun ensureUninstall(packageName: String, request: (IntentSender) -> Boolean) {
        if (uninstallFailures.contains(packageName)) return
        if (!uninstalling.add(packageName)) return
        if (!request(uninstallSender(packageName))) {
            latchUninstallFailure(packageName, "the system rejected the uninstall request")
        }
    }

    fun isUninstallInFlight(packageName: String): Boolean = uninstalling.contains(packageName)

    fun hasUninstallsInFlight(): Boolean = uninstalling.isNotEmpty()

    fun uninstallFailed(packageName: String): Boolean = uninstallFailures.contains(packageName)

    fun resetFailures() {
        if (failures.isNotEmpty()) {
            Log.i(TAG, "clearing ${failures.size} latched failure(s)")
            failures.clear()
        }
        resetUninstallFailures()
    }

    private fun resetUninstallFailures() {
        if (uninstallFailures.isEmpty()) return
        Log.i(TAG, "clearing ${uninstallFailures.size} latched uninstall failure(s)")
        uninstallFailures.clear()
    }

    fun onPackageInstalled(packageName: String) {
        inFlight.remove(packageName)
    }

    fun onPackageRemoved(packageName: String) {
        uninstalling.remove(packageName)
        uninstallFailures.remove(packageName)
    }

    private fun uninstallSender(packageName: String): IntentSender = ApkInstaller.resultSender(
        appContext,
        ACTION_UNINSTALL_COMPLETE,
        packageName.hashCode(),
        packageName,
    )

    private suspend fun fetchAndInstall(packageName: String, url: String, download: Download) {
        val staged = File(appContext.cacheDir, "install-$packageName.apk")
        try {
            val sha256 = downloadTo(url, staged, ApkInstaller.MAX_APK_BYTES)
            val expected = download.expectedSha256
            if (expected != null && expected != sha256) {
                latchFailure(
                    packageName,
                    InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN,
                    "SHA-256 mismatch: expected=$expected actual=$sha256",
                )
                return
            }
            if (!isSignedByAnAllowedSigner(packageName, staged, download.allowedSigners)) return
            if (!isUpgrade(packageName, staged, download.installAboveVersion)) return

            Log.i(TAG, "[$packageName] verified — committing install")
            FileInputStream(staged).use {
                ApkInstaller.commit(appContext, packageName, it, ACTION_INSTALL_COMPLETE)
            }
        } catch (e: CancellationException) {
            inFlight.remove(packageName)
            throw e
        } catch (e: IOException) {
            latchFailure(
                packageName,
                InstallationFailureReason.NETWORK_ERROR_UNRELIABLE_CONNECTION,
                "download failed: ${e.message}",
            )
        } catch (t: Throwable) {
            latchFailure(
                packageName,
                InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN,
                "install failed — ${t::class.simpleName}: ${t.message}",
            )
        } finally {
            runCatching { staged.delete() }
        }
    }

    private fun isSignedByAnAllowedSigner(
        packageName: String,
        staged: File,
        allowedSigners: Set<String>,
    ): Boolean {
        if (allowedSigners.isEmpty()) return true

        val signers = PackageIdentity.archiveSigners(appContext.packageManager, staged.path)
        if (signers.isEmpty()) {
            latchFailure(
                packageName,
                InstallationFailureReason.NOT_APPROVED,
                "could not read the signing certificate of the downloaded APK",
            )
            return false
        }
        if (signers.none { it in allowedSigners }) {
            latchFailure(
                packageName,
                InstallationFailureReason.NOT_APPROVED,
                "signer not in signingKeyCerts — apk=${signers.joinToString()} " +
                        "policy=${allowedSigners.joinToString()}",
            )
            return false
        }
        return true
    }

    private fun isUpgrade(packageName: String, staged: File, installedVersion: Long?): Boolean {
        if (installedVersion == null) return true
        val stagedVersion = runCatching {
            appContext.packageManager.getPackageArchiveInfo(staged.path, 0)
                ?.let { PackageIdentity.versionCodeOf(it) }
        }.getOrNull()
        val why = when {
            stagedVersion == null -> "could not read the version of the downloaded APK"
            stagedVersion <= installedVersion ->
                "downloaded v$stagedVersion is not newer than installed v$installedVersion"

            else -> return true
        }
        latchFailure(packageName, InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNKNOWN, why)
        return false
    }

    private fun onInstallResult(action: String, packageName: String?, outcome: InstallOutcome) {
        val removal = action == ACTION_UNINSTALL_COMPLETE
        when (outcome) {
            InstallOutcome.Success -> {
                Log.i(TAG, "[$packageName] $action succeeded")
                packageName?.let { if (removal) uninstalling.remove(it) else inFlight.remove(it) }
                onProgress()
            }

            InstallOutcome.AwaitingUser -> when {
                packageName == null -> Unit
                removal ->
                    latchUninstallFailure(packageName, "the removal asked for user confirmation")

                else -> armInstallPromptDeadline(packageName)
            }

            is InstallOutcome.Failed -> when {
                packageName == null -> {
                    Log.e(TAG, "$action failed for an unknown package: ${outcome.message}")
                    onProgress()
                }

                removal -> latchUninstallFailure(packageName, outcome.message)
                else -> latchFailure(packageName, outcome.reason, outcome.message)
            }
        }
    }

    private fun armInstallPromptDeadline(packageName: String) {
        val claim = inFlight[packageName] ?: return
        scope.armPromptDeadline({ inFlight[packageName] === claim }) {
            latchFailure(
                packageName,
                InstallationFailureReason.PERMISSIONS_NOT_ACCEPTED,
                "the confirmation prompt went unanswered",
            )
        }
    }

    private fun latchFailure(
        packageName: String,
        reason: InstallationFailureReason,
        why: String,
    ) {
        Log.e(TAG, "[$packageName] failed ($reason) — $why")
        failures[packageName] = reason
        inFlight.remove(packageName)
        onProgress()
    }

    private fun latchUninstallFailure(packageName: String, why: String) {
        Log.e(TAG, "[$packageName] uninstall failed — $why")
        uninstallFailures.add(packageName)
        uninstalling.remove(packageName)
        onProgress()
    }
}
