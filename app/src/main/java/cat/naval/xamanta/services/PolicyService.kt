package cat.naval.xamanta.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cat.naval.xamanta.commands.CommandEngine
import cat.naval.xamanta.commands.CommandOutcome
import cat.naval.xamanta.enrollment.CaCertInstaller
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.enrollment.TokenManager
import cat.naval.xamanta.models.Compliance
import cat.naval.xamanta.models.ConnectionSettings
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.Policy
import cat.naval.xamanta.models.PreferenceConstants.CONNECTION_SETTINGS
import cat.naval.xamanta.models.PreferenceConstants.NEEDS_REENROLLMENT
import cat.naval.xamanta.packages.PackageInstallerCoordinator
import cat.naval.xamanta.packages.SelfUpdater
import cat.naval.xamanta.policy.PolicyEnforcement
import cat.naval.xamanta.policy.PolicyEnforcement.Verdict
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.policy.PolicyVersionGate
import cat.naval.xamanta.policy.kiosk.KioskPolicy
import cat.naval.xamanta.proto.complianceReport
import cat.naval.xamanta.proto.toKotlinModel
import cat.naval.xamanta.protos.ApplyPolicyRequest
import cat.naval.xamanta.protos.ApplyPolicyResponse
import cat.naval.xamanta.protos.Command
import cat.naval.xamanta.receivers.PackageReceiver
import cat.naval.xamanta.system.DeviceIdentity
import cat.naval.xamanta.system.ManagementNotification
import cat.naval.xamanta.system.NetworkMonitor
import cat.naval.xamanta.system.policyPrefs
import cat.naval.xamanta.util.RetryTimer
import cat.naval.xamanta.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "PolicyService"

private const val RETRY_BASE_MS = 30_000L
private const val RETRY_CAP_MS = 30 * 60_000L

private const val RECONNECT_QUIET_MS = 60_000L

class PolicyService : LifecycleService(), GrpcListener {

    companion object {
        private const val EXTRA_RECONCILE_REASON = "cat.naval.xamanta.RECONCILE_REASON"

        fun requestReconcile(context: Context, reason: String) = launch(
            context,
            Intent(context, PolicyService::class.java).putExtra(EXTRA_RECONCILE_REASON, reason),
        )

        fun start(context: Context) = launch(context, Intent(context, PolicyService::class.java))

        private fun launch(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var preferences: SharedPreferences
    private lateinit var enrollment: EnrollmentStore
    private lateinit var policyStore: PolicyStore
    private lateinit var packageInstaller: PackageInstallerCoordinator
    private lateinit var selfUpdater: SelfUpdater
    private lateinit var packageReceiver: PackageReceiver
    private lateinit var commandEngine: CommandEngine
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var reconcileRetry: RetryTimer
    private lateinit var workingRetry: RetryTimer

    @Volatile
    private var grpcClient: GrpcClient? = null
    private var connectionSettings: ConnectionSettings? = null

    private val policyGate = PolicyVersionGate()
    private val incomingPolicy = AtomicReference<Policy?>(null)

    private val reconcileSerial = AtomicInteger(0)

    private val lastConnectedAt = AtomicLong(0L)

    private val retryLatchedFailures = AtomicBoolean(false)

    private val reconcileWakeup = Channel<Unit>(Channel.CONFLATED)

    private val complianceReports = Channel<ApplyPolicyResponse>(Channel.BUFFERED)

    private val packageIntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addDataScheme("package")
    }

    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            CONNECTION_SETTINGS -> requestConnect()
            NEEDS_REENROLLMENT -> grpcClient?.reconnect()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ManagementNotification.startForeground(this)

        preferences = applicationContext.policyPrefs()
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        enrollment = EnrollmentStore(applicationContext)
        policyStore = PolicyStore(applicationContext)

        packageInstaller = PackageInstallerCoordinator(applicationContext, lifecycleScope) {
            requestReconcile("installer progress")
        }.apply { register() }

        selfUpdater = SelfUpdater(applicationContext, lifecycleScope) {
            requestReconcile("self-update progress")
        }.apply { register() }

        commandEngine = CommandEngine(applicationContext)

        packageReceiver = PackageReceiver { action, packageName ->
            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> packageInstaller.onPackageInstalled(packageName)
                Intent.ACTION_PACKAGE_REMOVED -> packageInstaller.onPackageRemoved(packageName)
            }
            requestReconcile("package $action $packageName")
        }
        ContextCompat.registerReceiver(
            applicationContext,
            packageReceiver,
            packageIntentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        networkMonitor = NetworkMonitor(applicationContext) { grpcClient?.reconnect() }
        networkMonitor.register()

        reconcileRetry = RetryTimer("reconcile", lifecycleScope, RETRY_BASE_MS, RETRY_CAP_MS) {
            retryLatchedFailures.set(true)
            requestReconcile("defiance retry")
        }

        workingRetry = RetryTimer("working", lifecycleScope, RETRY_BASE_MS, RETRY_CAP_MS) {
            requestReconcile("working watchdog")
        }

        startReconcileLoop()
        startComplianceLoop()

        warmUp()
    }

