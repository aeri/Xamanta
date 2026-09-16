package cat.naval.xamanta.enrollment

import android.util.Log
import cat.naval.xamanta.models.DeviceMetadata
import cat.naval.xamanta.models.RegistrationResponse
import cat.naval.xamanta.util.json
import cat.naval.xamanta.util.postForJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.net.ssl.SSLSocketFactory

private const val TAG = "RegistrationClient"

object RegistrationClient {

    @Serializable
    private data class RegistrationRequest(
        @SerialName("android_id") val androidId: String,
        @SerialName("client_name") val clientName: String? = null,
        @SerialName("software_id") val softwareId: String? = null,
        val imei: String? = null,
        val meid: String? = null,
        val serial: String? = null,
        val esid: String? = null,
        val manufacturer: String? = null,
        val brand: String? = null,
        val model: String? = null,
    )

    fun registerEndpointFrom(tokenEndpoint: String): String =
        tokenEndpoint.trimEnd('/').substringBeforeLast("/") + "/register"

    suspend fun register(
        registerEndpoint: String,
        enrollmentToken: String,
        androidId: String,
        metadata: DeviceMetadata,
        clientName: String?,
        softwareId: String?,
        sslSocketFactory: SSLSocketFactory? = null,
    ): RegistrationResponse {
        Log.d(TAG, "registering at $registerEndpoint")
        val body = json.encodeToString(
            RegistrationRequest(
                androidId = androidId,
                clientName = clientName?.take(128),
                softwareId = softwareId,
                imei = metadata.imei,
                meid = metadata.meid,
                serial = metadata.serial,
                esid = metadata.esid,
                manufacturer = metadata.manufacturer,
                brand = metadata.brand,
                model = metadata.model,
            )
        )
        return json.decodeFromString(
            postForJson(
                endpoint = registerEndpoint,
                body = body,
                contentType = "application/json",
                authorization = "Bearer $enrollmentToken",
                sslSocketFactory = sslSocketFactory,
            )
        )
    }
}
