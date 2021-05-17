package com.draco.trackophile.viewmodels

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.draco.trackophile.repositories.constants.DownloaderState
import com.draco.trackophile.utils.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val WAKELOCK_TAG = "Trackophile:Download"
        const val WAKELOCK_TIMEOUT = 60 * 60 * 1000L
    }

    val downloader = Downloader(application.applicationContext)
    private val powerManager = application.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)

    init {
        /* Prepare the downloader by initializing */
        viewModelScope.launch(Dispatchers.IO) {
            downloader.init()
        }
    }

    /**
     * Download an audio track by URL
     */
    fun download(url: String) {
        if (downloader.state.value != DownloaderState.READY)
            return

        viewModelScope.launch(Dispatchers.IO) {
            wakelock.acquire(WAKELOCK_TIMEOUT)
            downloader.downloadAudio(url)
            downloader.writeToMediaStore()
            wakelock.release()
        }
    }
}