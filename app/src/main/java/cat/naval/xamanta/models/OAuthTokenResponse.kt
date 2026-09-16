package cat.naval.xamanta.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)
