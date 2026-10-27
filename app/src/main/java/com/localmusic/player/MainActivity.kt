package com.localmusic.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localmusic.player.playback.PlayerViewModel
import com.localmusic.player.ui.PlayerScreen
import com.localmusic.player.ui.theme.LocalMusicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LocalMusicTheme {
                val vm: PlayerViewModel = viewModel()

                val required = remember { requiredPermissions() }
                var granted by remember { mutableStateOf(hasAudioPermission(this)) }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    // 通知权限被拒绝不影响播放，只要拿到音频权限就够了
                    val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result[Manifest.permission.READ_MEDIA_AUDIO] ?: hasAudioPermission(this)
                    } else {
                        result[Manifest.permission.READ_EXTERNAL_STORAGE] ?: hasAudioPermission(this)
                    }
                    granted = audioGranted
                    if (audioGranted) vm.loadSongs()
                }

                LaunchedEffect(Unit) {
                    if (granted) vm.loadSongs() else launcher.launch(required)
                }

                PlayerScreen(
                    vm = vm,
                    permissionGranted = granted,
                    onRequestPermission = { launcher.launch(required) },
                )
            }
        }
    }

    private fun requiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
