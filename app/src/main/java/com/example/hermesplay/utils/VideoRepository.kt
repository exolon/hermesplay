package com.example.hermesplay.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.hermesplay.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {
    private val cachedContents = mutableMapOf<Uri, List<MediaItem>>()

    fun isCached(folderUri: Uri): Boolean = cachedContents.containsKey(folderUri)

    fun invalidateCache(folderUri: Uri) {
        cachedContents.remove(folderUri)
    }

    // NEW: Clears everything for the Global Rescan
    fun clearAllCache() {
        cachedContents.clear()
    }

    suspend fun getContents(folderUri: Uri): List<MediaItem> = withContext(Dispatchers.IO) {
        cachedContents[folderUri]?.let { return@withContext it }

        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val allFiles = rootDoc.listFiles()
        val items = mutableListOf<MediaItem>()

        allFiles.forEach { file ->
            val name = file.name ?: return@forEach

            if (file.isDirectory) {
                // 1. Look INSIDE the folder for show (poster.jpg) or season (name-poster.jpg) thumbnails
                val folderThumb = file.findFile("poster.jpg")?.uri
                    ?: file.findFile("$name-poster.jpg")?.uri

                items.add(MediaItem.Folder(name = name, uri = file.uri, thumbnailUri = folderThumb))
            } else if (file.type?.startsWith("video/") == true) {
                // 2. Look NEXT TO the video for the episode thumbnail
                val baseName = name.substringBeforeLast(".")
                val thumbName = "$baseName-thumb.jpg"
                val videoThumb = allFiles.find { it.name?.equals(thumbName, ignoreCase = true) == true }?.uri

                items.add(MediaItem.Video(name = name, uri = file.uri, thumbnailUri = videoThumb ?: file.uri))
            }
        }

        val sortedItems = items.sortedBy { it.name }
        cachedContents[folderUri] = sortedItems
        return@withContext sortedItems
    }
}