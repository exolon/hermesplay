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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.hermesplay.models.MediaItem
import com.example.hermesplay.utils.StorageManager
import com.example.hermesplay.utils.VideoRepository
import com.example.hermesplay.viewmodels.MainViewModel
import com.example.hermesplay.viewmodels.UiState
import kotlin.random.Random

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

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        storageManager = StorageManager(this)
        videoRepository = VideoRepository(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF1A1A1A), onSurfaceVariant = Color.White)) {
                val context = LocalContext.current
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components { add(VideoFrameDecoder.Factory()) }
                        .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
                        .diskCache { DiskCache.Builder().directory(context.cacheDir.resolve("thumbnail_vault")).maxSizeBytes(500L * 1024 * 1024).build() }
                        .build()
                }

                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    // Start with a solid black backdrop so the dimmed wallpaper looks rich
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        AppEntryPointWithSplash(storageManager, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppEntryPointWithSplash(storageManager: StorageManager, viewModel: MainViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    val splashResId = remember { if (Random.nextBoolean()) R.drawable.splash1 else R.drawable.splash2 }

    // Animate the wallpaper fading from 100% brightness to 20% brightness
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (showSplash) 1f else 0.45f,
        animationSpec = tween(durationMillis = 800),
        label = "WallpaperFade"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Bottom Layer: The Persistent Wallpaper
        Image(
            painter = painterResource(id = splashResId),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = backgroundAlpha
        )

        // 2. Middle Layer: The App (Fades in when tapped)
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(durationMillis = 800)),
            modifier = Modifier.fillMaxSize()
        ) {
            AppEntryPoint(storageManager, viewModel)
        }

        // 3. Top Layer: The "Tap to Enter" overlay (Vanishes when tapped)
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = 800)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showSplash = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Tap to Enter!",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 64.dp)
                )
            }
        }
    }
}

@Composable
fun AppEntryPoint(storageManager: StorageManager, viewModel: MainViewModel) {
    var hasAccess by remember { mutableStateOf(storageManager.hasValidAccess()) }
    val uiState by viewModel.uiState.collectAsState()

    var playingPlaylist by remember { mutableStateOf<List<MediaItem.Video>?>(null) }
    var playingStartIndex by remember { mutableIntStateOf(0) }

    val folderPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) { storageManager.saveTreeUri(uri); hasAccess = true; viewModel.loadRootFolder() }
    }

    if (hasAccess) {
        val isRoot = (uiState as? UiState.Success)?.isRoot == true

        BackHandler(enabled = playingPlaylist != null || !isRoot) {
            if (playingPlaylist != null) playingPlaylist = null else viewModel.navigateBack()
        }

        if (playingPlaylist != null) {
            VideoPlayerScreen(playlist = playingPlaylist!!, startIndex = playingStartIndex)
        } else {
            DirectoryScreen(
                uiState = uiState,
                onFolderClick = { folder -> viewModel.openFolder(folder.uri) },
                onVideoClick = { video, index, playlist -> playingStartIndex = index; playingPlaylist = playlist },
                onNavigateUp = { viewModel.navigateBack() },
                onJumpToRoot = { viewModel.jumpToRoot() },
                onRefresh = { viewModel.refreshCurrentFolder() },
                onRescanAll = { viewModel.rescanEverything() },
                onToggleShowHidden = { viewModel.toggleShowHidden() },
                onToggleHideItem = { uri -> viewModel.toggleHideItem(uri) }
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Welcome to HermesPlay!", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
            Button(onClick = { folderPickerLauncher.launch(null) }) { Text("Select TV Folder") }
        }
    }
}