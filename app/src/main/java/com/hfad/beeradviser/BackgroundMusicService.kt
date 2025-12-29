package com.hfad.beeradviser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BackgroundMusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false // 標記 MediaPlayer 是否已準備好

    // 【新增】定義通知的 ID 和 頻道 ID
    companion object {
        private const val TAG = "MusicService"
        private const val NOTIFICATION_ID = 101 // 前景服務通知的 ID
        private const val CHANNEL_ID = "MusicPlaybackChannel"
        private const val CHANNEL_NAME = "背景音樂播放"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer?.isLooping = true // 循環播放
        mediaPlayer?.setVolume(0.5f, 0.5f) // 設定音量
        isPrepared = true // 準備好播放

        // 【新增】建立通知頻道 (僅適用於 Android O (API 26) 或更高版本)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        val action = intent?.action

        // 【修正點 1】處理前景服務的啟動
        // 必須在收到 startForegroundService 呼叫後的 5 秒內呼叫 startForeground()
        // 不論 action 是 PLAY, PAUSE 還是 STOP，只要服務被啟動/重新啟動，就呼叫 startForeground
        startForeground(NOTIFICATION_ID, buildNotification("音樂播放中"))

        when (action) {
            "PLAY" -> {
                if (isPrepared && mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                    // 【修正點 2】更新前景服務的通知內容
                    updateNotification("音樂播放中")
                    Log.d(TAG, "Music started.")
                }
            }

            "PAUSE" -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    // 【修正點 2】更新前景服務的通知內容
                    updateNotification("音樂暫停中")
                    Log.d(TAG, "Music paused.")
                }
            }

            "STOP" -> {
                // 這個 action 主要由 MainActivity.kt 中的 stopService() 觸發
                stopSelf() // 停止服務並銷毀
            }
        }

        // 使用 START_STICKY 確保即使系統殺死服務，也會在資源充足時重建它
        // 這對於音樂播放服務是常見的設置
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        // 【修正點 3】停止前景服務並移除通知
        // 呼叫 stopForeground(true) 清除通知
        stopForeground(true)

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }

    // 【新增】建立通知頻道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // 低重要性通知，不發出聲音
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // 【新增】建立通知 Builder
    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("背景音樂")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 替換為您的應用程式圖示
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 設定為正在進行中的通知
            .build()

    // 【新增】更新通知
    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}