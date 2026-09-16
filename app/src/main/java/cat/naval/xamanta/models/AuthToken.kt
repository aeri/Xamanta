package cat.naval.xamanta.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthToken(
    val accessToken: String,
    val expiresAt: Long,
    val renewAt: Long = expiresAt,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= renewAt
}
