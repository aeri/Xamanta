package cat.naval.xamanta.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import cat.naval.xamanta.R

private const val TAG = "ManagementNotification"

object ManagementNotification {

    private const val ID = 1001
    private const val CHANNEL_ID = "xamanta_policy"

    fun startForeground(service: Service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        service.getString(R.string.management_notification_title),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                )
        }
        val notification = NotificationCompat.Builder(service, CHANNEL_ID)
            .setContentTitle(service.getString(R.string.management_notification_title))
            .setContentText(service.getString(R.string.management_notification_text))
            .setSmallIcon(R.drawable.baseline_sync_24)
            .setOngoing(true)
            .build()
        service.startForeground(ID, notification)
    }

    fun grantOwnPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!context.isDeviceOwner()) {
            Log.w(TAG, "not device owner — cannot self-grant POST_NOTIFICATIONS")
            return
        }
        runCatching {
            context.dpm.setPermissionGrantState(
                context.dpcAdmin,
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
            )
        }.onFailure { Log.e(TAG, "cannot grant POST_NOTIFICATIONS: ${it.message}") }
    }
}
