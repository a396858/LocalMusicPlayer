package com.localmusic.player.playback

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.localmusic.player.data.MusicScanner
import com.localmusic.player.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * 播放状态与定时关闭逻辑。
 * 与 [PlaybackService] 里的播放器通过 MediaController 通信，
 * 所以退到后台、锁屏，播放都不受影响。
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        /** 定时关闭：按一下就是 2 小时 */
        const val SLEEP_DURATION_MS = 2L * 60L * 60L * 1000L
    }

    private var controller: MediaController? = null
    private var tickJob: Job? = null
    private var sleepJob: Job? = null
    private var pendingSong: Song? = null

    private val mainExecutor = Executor { it.run() }

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** 定时关闭剩余毫秒；0 表示没有开启 */
    private val _sleepRemainingMs = MutableStateFlow(0L)
    val sleepRemainingMs: StateFlow<Long> = _sleepRemainingMs.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentSong.value = mediaItem?.let { item ->
                _songs.value.firstOrNull { it.id.toString() == item.mediaId }
            }
            _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _positionMs.value = 0L
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val c = controller ?: return
            _durationMs.value = c.duration.coerceAtLeast(0L)
            if (playbackState == Player.STATE_ENDED) {
                _positionMs.value = 0L
            }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()

        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                syncFromController()
                startTicker()
                pendingSong?.let { song ->
                    pendingSong = null
                    playSong(song)
                }
            } catch (_: Exception) {
                // 服务连接失败时界面仍可展示歌曲列表
            }
        }, mainExecutor)
    }

    private fun syncFromController() {
        val c = controller ?: return
        _isPlaying.value = c.isPlaying
        _durationMs.value = c.duration.coerceAtLeast(0L)
        _positionMs.value = c.currentPosition.coerceAtLeast(0L)
        _currentSong.value = c.currentMediaItem?.let { item ->
            _songs.value.firstOrNull { it.id.toString() == item.mediaId }
        }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                    val duration = c.duration
                    if (duration > 0) _durationMs.value = duration
                }
                delay(500L)
            }
        }
    }

    /** 扫描本地音乐（需要先拿到读取音频的权限） */
    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = MusicScanner.scan(getApplication())
            _songs.value = list
            _isLoading.value = false
            syncFromController()
        }
    }

    fun playSong(song: Song) {
        val c = controller
        if (c == null) {
            pendingSong = song
            return
        }
        val list = _songs.value
        val index = list.indexOfFirst { it.id == song.id }
        if (index < 0) return

        val sameQueue = c.mediaItemCount == list.size &&
            c.getMediaItemAt(index).mediaId == song.id.toString()

        if (sameQueue) {
            c.seekTo(index, 0L)
        } else {
            c.setMediaItems(list.map { it.toMediaItem() }, index, 0L)
        }
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE || c.mediaItemCount == 0) return
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    /**
     * 一次点击：开启 2 小时定时关闭；再点一次：取消。
     */
    fun toggleSleepTimer() {
        if (_sleepRemainingMs.value > 0L) cancelSleepTimer() else startSleepTimer(SLEEP_DURATION_MS)
    }

    fun startSleepTimer(totalMs: Long) {
        sleepJob?.cancel()
        _sleepRemainingMs.value = totalMs
        sleepJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0L) {
                delay(1_000L)
                remaining = (remaining - 1_000L).coerceAtLeast(0L)
                _sleepRemainingMs.value = remaining
            }
            controller?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepRemainingMs.value = 0L
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

    override fun onCleared() {
        tickJob?.cancel()
        sleepJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
