package cat.naval.xamanta.models

import kotlinx.serialization.Serializable

@Serializable
class ProvisionedNetwork(
    val networkId: Int,
    val digest: String? = null,
)
