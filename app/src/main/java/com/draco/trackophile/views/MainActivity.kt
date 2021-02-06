package com.draco.trackophile.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
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
    private lateinit var uploader: TextView
    private lateinit var misc: TextView
    private lateinit var thumbnail: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress)
        title = findViewById(R.id.title)
        uploader = findViewById(R.id.uploader)
        misc = findViewById(R.id.misc)
        thumbnail = findViewById(R.id.thumbnail)

        viewModel.downloaderReady.observe(this) {
            progress.isIndeterminate = !it

            if (it == true) {
                if (intent?.action == Intent.ACTION_SEND) {
                    val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return@observe
                    viewModel.download(url)
                }
            }
        }

        viewModel.downloader.downloadProgress.observe(this) {
            progress.progress = it.roundToInt()

            if (it == 100f)
                finishAffinity()
        }

        viewModel.currentTrack.observe(this) {
            if (it != null) {
                title.text = it.title
                uploader.text = it.uploader

                val time = DateUtils.formatElapsedTime(it.duration.toLong())

                @SuppressLint("SetTextI18n")
                misc.text = "$time / ${it.id}"

                Glide
                    .with(this)
                    .load(it.thumbnail)
                    .placeholder(R.drawable.ic_baseline_image_24)
                    .circleCrop()
                    .into(thumbnail)
            }
        }

        viewModel.error.observe(this) {
            if (it != null) {
                title.setText(R.string.error)
                uploader.text = it
            }
        }
    }
}