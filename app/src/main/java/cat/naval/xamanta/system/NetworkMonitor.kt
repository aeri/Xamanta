package cat.naval.xamanta.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log

private const val TAG = "NetworkMonitor"

class NetworkMonitor(context: Context, private val onNetworkChanged: () -> Unit) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    @Volatile
    private var currentNetwork: Network? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (network == currentNetwork) return
            Log.d(TAG, "default network changed")
            currentNetwork = network
            onNetworkChanged()
        }

        override fun onLost(network: Network) {
            if (network == currentNetwork) currentNetwork = null
        }
    }

    fun register() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, callback)
            }
        }.onFailure { Log.e(TAG, "cannot observe networks: ${it.message}") }
    }

    fun unregister() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
