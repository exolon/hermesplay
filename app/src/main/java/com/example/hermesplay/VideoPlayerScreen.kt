package com.example.hermesplay

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.hermesplay.models.MediaItem
import androidx.media3.common.MediaItem as ExoMediaItem

@Composable
fun VideoPlayerScreen(playlist: List<MediaItem.Video>, startIndex: Int) {
    val context = LocalContext.current

    // Grab up to 5 episodes starting from the one clicked
    val bingeQueue = remember(playlist, startIndex) {
        val endIndex = minOf(startIndex + 5, playlist.size)
        playlist.subList(startIndex, endIndex)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItems = bingeQueue.map { ExoMediaItem.fromUri(it.uri) }
            setMediaItems(mediaItems)

            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                keepScreenOn = true
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                onResume()
            }
        }
    )
}