package com.example.hermesplay.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hermesplay.models.MediaItem
import com.example.hermesplay.utils.StorageManager
import com.example.hermesplay.utils.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Loading : UiState
    data class Success(val items: List<MediaItem>, val isRoot: Boolean) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(
    private val storageManager: StorageManager,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val folderStack = mutableListOf<Uri>()

    init { loadRootFolder() }

    fun loadRootFolder() {
        val rootUri = storageManager.getSavedTreeUri()
        if (rootUri != null) {
            folderStack.clear()
            folderStack.add(rootUri)
            fetchFolderContents(rootUri)
        } else {
            _uiState.value = UiState.Error("Root folder permission lost.")
        }
    }

    fun openFolder(folderUri: Uri) {
        folderStack.add(folderUri)
        fetchFolderContents(folderUri)
    }

    fun navigateBack(): Boolean {
        if (folderStack.size > 1) {
            folderStack.removeLast()
            fetchFolderContents(folderStack.last())
            return true
        }
        return false
    }

    fun jumpToRoot() {
        if (folderStack.size > 1) {
            val rootFolder = folderStack.first()
            folderStack.clear()
            folderStack.add(rootFolder)
            fetchFolderContents(rootFolder)
        }
    }

    // NEW: Forces a rescan of the current folder
    fun refreshCurrentFolder() {
        if (folderStack.isNotEmpty()) {
            val currentFolder = folderStack.last()
            videoRepository.invalidateCache(currentFolder)
            fetchFolderContents(currentFolder)
        }
    }

    private fun fetchFolderContents(uri: Uri) {
        viewModelScope.launch {
            if (!videoRepository.isCached(uri)) { _uiState.value = UiState.Loading }
            try {
                val items = videoRepository.getContents(uri)
                val isRoot = folderStack.size == 1
                _uiState.value = UiState.Success(items, isRoot)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load folder: ${e.message}")
            }
        }
    }
    // NEW: Wipes the entire cache and reloads from the top
    fun rescanEverything() {
        videoRepository.clearAllCache()
        loadRootFolder()
    }
}