package com.example.hermesplay.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.hermesplay.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {
    // Permanent Disk Storage for Cache and Hidden Files
    private val diskCache = context.getSharedPreferences("hermes_repo_cache", Context.MODE_PRIVATE)
    private val hiddenPrefs = context.getSharedPreferences("hermes_hidden_prefs", Context.MODE_PRIVATE)

    // Fast RAM Storage so we don't read the disk unnecessarily while scrolling
    private val memCache = mutableMapOf<String, List<MediaItem>>()

    fun isCached(folderUri: Uri): Boolean = memCache.containsKey(folderUri.toString()) || diskCache.contains(folderUri.toString())

    fun invalidateCache(folderUri: Uri) {
        memCache.remove(folderUri.toString())
        diskCache.edit().remove(folderUri.toString()).apply()
    }

    fun clearAllCache() {
        memCache.clear()
        diskCache.edit().clear().apply()
    }

    // --- HIDDEN ITEM LOGIC ---
    fun getHiddenUris(): Set<String> = hiddenPrefs.getStringSet("hidden_uris", emptySet()) ?: emptySet()

    fun toggleHiddenUri(uri: String) {
        val current = getHiddenUris().toMutableSet()
        if (current.contains(uri)) current.remove(uri) else current.add(uri)
        hiddenPrefs.edit().putStringSet("hidden_uris", current).apply()
    }

    // --- SCANNING & CACHING LOGIC ---
    suspend fun getContents(folderUri: Uri): List<MediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = folderUri.toString()

        // 1. Check RAM
        memCache[cacheKey]?.let { return@withContext it }

        // 2. Check Permanent Disk Cache
        val savedData = diskCache.getStringSet(cacheKey, null)
        if (savedData != null) {
            val items = mutableListOf<MediaItem>()
            for (line in savedData) {
                val parts = line.split("<||>") // Safe delimiter
                if (parts.size == 4) {
                    val type = parts[0]
                    val uri = Uri.parse(parts[2])
                    val thumb = if (parts[3] == "NULL") null else Uri.parse(parts[3])
                    if (type == "F") items.add(MediaItem.Folder(parts[1], uri, thumb))
                    else if (type == "V") items.add(MediaItem.Video(parts[1], uri, thumb))
                }
            }
            val sorted = items.sortedBy { it.name }
            memCache[cacheKey] = sorted
            return@withContext sorted
        }

        // 3. Fallback: Full Disk Scan
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val items = mutableListOf<MediaItem>()

        rootDoc.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            if (file.isDirectory) {
                val folderThumb = file.findFile("poster.jpg")?.uri ?: file.findFile("$name-poster.jpg")?.uri
                items.add(MediaItem.Folder(name, file.uri, folderThumb))
            } else if (file.type?.startsWith("video/") == true) {
                val baseName = name.substringBeforeLast(".")
                val videoThumb = rootDoc.findFile("$baseName-thumb.jpg")?.uri
                items.add(MediaItem.Video(name, file.uri, videoThumb ?: file.uri))
            }
        }

        val sortedItems = items.sortedBy { it.name }
        memCache[cacheKey] = sortedItems

        // Serialize and Save to Disk Cache
        val serializedSet = sortedItems.map { item ->
            val type = if (item is MediaItem.Folder) "F" else "V"
            val thumb = item.thumbnailUri?.toString() ?: "NULL"
            "$type<||>${item.name}<||>${item.uri}<||>$thumb"
        }.toSet()
        diskCache.edit().putStringSet(cacheKey, serializedSet).apply()

        return@withContext sortedItems
    }
}