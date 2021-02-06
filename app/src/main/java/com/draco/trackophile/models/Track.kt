package com.draco.trackophile.models

data class Track(
    val uploader: String,
    val title: String,
    val thumbnail: String,
    val duration: Int,
    val id: String
)