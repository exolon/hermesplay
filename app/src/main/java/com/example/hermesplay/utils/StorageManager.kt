package com.example.hermesplay.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class StorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("HermesPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TV_FOLDER_URI = "TV_FOLDER_URI"
        private const val KEY_RESUME_PLAYBACK = "RESUME_PLAYBACK"
        private const val KEY_SERIES_THUMB_SIZE = "SERIES_THUMB_SIZE"
        private const val KEY_EPISODE_THUMB_SIZE = "EPISODE_THUMB_SIZE"
    }

    // --- System Storage Permissions ---

    fun saveTreeUri(uri: Uri) {
        // 1. Take persistable permission so access survives device reboots
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)

        // 2. Save the string representation of the URI
        prefs.edit().putString(KEY_TV_FOLDER_URI, uri.toString()).apply()
    }

    fun getSavedTreeUri(): Uri? {
        val uriString = prefs.getString(KEY_TV_FOLDER_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    fun hasValidAccess(): Boolean {
        val uri = getSavedTreeUri() ?: return false

        // Aggressive Anti-Regression check:
        // Just because we saved the URI doesn't mean the user didn't move or delete the folder via a File Manager.
        // We must verify the system still actively recognizes our permission grant.
        val persistedUris = context.contentResolver.persistedUriPermissions
        return persistedUris.any { it.uri == uri }
    }

    // --- App Settings ---

    fun setResumePlayback(enabled: Boolean) = prefs.edit().putBoolean(KEY_RESUME_PLAYBACK, enabled).apply()
    fun getResumePlayback(): Boolean = prefs.getBoolean(KEY_RESUME_PLAYBACK, false) // Default: Start at 0:00

    // Size stored as a float multiplier (1.0f = default, 1.5f = 50% larger, etc.)
    fun setSeriesThumbSize(size: Float) = prefs.edit().putFloat(KEY_SERIES_THUMB_SIZE, size).apply()
    fun getSeriesThumbSize(): Float = prefs.getFloat(KEY_SERIES_THUMB_SIZE, 1.0f)

    fun setEpisodeThumbSize(size: Float) = prefs.edit().putFloat(KEY_EPISODE_THUMB_SIZE, size).apply()
    fun getEpisodeThumbSize(): Float = prefs.getFloat(KEY_EPISODE_THUMB_SIZE, 1.0f)
}