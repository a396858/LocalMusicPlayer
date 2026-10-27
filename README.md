# 本地音乐 LocalMusic

一个极简的安卓本地音乐播放器：**只播手机里的音乐，不联网、不申请多余权限、没有推荐没有广告**。

除了播放本身，整个 App 只有一个额外功能——**一个按键，按一下就是 2 小时定时关闭**。

---

## 功能

| 功能 | 说明 |
| --- | --- |
| 扫描本地音乐 | 读取系统媒体库（MediaStore），自动过滤铃声、提示音和短于 30 秒的音频 |
| 播放 | 点击列表任意一首开始播放，支持上一首 / 下一首 |
| 进度 | 可拖动进度条跳转 |
| **定时关闭** | 点击「定时关闭 · 2 小时」按钮，2 小时后自动暂停；再点一下取消，按钮上实时显示剩余时间 |
| 后台播放 | 通过 Media3 前台服务播放，锁屏、切到后台都不中断，通知栏自带播放控制 |

## 没有的东西

没有在线曲库、没有歌词、没有均衡器、没有歌单管理、没有下载、没有账号、没有网络请求。
清单里的音频文件在哪儿，它就播哪儿的。

---

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `READ_MEDIA_AUDIO`（Android 13+）/ `READ_EXTERNAL_STORAGE`（Android 12 及以下） | 读取本地音频文件 |
| `POST_NOTIFICATIONS`（Android 13+） | 显示通知栏播放控制（拒绝也能正常播放） |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放 |

**没有 `INTERNET` 权限**——它可以物理上无法联网。

---

## 环境要求

- Android Studio（Ladybug 或更新版本）
- JDK 17
- Android SDK Platform 34 + Build-Tools 34.0.0
- 手机 Android 8.0（API 26）及以上

## 自己编译

### 方式一：Android Studio

1. `File → Open`，选择本目录
2. 等待 Gradle Sync 完成
3. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. 产物位置：`app/build/outputs/apk/debug/app-debug.apk`

### 方式二：命令行

```bash
# 先写好自己的 SDK 路径到 local.properties
echo "sdk.dir=/your/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

装到手机：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

首次打开会请求「读取本地音乐」权限，允许后即可看到清单。

---

## 技术栈

- Kotlin 2.0.20 + Jetpack Compose（Material 3）
- Media3 ExoPlayer 1.4.1 + MediaSessionService
- AGP 8.5.2 / Gradle 8.7 / compileSdk 34 / minSdk 26
- 无数据库、无网络库、无第三方 SDK

## 目录结构

```
app/src/main/java/com/localmusic/player/
├── MainActivity.kt              # 权限申请 + 入口
├── data/
│   ├── Song.kt                  # 歌曲数据类
│   └── MusicScanner.kt          # MediaStore 扫描
├── playback/
│   ├── PlaybackService.kt       # 前台播放服务（后台播放 + 通知栏）
│   └── PlayerViewModel.kt       # 播放状态 + 定时关闭逻辑
└── ui/
    ├── Format.kt                # 时间格式化
    ├── PlayerScreen.kt          # 主界面
    └── theme/Theme.kt           # 主题色
```

## 二次开发提示

- **定时关闭时长**：改 `PlayerViewModel.SLEEP_DURATION_MS`（当前 `2 * 60 * 60 * 1000`）
- **扫描过滤规则**：改 `MusicScanner` 里的 `selection`（现在过滤了铃声和 30 秒以内的音频）
- **想加随机播放 / 单曲循环**：在 `PlayerViewModel` 里对 `ExoPlayer` 调用 `setShuffleModeEnabled` / `setRepeatMode`

## FAQ

**Q：为什么列表是空的？**
A：确认已授权「音乐和音频」权限；确认手机里确实有 mp3 / flac / m4a 等文件；部分机型需要重启一次 App 让媒体库刷新。

**Q：定时关闭到点了会怎样？**
A：暂停播放，不会杀掉 App，也不会关闭通知栏。想继续听点一下播放即可。

**Q：应用彻底关掉后定时还会生效吗？**
A：定时逻辑跟随界面进程。正常切后台、锁屏不受影响；如果从最近任务里划掉且当时正在播放，播放会继续，但定时会被取消。所以想准点停，别手动清后台。
