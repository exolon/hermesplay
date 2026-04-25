package com.example.hermesplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    uiState: UiState,
    onFolderClick: (MediaItem.Folder) -> Unit,
    onVideoClick: (MediaItem.Video) -> Unit,
    onNavigateUp: () -> Unit,
    onJumpToRoot: () -> Unit,
    onRefresh: () -> Unit,
    onRescanAll: () -> Unit // NEW Callback
) {
    var gridSize by remember { mutableFloatStateOf(180f) }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("HermesPlay") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp).width(180.dp)
                    ) {
                        Text("Size", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = gridSize,
                            onValueChange = { gridSize = it },
                            valueRange = 120f..400f,
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.DarkGray)
                        )
                    }

                    if (uiState is UiState.Success) {
                        if (uiState.isRoot) {
                            // Show Global Rescan if at the root
                            IconButton(onClick = onRescanAll) {
                                Icon(Icons.Default.Sync, contentDescription = "Rescan All Folders")
                            }
                        } else {
                            // Show Folder Refresh and Home if in a sub-folder
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Folder")
                            }
                            IconButton(onClick = onJumpToRoot) {
                                Icon(Icons.Default.Home, contentDescription = "Go to Root")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                is UiState.Error -> Text(text = uiState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    if (uiState.items.isEmpty() && uiState.isRoot) {
                        Text("This folder is empty!", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = gridSize.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!uiState.isRoot) { item { BackNavigationCard(onClick = onNavigateUp) } }

                            items(uiState.items, key = { it.uri.toString() }) { item ->
                                MediaItemCard(
                                    item = item,
                                    onClick = {
                                        when (item) {
                                            is MediaItem.Folder -> onFolderClick(item)
                                            is MediaItem.Video -> onVideoClick(item)
                                        }
                                    }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", modifier = Modifier.size(64.dp), tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Back", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

@Composable
fun MediaItemCard(item: MediaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick).clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current

            // SMART LOADING: Only time-shift if we are forced to extract a video frame
            val imageRequest = remember(item.uri, item.thumbnailUri) {
                val isExtractingFromVideo = item.thumbnailUri == null || item.thumbnailUri == item.uri

                ImageRequest.Builder(context)
                    .data(item.thumbnailUri ?: item.uri)
                    .apply {
                        if (isExtractingFromVideo) {
                            videoFrameMillis(60_000)
                        }
                    }
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(8.dp)) {
                Text(text = item.name.substringBeforeLast("."), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                when (item) {
                    is MediaItem.Folder -> Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color.Yellow)
                    is MediaItem.Video -> Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
            }
        }
    }
}