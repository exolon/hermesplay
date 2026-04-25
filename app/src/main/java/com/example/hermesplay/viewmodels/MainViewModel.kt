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
    data class Success(
        val items: List<MediaItem>,
        val isRoot: Boolean,
        val hiddenUris: Set<String>,
        val showHidden: Boolean
    ) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(
    private val storageManager: StorageManager,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val folderStack = mutableListOf<Uri>()
    private var isShowingHidden = false // Tracks if the "Eye" icon is open

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
            val root = folderStack.first()
            folderStack.clear()
            folderStack.add(root)
            fetchFolderContents(root)
        }
    }

    fun refreshCurrentFolder() {
        if (folderStack.isNotEmpty()) {
            val current = folderStack.last()
            videoRepository.invalidateCache(current)
            fetchFolderContents(current)
        }
    }

    fun rescanEverything() {
        videoRepository.clearAllCache()
        loadRootFolder()
    }

    // --- NEW: Hidden Items Handlers ---
    fun toggleShowHidden() {
        isShowingHidden = !isShowingHidden
        if (folderStack.isNotEmpty()) fetchFolderContents(folderStack.last())
    }

    fun toggleHideItem(uri: Uri) {
        videoRepository.toggleHiddenUri(uri.toString())
        if (folderStack.isNotEmpty()) fetchFolderContents(folderStack.last())
    }

    private fun fetchFolderContents(uri: Uri) {
        viewModelScope.launch {
            if (!videoRepository.isCached(uri)) _uiState.value = UiState.Loading
            try {
                val items = videoRepository.getContents(uri)
                val isRoot = folderStack.size == 1
                val hiddenUris = videoRepository.getHiddenUris()
                _uiState.value = UiState.Success(items, isRoot, hiddenUris, isShowingHidden)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed: ${e.message}")
            }
        }
    }
}