package com.localmusic.player.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过 MediaStore 扫描设备上的本地音乐。
 * 不联网、不上传，只读取系统媒体库索引。
 */
object MusicScanner {

    private const val MIN_DURATION_MS = 30_000L

    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )

        // 只取真正的音乐：排除铃声/提示音，并过滤掉过短的音频
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 " +
            "AND ${MediaStore.Audio.Media.DURATION} > $MIN_DURATION_MS"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = ArrayList<Song>()
        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val title = cursor.getString(titleIndex)
                        ?.takeIf { it.isNotBlank() } ?: "未知歌曲"
                    val artist = cursor.getString(artistIndex)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING } ?: "未知艺术家"
                    val album = cursor.getString(albumIndex)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING } ?: ""
                    val duration = cursor.getLong(durationIndex)

                    songs += Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = ContentUris.withAppendedId(collection, id),
                    )
                }
            }
        } catch (_: Exception) {
            // 没有权限或媒体库不可用时，返回空列表，由界面提示用户
        }
        songs
    }
}
