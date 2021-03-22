package com.draco.trackophile.views

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.draco.trackophile.R
import com.draco.trackophile.viewmodels.MainActivityViewModel
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var thumbnail: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress)
        title = findViewById(R.id.title)
        thumbnail = findViewById(R.id.thumbnail)

        viewModel.downloaderReady.observe(this) {
            /* Load infinitely while updating downloader */
            progress.isIndeterminate = !it

            when (it) {
                false -> title.setText(R.string.first_launch_title)
                true -> title.setText(R.string.default_title)
            }

            /* Handle URL */
            if (it == true) {
                if (intent?.action == Intent.ACTION_SEND) {
                    val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return@observe
                    viewModel.download(url)
                }

                /* Prevent re-download */
                intent = null
            }
        }

        /* Update progress bar */
        viewModel.downloader.downloadProgress.observe(this) {
            progress.progress = it.roundToInt()
        }

        /* Show current track information */
        viewModel.currentTrack.observe(this) {
            if (it != null) {
                title.text = it.title

                Glide
                    .with(this)
                    .load(it.thumbnail)
                    .placeholder(R.drawable.ic_baseline_music_note_24)
                    .into(thumbnail)
            }
        }

        /* Panic on an error */
        viewModel.error.observe(this) {
            if (it != null)
                title.setText(R.string.error)
        }

        /* Finish once no longer busy and downloader is ready */
        viewModel.downloader.isBusy.observe(this) {
            if (it == false && viewModel.downloaderReady.value == true)
                finishAffinity()
        }
    }
}