    private fun warmUp() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatchingCancellable {
                connectToServer()
                BackstopWorker.enqueuePeriodic(applicationContext)
                ManagementNotification.grantOwnPermission(applicationContext)
                CaCertInstaller.installIfPresent(applicationContext)
                requestReconcile("service start")
            }.onFailure { Log.e(TAG, "warm-up failed: ${it.message}", it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ManagementNotification.startForeground(this)
        requestConnect()
        intent?.getStringExtra(EXTRA_RECONCILE_REASON)?.let { requestReconcile(it) }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
        networkMonitor.unregister()
        runCatching { applicationContext.unregisterReceiver(packageReceiver) }
        packageInstaller.unregister()
        selfUpdater.unregister()
        reconcileRetry.cancel()
        workingRetry.cancel()
        stopClient()
        reconcileWakeup.close()
        complianceReports.close()
        Log.w(TAG, "PolicyService stopped")
    }

    override fun onConnected() {
        val now = SystemClock.elapsedRealtime()
        val previous = lastConnectedAt.getAndSet(now)
        val sinceLast = now - previous
        if (previous != 0L && sinceLast < RECONNECT_QUIET_MS) {
            Log.i(TAG, "reconnected after ${sinceLast}ms — no reconcile, nothing was away")
            return
        }
        requestReconcile("server connected")
    }

    override fun onDisconnected() {
        Log.w(TAG, "server connection lost — retrying with backoff")
    }

    override fun pendingCommandResults() = commandEngine.pendingResults()

    override fun onCommandResultSent(id: String) = commandEngine.markResultSent(id)

    override suspend fun onCommandReceived(command: Command): CommandOutcome =
        commandEngine.execute(command)

    override fun onPolicyReceived(request: ApplyPolicyRequest) {
        runCatching {
            val policy = request.policy.toKotlinModel()
            if (!policyGate.admit(policy)) {
                Log.i(TAG, "policy '${policy.name}' v${policy.version} ignored — not newer")
                return@runCatching
            }
            Log.i(TAG, "policy '${policy.name}' v${policy.version} accepted")
            incomingPolicy.set(policy)
            requestReconcile("new policy")
        }.onFailure { Log.e(TAG, "unusable policy from server: ${it.message}", it) }
    }

    private fun requestReconcile(reason: String) {
        Log.d(TAG, "reconcile requested: $reason")
        reconcileWakeup.trySend(Unit)
    }

    private fun startReconcileLoop() {
        lifecycleScope.launch {
            for (wakeup in reconcileWakeup) {
                runCatchingCancellable { reconcile() }
                    .onFailure { Log.e(TAG, "reconcile failed: ${it.message}", it) }
            }
        }
    }

    private suspend fun reconcile() {
        withContext(Dispatchers.IO + NonCancellable) { reconcilePass() }
    }

    private fun reconcilePass() {
        if (retryLatchedFailures.getAndSet(false)) forgiveLatchedFailures()

        val stored = policyStore.load()
        val incoming = incomingPolicy.getAndSet(null)
        val forceKioskTransition = incoming?.let { policy ->
            val kioskChanged = kioskTarget(stored) != kioskTarget(policy)

            if (!policyStore.save(policy)) {
                Log.e(TAG, "could not persist '${policy.name}' v${policy.version} — not acking")
                incomingPolicy.compareAndSet(null, policy)
                reconcileRetry.schedule()
                return
            }
            sendCompliance(policy, Compliance.PENDING_COMPLIANCE, emptyList())

            policyStore.clearKioskCrashState()
            forgiveLatchedFailures()
            reconcileRetry.reset()
            workingRetry.reset()
            kioskChanged
        } ?: false

        val policy = incoming ?: stored ?: run {
            Log.d(TAG, "nothing to reconcile — no policy stored yet")
            return
        }

        val verdict = runCatching {
            PolicyEnforcement(applicationContext, packageInstaller, selfUpdater)
                .reconcile(policy, forceKioskTransition, policyStore.isKioskCrashLooping(policy))
        }.getOrElse {
            Log.e(TAG, "reconcile threw: ${it.message}", it)
            Verdict.Defiance(emptyList())
        }

        report(policy, verdict)
    }

    private fun forgiveLatchedFailures() {
        packageInstaller.resetFailures()
        selfUpdater.resetFailures()
    }

    private fun report(policy: Policy, verdict: Verdict) = when (verdict) {
        Verdict.InForce -> {
            reconcileRetry.reset()
            workingRetry.reset()
            sendCompliance(policy, Compliance.IN_FORCE, emptyList())
        }

        is Verdict.Defiance -> {
            workingRetry.reset()
            sendCompliance(policy, Compliance.DEFIANCE, verdict.details)
            reconcileRetry.schedule()
        }

        Verdict.Working -> {
            reconcileRetry.cancel()
            workingRetry.schedule()
        }
    }

    private fun sendCompliance(
        policy: Policy,
        compliance: Compliance,
        details: List<NonComplianceDetail>,
    ) {
        val executionId = "v${policy.version}.${reconcileSerial.incrementAndGet()}"
        Log.i(TAG, "reporting $compliance [$executionId] details=${details.size}")
        val report = complianceReport(compliance, executionId, details)
        if (complianceReports.trySend(report).isFailure) {
            Log.w(TAG, "compliance queue full or closed — dropped [$executionId]")
        }
    }

    private fun startComplianceLoop() {
        lifecycleScope.launch {
            for (report in complianceReports) {
                runCatchingCancellable { grpcClient?.reportCompliance(report) }
                    .onFailure { Log.e(TAG, "compliance report failed: ${it.message}", it) }
            }
        }
    }

    private fun requestConnect() {
        lifecycleScope.launch(Dispatchers.IO) { connectToServer() }
    }

    @Synchronized
    private fun connectToServer() {
        val settings = storedConnectionSettings() ?: return
        if (settings == connectionSettings) return

        policyGate.seed(policyStore.load())
        incomingPolicy.set(null)

        val client = grpcClient
        if (client == null) {
            Log.i(TAG, "connecting to ${settings.host}:${settings.port}")
            grpcClient = GrpcClient(
                settings = settings,
                syncRequest = DeviceIdentity.buildSyncRequest(applicationContext),
                listener = this,
                tokens = TokenManager(applicationContext),
                caProvider = enrollment::serverCaCertDer,
            ).apply { start() }
        } else {
            Log.i(TAG, "server moved to ${settings.host}:${settings.port} — reconnecting")
            client.reconfigure(settings)
        }
        connectionSettings = settings
    }

    @Synchronized
    private fun stopClient() {
        grpcClient?.stop()
        grpcClient = null
        connectionSettings = null
    }

    private fun storedConnectionSettings(): ConnectionSettings? =
        enrollment.connectionSettings()
            ?: run {
                Log.w(TAG, "no usable connection settings — waiting for enrollment")
                null
            }

    private fun kioskTarget(policy: Policy?): String? =
        policy?.devicePolicy?.let { KioskPolicy.app(it)?.packageName }
}
