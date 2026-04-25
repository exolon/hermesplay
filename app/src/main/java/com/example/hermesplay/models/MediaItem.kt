package com.example.hermesplay.models

import android.net.Uri

sealed class MediaItem {
    abstract val name: String
    abstract val uri: Uri
    abstract val thumbnailUri: Uri?

    data class Folder(
        override val name: String,
        override val uri: Uri,
        override val thumbnailUri: Uri? = null
    ) : MediaItem()

    data class Video(
        override val name: String,
        override val uri: Uri,
        override val thumbnailUri: Uri? = null
    ) : MediaItem()
}