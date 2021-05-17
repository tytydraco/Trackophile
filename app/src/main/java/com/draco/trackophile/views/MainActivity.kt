package com.draco.trackophile.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.draco.trackophile.R
import com.draco.trackophile.repositories.constants.DownloaderState
import com.draco.trackophile.viewmodels.MainActivityViewModel
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var progress: ProgressBar
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress)
        status = findViewById(R.id.status)

        /* Update progress bar */
        viewModel.downloader.downloadProgress.observe(this) {
            progress.isIndeterminate = (it == 0f || it == 100f)
            progress.progress = it.roundToInt()
        }

        /* Report error messages */
        viewModel.downloader.error.observe(this) {
            if (it != null) {
                progress.visibility = View.GONE
                status.text = it
            }
        }

        /* Handle downloader states */
        viewModel.downloader.state.observe(this) {
            when (it!!) {
                DownloaderState.INITIALIZING -> R.string.state_initializing
                DownloaderState.READY -> {
                    downloadFromIntent()
                    R.string.state_ready
                }
                DownloaderState.PROCESSING -> R.string.state_processing
                DownloaderState.DOWNLOADING -> R.string.state_downloading
                DownloaderState.FINALIZING -> R.string.state_finalizing
                DownloaderState.COMPLETED -> {
                    finishAffinity()
                    R.string.state_completed
                }
            }.also { resId ->
                status.setText(resId)
            }
        }
    }

    /**
     * Take the provided intent and extract relevant download information
     */
    private fun downloadFromIntent() {
        intent?.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
            viewModel.download(url)
            intent = null
        }
    }
}