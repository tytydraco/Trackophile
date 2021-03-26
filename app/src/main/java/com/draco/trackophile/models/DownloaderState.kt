package com.draco.trackophile.models

enum class DownloaderState {
    INITIALIZING,       /* Init and updates */
    READY,              /* Waiting for a request */
    PROCESSING,         /* Resolving request */
    DOWNLOADING,        /* Downloading content */
    COMPLETED           /* Finished downloading */
}