package com.draco.trackophile.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.draco.trackophile.models.Track
import com.draco.trackophile.utils.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val downloader = Downloader(application.applicationContext)

    private val _downloaderReady = MutableLiveData(false)
    val downloaderReady: LiveData<Boolean> = _downloaderReady

    private val _currentTrack = MutableLiveData<Track>(null)
    val currentTrack: LiveData<Track?> = _currentTrack

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
        if (downloader.isBusy.value == true)
            return

        viewModelScope.launch(Dispatchers.IO) {
            val track = downloader.getTrack(url)
            _currentTrack.postValue(track)

            downloader.downloadAudio(url)?.let {
                _error.postValue(it)
                _downloaderReady.postValue(false)
            }

            _currentTrack.postValue(null)
        }
    }
}