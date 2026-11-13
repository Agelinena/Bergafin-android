package org.jellyfin.mobile.bergafin.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.jellyfin.mobile.R

internal object UpdateNotifications {
    private const val CHANNEL_ID = "bergafin_updates"
    private const val NOTIFICATION_ID = 0x42455246
    private const val TIMEOUT_MS = 10_000L

    fun showAvailable(context: Context, release: Release) {
        val pending = PendingIntent.getActivity(
            context,
            0,
            UpdateActivity.intent(context, release),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        notify(
            context,
            builder(context)
                .setContentTitle(context.getString(R.string.bergafin_update_available_title, release.versionName))
                .setContentText(context.getString(R.string.bergafin_update_available_text))
                .setContentIntent(pending)
                .addAction(0, context.getString(R.string.bergafin_update_action), pending)
                .setAutoCancel(true),
        )
    }

    fun showUpToDate(context: Context) = notifyText(context, R.string.bergafin_update_up_to_date)

    fun showCheckFailed(context: Context) = notifyText(context, R.string.bergafin_update_check_failed)

    private fun notifyText(context: Context, text: Int) {
        notify(
            context,
            builder(context)
                .setContentTitle(context.getString(R.string.bergafin_update_title))
                .setContentText(context.getString(text))
                .setTimeoutAfter(TIMEOUT_MS)
                .setAutoCancel(true),
        )
    }

    private fun builder(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    @SuppressLint("MissingPermission") // conferida logo abaixo
    private fun notify(context: Context, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.bergafin_update_channel))
                .build(),
        )
        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
