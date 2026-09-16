package cat.naval.xamanta.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
)
