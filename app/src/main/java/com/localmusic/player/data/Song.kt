package com.localmusic.player.data

import android.net.Uri

/**
 * 一首本地歌曲的轻量描述。
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
)
