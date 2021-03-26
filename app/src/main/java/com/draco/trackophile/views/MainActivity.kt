package com.draco.trackophile.views

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.draco.trackophile.R
import com.draco.trackophile.models.DownloaderState
import com.draco.trackophile.viewmodels.MainActivityViewModel
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var progress: ProgressBar
    private lateinit var title: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress)
        title = findViewById(R.id.title)

        /* Update progress bar */
        viewModel.downloader.downloadProgress.observe(this) {
            progress.isIndeterminate = (it == 0f || it == 100f)
            progress.progress = it.roundToInt()
        }

        /* Report error messages */
        viewModel.downloader.error.observe(this) {
            if (it != null)
                title.text = it
        }

        /* Handle downloader states */
        viewModel.downloader.state.observe(this) {
            when (it!!) {
                DownloaderState.INITIALIZING -> title.setText(R.string.state_initializing)

                DownloaderState.READY -> {
                    title.setText(R.string.state_ready)
                    intent?.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                        viewModel.download(url)
                        intent = null
                    }
                }

                DownloaderState.PROCESSING -> title.setText(R.string.state_processing)

                DownloaderState.DOWNLOADING -> title.setText(R.string.state_downloading)

                DownloaderState.COMPLETED -> {
                    title.setText(R.string.state_completed)
                    finishAffinity()
                }
            }
        }
    }
}