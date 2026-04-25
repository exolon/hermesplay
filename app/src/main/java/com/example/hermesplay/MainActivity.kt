package com.example.hermesplay

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.hermesplay.utils.StorageManager
import com.example.hermesplay.utils.VideoRepository
import com.example.hermesplay.viewmodels.MainViewModel
import com.example.hermesplay.viewmodels.UiState

class MainActivity : ComponentActivity() {
    private lateinit var storageManager: StorageManager
    private lateinit var videoRepository: VideoRepository

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(storageManager, videoRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Hardware Landscape Lock
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 2. True Fullscreen (Immersive Mode)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        storageManager = StorageManager(this)
        videoRepository = VideoRepository(this)

        setContent {
            // 3. OLED Black Theme
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1A1A1A),
                    onSurfaceVariant = Color.White
                )
            ) {
                val context = LocalContext.current

                // 4. Coil Image Loader (Handles video fallbacks + caching)
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components { add(VideoFrameDecoder.Factory()) }
                        .memoryCache {
                            MemoryCache.Builder(context)
                                .maxSizePercent(0.25)
                                .build()
                        }
                        .diskCache {
                            DiskCache.Builder()
                                .directory(context.cacheDir.resolve("thumbnail_vault"))
                                .maxSizeBytes(500L * 1024 * 1024)
                                .build()
                        }
                        .build()
                }

                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AppEntryPoint(storageManager, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppEntryPoint(storageManager: StorageManager, viewModel: MainViewModel) {
    var hasAccess by remember { mutableStateOf(storageManager.hasValidAccess()) }
    val uiState by viewModel.uiState.collectAsState()
    var playingVideoUri by remember { mutableStateOf<Uri?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            storageManager.saveTreeUri(uri)
            hasAccess = true
            viewModel.loadRootFolder()
        }
    }

    if (hasAccess) {
        val isRoot = (uiState as? UiState.Success)?.isRoot == true

        // Intercept back swipes
        BackHandler(enabled = playingVideoUri != null || !isRoot) {
            if (playingVideoUri != null) {
                playingVideoUri = null
            } else {
                viewModel.navigateBack()
            }
        }

        // Screen Routing
        if (playingVideoUri != null) {
            VideoPlayerScreen(videoUri = playingVideoUri!!)
        } else {
            DirectoryScreen(
                uiState = uiState,
                onFolderClick = { folder -> viewModel.openFolder(folder.uri) },
                onVideoClick = { video -> playingVideoUri = video.uri },
                onNavigateUp = { viewModel.navigateBack() },
                onJumpToRoot = { viewModel.jumpToRoot() },
                onRefresh = { viewModel.refreshCurrentFolder() },
                onRescanAll = { viewModel.rescanEverything() } // Now properly hooked up!
            )
        }
    } else {
        // Initial setup screen
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Welcome to HermesPlay!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = { folderPickerLauncher.launch(null) }) {
                Text("Select TV Folder")
            }
        }
    }
}