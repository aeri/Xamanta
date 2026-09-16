package cat.naval.xamanta.services

import android.os.SystemClock
import android.util.Log
import cat.naval.xamanta.enrollment.TokenResult
import cat.naval.xamanta.enrollment.TokenSource
import cat.naval.xamanta.util.Backoff
import cat.naval.xamanta.util.ServerTrust
import cat.naval.xamanta.util.runCatchingCancellable
import cat.naval.xamanta.models.ConnectionSettings
import cat.naval.xamanta.protos.ApplyPolicyResponse
import cat.naval.xamanta.protos.CommandResult
import cat.naval.xamanta.protos.CommandServiceGrpcKt
import cat.naval.xamanta.protos.PolicySyncServiceGrpcKt
import cat.naval.xamanta.protos.SyncRequest
import io.grpc.CallCredentials
import io.grpc.CallOptions
import io.grpc.Channel as GrpcChannel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "GrpcClient"

private const val INITIAL_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L
private const val REENROLLMENT_BACKOFF_MS = 5 * 60_000L
private const val STABLE_SESSION_MS = 5_000L

private const val COMPLIANCE_DEADLINE_S = 30L

private const val KEEPALIVE_TIME_S = 300L

private const val KEEPALIVE_TIMEOUT_S = 20L
private const val CHANNEL_SHUTDOWN_MS = 800L

private const val DESTRUCTIVE_FLUSH_MS = 2_000L

private val AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

