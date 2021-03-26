package com.draco.trackophile.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.draco.trackophile.models.DownloaderState
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.*

class Downloader(private val context: Context) {
    companion object {
        /**
         * Audio format extension
         */
        const val AUDIO_FORMAT = "mp3"

        /**
         * YouTubeDL recognizable output format (appended to output location)
         */
        const val OUTPUT_FORMAT = "%(title)s ~ %(id)s.%(ext)s"
    }

    private val youtubeDL = YoutubeDL.getInstance()
    private val ffmpeg = FFmpeg.getInstance()

    /**
     * Download progress as a percentage
     */
    private val _downloadProgress = MutableLiveData(0f)
    val downloadProgress: LiveData<Float> = _downloadProgress

    /**
     * Report a recent exception
     */
    private val _error = MutableLiveData<String>(null)
    val error: LiveData<String> = _error

    /**
     * The current state of the downloader
     */
    private val _state = MutableLiveData(DownloaderState.INITIALIZING)
    val state: LiveData<DownloaderState> = _state

    /**
     * Where downloads are stored
     */
    private val downloadsFolder = context.getExternalFilesDir("downloads")!!

    /**
     * Where archives are stored
     */
    private val archivesFolder = context.getExternalFilesDir("archives")!!

    /**
     * An archive of existing downloads as to not download multiple times
     */
    private val archive = File("${archivesFolder.absolutePath}/archive.txt")

    fun init() {
        youtubeDL.init(context)
        ffmpeg.init(context)

        /* Update via internet */
        youtubeDL.updateYoutubeDL(context)

        _state.postValue(DownloaderState.READY)
    }

    /**
     * Download audio track given a URL and return an exception message
     */
    fun downloadAudio(url: String) {
        _state.postValue(DownloaderState.PROCESSING)
        _downloadProgress.postValue(0f)

        val request = YoutubeDLRequest(url)
            .addOption("--extract-audio")
            .addOption("--ignore-errors")
            .addOption("--audio-format", AUDIO_FORMAT)
            .addOption("--audio-quality", 0)
            .addOption("--embed-thumbnail")
            .addOption("--add-metadata")
            .addOption("--match-filter", "!is_live")
            .addOption("--no-overwrites")
            .addOption("--no-post-overwrites")
            .addOption("--download-archive", archive.absolutePath)
            .addOption("-o", "${downloadsFolder.absolutePath}/$OUTPUT_FORMAT")

        try {
            youtubeDL.execute(request) { percent: Float, _ ->
                _state.postValue(DownloaderState.DOWNLOADING)
                _downloadProgress.postValue(percent)
            }
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
            _error.postValue(e.message)
        } finally {
            _state.postValue(DownloaderState.COMPLETED)
        }
    }
}