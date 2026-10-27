package com.localmusic.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localmusic.player.data.Song
import com.localmusic.player.playback.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: PlayerViewModel,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val current by vm.currentSong.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val positionMs by vm.positionMs.collectAsStateWithLifecycle()
    val durationMs by vm.durationMs.collectAsStateWithLifecycle()
    val sleepRemaining by vm.sleepRemainingMs.collectAsStateWithLifecycle()

    val background = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "本地音乐",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                !permissionGranted -> PermissionState(onRequestPermission)
                isLoading -> LoadingState()
                songs.isEmpty() -> EmptyState()
                else -> {
                    NowPlaying(
                        song = current,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        sleepRemainingMs = sleepRemaining,
                        onTogglePlay = vm::togglePlay,
                        onPrevious = vm::previous,
                        onNext = vm::next,
                        onSeek = vm::seekTo,
                        onToggleSleepTimer = vm::toggleSleepTimer,
                    )

                    Text(
                        text = "全部歌曲 · ${songs.size} 首",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                    )

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(songs, key = { it.id }) { song ->
                            SongRow(
                                song = song,
                                isCurrent = song.id == current?.id,
                                isPlaying = isPlaying,
                                onClick = { vm.playSong(song) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlaying(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    sleepRemainingMs: Long,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleSleepTimer: () -> Unit,
) {
    val hasSong = song != null
    val timerOn = sleepRemainingMs > 0L

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(hasSong = hasSong, isPlaying = isPlaying)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "还没有开始播放",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song?.artist ?: "从下面选一首歌",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SeekBar(
            enabled = hasSong,
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = onSeek,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = hasSong) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "上一首",
                    modifier = Modifier.size(32.dp),
                )
            }

            FilledIconButton(
                onClick = onTogglePlay,
                enabled = hasSong,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(34.dp),
                )
            }

            IconButton(onClick = onNext, enabled = hasSong) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "下一首",
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 唯一的功能按钮：按一下就是 2 小时定时关闭
        Surface(
            onClick = onToggleSleepTimer,
            shape = RoundedCornerShape(50),
            color = if (timerOn) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (timerOn) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Bedtime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (timerOn) {
                        "还有 ${formatCountdown(sleepRemainingMs)} 自动暂停"
                    } else {
                        "定时关闭 · 2 小时"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (timerOn) "再点一下可取消定时" else "点一下，2 小时后自动暂停播放",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AlbumArt(hasSong: Boolean, isPlaying: Boolean) {
    val colors = if (hasSong && isPlaying) {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    } else {
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = if (hasSong && isPlaying) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun SeekBar(
    enabled: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val max = durationMs.coerceAtLeast(1L).toFloat()
    val value = if (dragging) dragValue else positionMs.coerceIn(0L, durationMs).toFloat()

    Column {
        Slider(
            value = value,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeek(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..max,
            enabled = enabled,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(if (dragging) dragValue.toLong() else positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when {
                isCurrent && isPlaying -> Icons.Rounded.GraphicEq
                else -> Icons.Rounded.MusicNote
            },
            contentDescription = null,
            tint = if (isCurrent) accent else muted,
            modifier = Modifier.size(24.dp),
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(song.artist)
                    if (song.album.isNotBlank()) {
                        append(" · ")
                        append(song.album)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
    }
}

@Composable
private fun PermissionState(onRequestPermission: () -> Unit) {
    CenteredMessage(
        icon = { Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary) },
        title = "需要读取本地音乐",
        body = "这个播放器只读取手机里的音频文件，不联网、不上传任何内容。",
        action = {
            FilledTonalButton(onClick = onRequestPermission) {
                Text("授予权限")
            }
        },
    )
}

@Composable
private fun EmptyState() {
    CenteredMessage(
        icon = { Icon(Icons.Rounded.MusicNote, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        title = "没有找到音乐",
        body = "把 mp3、flac、m4a 等音频文件放进手机存储，再重新打开应用即可。",
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}
