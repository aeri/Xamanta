package cat.naval.xamanta.models

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionSettings(
    val host: String,
    val port: Int = DEFAULT_GRPC_PORT,
) {
    companion object {
        const val DEFAULT_GRPC_PORT = 8443
    }
}
