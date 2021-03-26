package com.draco.trackophile.viewmodels

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.draco.trackophile.utils.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val WAKELOCK_TAG = "Trackophile:Download"
        const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L
    }

    val downloader = Downloader(application.applicationContext)
    private val powerManager = application.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)

    private val _downloaderReady = MutableLiveData(false)
    val downloaderReady: LiveData<Boolean> = _downloaderReady

    private val _error = MutableLiveData<String>(null)
    val error: LiveData<String> = _error

    init {
        /* Prepare the downloader by initializing */
        viewModelScope.launch(Dispatchers.IO) {
            downloader.init()
            _downloaderReady.postValue(true)
        }
    }

    /**
     * Download an audio track by URL
     */
    fun download(url: String) {
        /* Do not overlap downloads */
        if (downloader.isBusy.value == true)
            return

        viewModelScope.launch(Dispatchers.IO) {
            wakelock.acquire(WAKELOCK_TIMEOUT)

            /* Download the track and post errors if they occur */
            downloader.downloadAudio(url)?.let {
                _error.postValue(it)
                _downloaderReady.postValue(false)
            }

            wakelock.release()
        }
    }
}