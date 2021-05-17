package com.draco.trackophile.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.draco.trackophile.repositories.constants.DownloaderState
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.FileFilter
import java.nio.file.Files

class Downloader(private val context: Context) {
    companion object {
        const val AUDIO_FORMAT = "mp3"
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
    private val downloadsFolder = Files.createTempDirectory("downloads").toFile()

    fun init() {
        try {
            /* Prepare essential libraries */
            youtubeDL.init(context)
            ffmpeg.init(context)

            /* Update via internet */
            youtubeDL.updateYoutubeDL(context)

            /* We're ready to handle requests now! */
            _state.postValue(DownloaderState.READY)
        } catch (e: YoutubeDLException) {
            _error.postValue(e.message)
        }
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
            .addOption("-o", "${downloadsFolder.absolutePath}/$OUTPUT_FORMAT")

        try {
            youtubeDL.execute(request) { percent: Float, _ ->
                _state.postValue(DownloaderState.DOWNLOADING)
                _downloadProgress.postValue(percent)
            }
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
            _error.postValue(e.message)
        }
    }

    private fun getMediaStoreLocation(): Uri? {
        /* Where we should put the audio */
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    /**
     * Update Android MediaStore entries for downloaded tracks
     *
     * 1) Downloaded tracks are first stored internally for youtube-dl
     * 2) We iterate over all unwritten tracks and create MediaStore entries
     * 3) We write our track data to this new URI
     * 4) Tracks are deleted once completed
     */
    fun writeToMediaStore() {
        _state.postValue(DownloaderState.FINALIZING)
        _downloadProgress.postValue(0f)

        val contentResolver = context.contentResolver

        /* Get a list of all tracks */
        val fileList = downloadsFolder.listFiles(
            FileFilter {
                it.extension == AUDIO_FORMAT
            }
        )!!

        val collection = getMediaStoreLocation() ?: return
        /* Iterate over each one and write their contents to MediaStore URIs */
        for (index in fileList.indices) {
            val file = fileList[index]

            /* Don't worry; Android will handle metadata */
            val songContentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.nameWithoutExtension)
            }

            /* Copy contents over to new URI */
            val songUri = contentResolver.insert(collection, songContentValues) ?: return
            contentResolver.openOutputStream(songUri).use { outputStream ->
                file.inputStream().use {
                    it.copyTo(outputStream!!)
                }
            }

            /* Clean up finished file */
            file.delete()

            /* Update progress bar */
            val progress = ((index + 1f) / fileList.size)
            _downloadProgress.postValue(progress)
        }

        /* Clean up invalid files as well */
        downloadsFolder.deleteRecursively()
    }
}