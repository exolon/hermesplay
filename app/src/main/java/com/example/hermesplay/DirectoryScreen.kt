package com.example.hermesplay

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.hermesplay.models.MediaItem
import com.example.hermesplay.viewmodels.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    uiState: UiState,
    onFolderClick: (MediaItem.Folder) -> Unit,
    onVideoClick: (MediaItem.Video, Int, List<MediaItem.Video>) -> Unit,
    onNavigateUp: () -> Unit,
    onJumpToRoot: () -> Unit,
    onRefresh: () -> Unit,
    onRescanAll: () -> Unit,
    onToggleShowHidden: () -> Unit,
    onToggleHideItem: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE)
    var gridSize by remember { mutableFloatStateOf(prefs.getFloat("grid_size", 180f)) }

    // Parent HUD State
    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(0) }

    // Background loop to update the clock and battery
    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(5000) // Update every 5 seconds
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = R.mipmap.ic_launcher,
                            contentDescription = "App Icon",
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("HermesPlay", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    // The Parent HUD (Clock & Battery)
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = currentTime, color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        Text(text = "Battery: $batteryLevel%", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp).width(180.dp)) {
                        Text("Size", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = gridSize,
                            onValueChange = { gridSize = it },
                            onValueChangeFinished = { prefs.edit().putFloat("grid_size", gridSize).apply() },
                            valueRange = 120f..400f,
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.DarkGray)
                        )
                    }

                    if (uiState is UiState.Success) {
                        IconButton(onClick = onToggleShowHidden) {
                            Icon(if (uiState.showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Toggle Hidden")
                        }

                        if (uiState.isRoot) {
                            IconButton(onClick = onRescanAll) { Icon(Icons.Default.Sync, "Rescan All") }
                        } else {
                            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
                            IconButton(onClick = onJumpToRoot) { Icon(Icons.Default.Home, "Root") }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                is UiState.Error -> Text(uiState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    val visibleItems = if (uiState.showHidden) uiState.items else uiState.items.filter { !uiState.hiddenUris.contains(it.uri.toString()) }

                    if (visibleItems.isEmpty() && uiState.isRoot) {
                        Text("This folder is empty!", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = gridSize.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!uiState.isRoot) item { BackNavigationCard(onClick = onNavigateUp) }

                            items(visibleItems, key = { it.uri.toString() }) { item ->
                                val isHidden = uiState.hiddenUris.contains(item.uri.toString())
                                MediaItemCard(
                                    item = item,
                                    isHidden = isHidden,
                                    onClick = {
                                        when (item) {
                                            is MediaItem.Folder -> onFolderClick(item)
                                            is MediaItem.Video -> {
                                                val allVideos = visibleItems.filterIsInstance<MediaItem.Video>()
                                                val index = allVideos.indexOf(item).coerceAtLeast(0)
                                                onVideoClick(item, index, allVideos)
                                            }
                                        }
                                    },
                                    onHideToggle = { onToggleHideItem(item.uri) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackNavigationCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick).clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ArrowBack, "Go Back", modifier = Modifier.size(64.dp), tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Back", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

@Composable
fun MediaItemCard(item: MediaItem, isHidden: Boolean, onClick: () -> Unit, onHideToggle: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale, label = "shrink")

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).scale(animatedScale).alpha(if (isHidden) 0.3f else 1f).pointerInput(item.uri) {
            detectTapGestures(
                onTap = { onClick() },
                onPress = {
                    scale = 0.85f
                    val job = coroutineScope.launch { delay(3000); onHideToggle() }
                    tryAwaitRelease()
                    job.cancel()
                    scale = 1f
                }
            )
        }.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val imageRequest = remember(item.uri, item.thumbnailUri) {
                val isExtracting = item.thumbnailUri == null || item.thumbnailUri == item.uri
                ImageRequest.Builder(context).data(item.thumbnailUri ?: item.uri).apply { if (isExtracting) videoFrameMillis(60_000) }.crossfade(true).build()
            }

            AsyncImage(model = imageRequest, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(8.dp)) {
                Text(text = item.name.substringBeforeLast("."), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                when (item) {
                    is MediaItem.Folder -> Icon(Icons.Default.Folder, "Folder", tint = Color.Yellow)
                    is MediaItem.Video -> Icon(Icons.Default.PlayArrow, "Play", tint = Color.White)
                }
            }
        }
    }
}