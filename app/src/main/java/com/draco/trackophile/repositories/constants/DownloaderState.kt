package com.draco.trackophile.repositories.constants

enum class DownloaderState {
    INITIALIZING,       /* Init and updates */
    READY,              /* Waiting for a request */
    PROCESSING,         /* Resolving request */
    DOWNLOADING,        /* Downloading content */
    FINALIZING,         /* Creating MediaStore entries */
    COMPLETED           /* Finished */
}