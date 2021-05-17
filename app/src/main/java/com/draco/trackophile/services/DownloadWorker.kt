package com.draco.trackophile.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.draco.trackophile.R
import com.draco.trackophile.repositories.constants.DownloaderState
import com.draco.trackophile.utils.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

class DownloadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        const val WAKELOCK_TAG = "Trackophile:Download"
        const val WAKELOCK_TIMEOUT = 60 * 60 * 1000L
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: Notification.Builder

    /* Generate unique ID so we can download multiple things at once */
    private val notificationId = Calendar.getInstance().timeInMillis.toInt()

    private val downloader = Downloader(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val wakelock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        WAKELOCK_TAG
    )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            context.getString(R.string.notif_channel_id),
            context.getString(R.string.notif_channel_title),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun createNotification() {
        notificationBuilder = Notification.Builder(context, context.getString(R.string.notif_channel_id))
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.state_ready))
            .setSmallIcon(R.drawable.ic_baseline_play_circle_24)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)

        /* Hook our worker to a foreground object to support long-running workers */
        val foregroundInfo = ForegroundInfo(notificationId, notification)
        setForeground(foregroundInfo)
    }

    private fun prepareObservables() {
        downloader.state.observeForever { state ->
            when (state!!) {
                DownloaderState.INITIALIZING -> R.string.state_initializing
                DownloaderState.READY -> R.string.state_ready
                DownloaderState.PROCESSING -> R.string.state_processing
                DownloaderState.DOWNLOADING -> R.string.state_downloading
                DownloaderState.FINALIZING -> R.string.state_finalizing
                DownloaderState.COMPLETED -> R.string.state_completed
            }.also { resId ->
                /* Notification shouldn't be updated as it has been cancelled */
                if (!notificationManager.activeNotifications.any { it.id == notificationId })
                    return@also

                notificationBuilder
                    .setContentText(context.getString(resId))
                    .build()
                    .also {
                        notificationManager.notify(notificationId, it)
                    }
            }
        }

        downloader.error.observeForever { error ->
            if (error != null) {
                notificationBuilder
                    .setContentText(error)
                    .setOngoing(false)
                    .build()
                    .also {
                        notificationManager.notify(notificationId, it)
                    }
            }
        }

        downloader.downloadProgress.observeForever {
            notificationBuilder
                .setProgress(100, it.roundToInt(), (it == 0f || it == 100f))
                .build()
                .also { notification ->
                    notificationManager.notify(notificationId, notification)
                }
        }
    }

    override suspend fun doWork(): Result {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /* Prepare  */
        createNotificationChannel()
        createNotification()

        val url = workerParams.inputData.getString("url") ?: return Result.failure()

        /* LiveData observation requires the main thread */
        coroutineScope {
            launch(Dispatchers.Main) {
                prepareObservables()
            }
        }

        /* Begin download process */
        wakelock.acquire(WAKELOCK_TIMEOUT)
        downloader.init()
        downloader.downloadAudio(url)
        downloader.writeToMediaStore()
        wakelock.release()

        notificationManager.cancel(notificationId)

        return Result.success()
    }
}