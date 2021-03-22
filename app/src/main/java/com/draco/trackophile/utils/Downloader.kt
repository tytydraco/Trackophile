package com.draco.trackophile.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.draco.trackophile.models.Track
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class Downloader(private val context: Context) {
    companion object {
        /**
         * Audio format extension
         */
        const val AUDIO_FORMAT = "mp3"

        /**
         * YouTubeDL recognizable output format (appended to output location)
         */
        const val OUTPUT_FORMAT = "%(creator)s - %(title)s.%(ext)s"
    }

    private val youtubeDL = YoutubeDL.getInstance()
    private val ffmpeg = FFmpeg.getInstance()

    /**
     * Download progress as a percentage
     */
    private val _downloadProgress = MutableLiveData(0f)
    val downloadProgress: LiveData<Float> = _downloadProgress

    /**
     * Is the downloader doing work right now
     */
    private val _isBusy = MutableLiveData(false)
    val isBusy: LiveData<Boolean> = _isBusy

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
    }

    /**
     * Return Track containing track info from URL
     */
    fun getTrack(url: String): Track? {
        val info = try {
            youtubeDL.getInfo(url)
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
            return null
        }

        with (info) {
            return Track(
                uploader,
                title,
                thumbnail,
                duration,
                id
            )
        }
    }

    /**
     * Download audio track given a URL and return an exception message
     */
    fun downloadAudio(url: String): String? {
        _isBusy.postValue(true)
        _downloadProgress.postValue(0f)

        val request = YoutubeDLRequest(url)
            .addOption("-x")
            .addOption("-i")
            .addOption("-w")
            .addOption("--audio-format", AUDIO_FORMAT)
            .addOption("--audio-quality", 0)
            .addOption("--embed-thumbnail")
            .addOption("--add-metadata")
            .addOption("--match-filter", "!is_live")
            .addOption("--no-post-overwrites")
            .addOption("--download-archive", archive.absolutePath)
            .addOption("-o", "${downloadsFolder.absolutePath}/$OUTPUT_FORMAT")

        try {
            youtubeDL.execute(request) { percent: Float, _ ->
                _downloadProgress.postValue(percent)
            }
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
            return e.message
        } finally {
            _isBusy.postValue(false)
        }

        return null
    }
}