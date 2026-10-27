package com.localmusic.player.playback

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 定时关闭计时器。
 *
 * 放在进程级的单例里，而不是 ViewModel：这样按返回键退出界面、
 * 甚至 Activity 被回收，倒计时都不会丢，到点照样暂停播放。
 * 用 [SystemClock.elapsedRealtime] 计时，不会因为系统休眠漂移。
 */
object SleepTimer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _remainingMs = MutableStateFlow(0L)

    /** 剩余毫秒；0 表示没有开启 */
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    /** 到点后要做的事，由播放服务注册 */
    @Volatile
    var onFinished: (() -> Unit)? = null

    val isRunning: Boolean
        get() = _remainingMs.value > 0L

    fun start(totalMs: Long) {
        job?.cancel()
        _remainingMs.value = totalMs
        job = scope.launch {
            val deadline = SystemClock.elapsedRealtime() + totalMs
            while (isActive) {
                val left = deadline - SystemClock.elapsedRealtime()
                if (left <= 0L) break
                _remainingMs.value = left
                delay(500L)
            }
            _remainingMs.value = 0L
            try {
                onFinished?.invoke()
            } catch (_: Exception) {
                // 服务已销毁等情况，忽略
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = 0L
    }

    fun toggle(totalMs: Long) {
        if (isRunning) cancel() else start(totalMs)
    }
}
