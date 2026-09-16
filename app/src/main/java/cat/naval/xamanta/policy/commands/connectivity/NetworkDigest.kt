package cat.naval.xamanta.policy.commands.connectivity

import cat.naval.xamanta.models.NetworkConfiguration
import cat.naval.xamanta.models.OncCertificate
import cat.naval.xamanta.util.json
import cat.naval.xamanta.util.sha256Hex

fun NetworkConfiguration.digest(certificates: List<OncCertificate>): String {
    val referenced = buildSet {
        wifi.eap?.let { eap ->
            addAll(eap.serverCaRefs)
            eap.clientCertRef?.let(::add)
        }
    }
    val material = certificates.filter { it.guid in referenced }.sortedBy { it.guid }
    return (json.encodeToString(this) + json.encodeToString(material)).sha256Hex()
}
