package com.draco.trackophile.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.draco.trackophile.services.DownloadWorker

class DownloadActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    "url" to getIntentUrl()
                )
            )
            .build()

        WorkManager
            .getInstance(this)
            .enqueue(downloadRequest)
    }

    /**
     * Take the provided intent and extract relevant download information
     */
    private fun getIntentUrl(): String? {
        var url: String? = null

        intent?.getStringExtra(Intent.EXTRA_TEXT)?.let {
            url = it
            intent = null
        }

        return url
    }
}