class GrpcClient(
    settings: ConnectionSettings,
    private val syncRequest: SyncRequest,
    private val listener: GrpcListener,
    private val tokens: TokenSource,
    private val caProvider: () -> ByteArray?,
) {

    private inner class Session(val channel: ManagedChannel) {
        val policies = PolicySyncServiceGrpcKt.PolicySyncServiceCoroutineStub(channel)
            .withCallCredentials(credentials)
        val commands = CommandServiceGrpcKt.CommandServiceCoroutineStub(channel)
            .withCallCredentials(credentials)
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("GrpcClient") +
            CoroutineExceptionHandler { _, e -> Log.e(TAG, "uncaught in GrpcClient scope", e) }
    )

    private val credentials = BearerCredentials()

    private val reconnectSignal = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    private var settings: ConnectionSettings = settings

    @Volatile
    private var session: Session? = null

    private var connectionJob: Job? = null

    @Synchronized
    fun start() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch { connectionLoop() }
    }

    @Synchronized
    fun stop() {
        val channel = session?.channel
        session = null
        scope.cancel()
        channel?.shutdownNow()
    }

    fun reconfigure(settings: ConnectionSettings) {
        this.settings = settings
        reconnect()
    }

    fun reconnect() {
        reconnectSignal.trySend(Unit)
        session?.channel?.shutdownNow()
    }

    suspend fun reportCompliance(response: ApplyPolicyResponse) {
        withContext(Dispatchers.IO) {
            val stub = session?.policies
            if (stub == null) {
                Log.w(TAG, "no session — compliance report dropped")
                return@withContext
            }
            runCatchingCancellable {
                stub.withDeadlineAfter(COMPLIANCE_DEADLINE_S, TimeUnit.SECONDS)
                    .reportCompliance(response)
            }.onFailure {
                Log.e(TAG, "reportCompliance failed: ${it.message}", it)
                when (Status.fromThrowable(it).code) {
                    Status.Code.UNAUTHENTICATED -> onTokenRefused()

                    Status.Code.DEADLINE_EXCEEDED, Status.Code.UNAVAILABLE -> {
                        Log.w(TAG, "the session no longer carries calls — reconnecting")
                        reconnect()
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun onTokenRefused() {
        Log.w(TAG, "token refused — dropping it and re-authenticating the session")
        tokens.invalidate()
        reconnect()
    }

    private suspend fun connectionLoop() {
        val backoff = Backoff(INITIAL_BACKOFF_MS, MAX_BACKOFF_MS)
        try {
            while (true) {
                when (tokens.token()) {
                    is TokenResult.Available -> Unit

                    TokenResult.Transient -> {
                        Log.w(TAG, "no token yet — retrying")
                        awaitRetry(backoff.nextDelayMs())
                        continue
                    }

                    TokenResult.NeedsReenrollment -> {
                        Log.e(TAG, "device needs re-enrollment — pausing until it completes")
                        awaitRetry(REENROLLMENT_BACKOFF_MS)
                        continue
                    }
                }

                if (runSession()) backoff.reset()

                if (reconnectSignal.tryReceive().isSuccess) {
                    Log.i(TAG, "reconnecting immediately on request")
                    backoff.reset()
                    continue
                }
                awaitRetry(backoff.nextDelayMs())
            }
        } finally {
            closeSession()
        }
    }

    private suspend fun runSession(): Boolean {
        val startedAt = SystemClock.elapsedRealtime()
        try {
            val active = openSession()
            runStreams(active)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StatusException) {
            Log.e(TAG, "gRPC status [${e.status.code}]: ${e.status.description}")
            if (e.status.code == Status.Code.UNAUTHENTICATED) tokens.invalidate()
        } catch (e: Exception) {
            Log.e(TAG, "session failed — ${e::class.simpleName}: ${e.message}")
        } finally {
            closeSession()
            listener.onDisconnected()
        }
        return SystemClock.elapsedRealtime() - startedAt >= STABLE_SESSION_MS
    }

    private suspend fun runStreams(session: Session) = coroutineScope {
        val policies = launch {
            session.policies.syncPolicy(syncRequest).collect(listener::onPolicyReceived)
            Log.w(TAG, "policy stream closed by server")
        }
        val commands = launch { streamCommands(session) }
        policies.invokeOnCompletion { commands.cancel() }
        commands.invokeOnCompletion { policies.cancel() }
    }

    private suspend fun streamCommands(session: Session) {
        val results = Channel<CommandResult>(Channel.RENDEZVOUS)
        val outbound = flow {
            listener.pendingCommandResults().forEach { result ->
                emit(result)
                listener.onCommandResultSent(result.id)
            }
            emitAll(results.receiveAsFlow())
        }
        try {
            session.commands.commandChannel(outbound).collect { command ->
                val outcome = listener.onCommandReceived(command)
                val destructive = outcome.afterReported
                if (destructive == null) {
                    results.send(outcome.result)
                    listener.onCommandResultSent(outcome.result.id)
                    return@collect
                }
                withContext(NonCancellable) {
                    val sent = withTimeoutOrNull(DESTRUCTIVE_FLUSH_MS) {
                        results.send(outcome.result)
                    } != null
                    if (sent) listener.onCommandResultSent(outcome.result.id)
                    destructive()
                }
            }
            Log.w(TAG, "command channel closed by server")
        } finally {
            results.close()
        }
    }

    private fun openSession(): Session {
        val settings = this.settings
        Log.d(TAG, "connecting to ${settings.host}:${settings.port}")
        val channel = OkHttpChannelBuilder.forAddress(settings.host, settings.port)
            .useTransportSecurity()
            .keepAliveTime(KEEPALIVE_TIME_S, TimeUnit.SECONDS)
            .keepAliveTimeout(KEEPALIVE_TIMEOUT_S, TimeUnit.SECONDS)
            .intercept(ConnectionInterceptor(listener::onConnected))
            .apply {
                ServerTrust.socketFactory(caProvider())?.let { sslSocketFactory(it) }
            }
            .build()
        return Session(channel).also { session = it }
    }

    private fun closeSession() {
        val channel = session?.channel ?: return
        session = null
        channel.shutdown()
        scope.launch {
            if (!channel.awaitTermination(CHANNEL_SHUTDOWN_MS, TimeUnit.MILLISECONDS)) {
                channel.shutdownNow()
            }
        }
    }

    private suspend fun awaitRetry(delayMs: Long) {
        Log.i(TAG, "next connection attempt in ${delayMs}ms")
        withTimeoutOrNull(delayMs) { reconnectSignal.receive() }
    }

    private class ConnectionInterceptor(private val onConnected: () -> Unit) : ClientInterceptor {
        private val announced = AtomicBoolean(false)

        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: GrpcChannel,
        ): ClientCall<ReqT, RespT> =
            object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)
            ) {
                override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                    val wrapped =
                        object : ForwardingClientCallListener
                        .SimpleForwardingClientCallListener<RespT>(responseListener) {
                            override fun onHeaders(headers: Metadata) {
                                if (announced.compareAndSet(false, true)) onConnected()
                                super.onHeaders(headers)
                            }
                        }
                    super.start(wrapped, headers)
                }
            }
    }

    private inner class BearerCredentials : CallCredentials() {
        override fun applyRequestMetadata(
            requestInfo: RequestInfo,
            appExecutor: Executor,
            applier: MetadataApplier,
        ) {
            scope.launch {
                runCatchingCancellable {
                    when (val result = tokens.token()) {
                        is TokenResult.Available -> applier.apply(
                            Metadata().apply { put(AUTHORIZATION, "Bearer ${result.token}") }
                        )

                        TokenResult.Transient -> applier.fail(
                            Status.UNAVAILABLE.withDescription("no access token available")
                        )

                        TokenResult.NeedsReenrollment -> applier.fail(
                            Status.UNAUTHENTICATED.withDescription("device needs re-enrollment")
                        )
                    }
                }.onFailure {
                    applier.fail(Status.UNAVAILABLE.withDescription("token lookup failed").withCause(it))
                }
            }
        }
    }
}
