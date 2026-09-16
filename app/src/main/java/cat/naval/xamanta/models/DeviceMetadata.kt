package cat.naval.xamanta.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceMetadata(
    val imei: String? = null,
    val meid: String? = null,
    val serial: String? = null,
    val esid: String? = null,
    val manufacturer: String? = null,
    val brand: String? = null,
    val model: String? = null,
)
