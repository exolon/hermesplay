package com.example.hermesplay

import android.content.Context
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
    val prefs = context.getSharedPreferences("hermes_playback_prefs", Context.MODE_PRIVATE)

    val bingeQueue = remember(playlist, startIndex) {
        val endIndex = minOf(startIndex + 5, playlist.size)
        playlist.subList(startIndex, endIndex)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItems = bingeQueue.map { ExoMediaItem.fromUri(it.uri) }
            setMediaItems(mediaItems)

            val firstUri = bingeQueue.first().uri.toString()
            val savedPosition = prefs.getLong(firstUri, 0L)
            if (savedPosition > 0L) seekTo(0, savedPosition)

            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentWindowIndex = exoPlayer.currentMediaItemIndex
            if (currentWindowIndex in bingeQueue.indices) {
                val activeUri = bingeQueue[currentWindowIndex].uri.toString()
                val currentPos = exoPlayer.currentPosition
                val totalDuration = exoPlayer.duration

                val positionToSave = if (totalDuration > 0 && currentPos >= totalDuration - 5000) 0L else currentPos
                prefs.edit().putLong(activeUri, positionToSave).apply()
            }
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                keepScreenOn = true
                // THE FIX: Force the view to fill the space and wake up immediately
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                onResume()
            }
        }
    )
}