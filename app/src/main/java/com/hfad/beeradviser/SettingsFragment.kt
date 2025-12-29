package com.hfad.beeradviser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

class SettingsFragment : DialogFragment() {

    interface SettingsChangeListener {
        fun onSoundSettingChanged(enabled: Boolean)
        fun onMusicSettingChanged(enabled: Boolean)
        fun onApplyBlurEffect(apply: Boolean)
    }

    private var settingsChangeListener: SettingsChangeListener? = null

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var switchSound: Switch
    private lateinit var switchMusic: Switch
    private lateinit var switchNotification: Switch
    private lateinit var buttonCloseSettings: Button

    // 通知相關常量
    private val NOTIFICATION_CHANNEL_ID = "game_notifications_channel"
    private val NOTIFICATION_CHANNEL_NAME = "遊戲通知"

    // 權限請求啟動器 (僅在 Android 13+ 需要)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 權限已授予，可以嘗試發送通知或啟用頻道
            Toast.makeText(requireContext(), "通知權限已授予", Toast.LENGTH_SHORT).show()
            // 如果用戶授予了權限，將 switchNotification 設置為 true
            // 注意：這裡只設置 UI，實際發送通知時仍需檢查
            switchNotification.isChecked = true
            // 通知 manager 啟用頻道（如果之前是禁用的）
            setNotificationChannelEnabled(true)
        } else {
            // 權限被拒絕
            Toast.makeText(requireContext(), "通知權限被拒絕，可能無法收到通知", Toast.LENGTH_LONG)
                .show()
            switchNotification.isChecked = false // 用戶拒絕，將開關設為關閉
            setNotificationChannelEnabled(false) // 禁用通知頻道
            showPermissionDeniedDialog() // 提示用戶去系統設定中開啟
        }
    }


    companion object {
        const val PREFS_NAME = "game_settings"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_MUSIC_ENABLED = "music_enabled"
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingsChangeListener) {
            settingsChangeListener = context
        } else {
            throw RuntimeException("$context must implement SettingsChangeListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_GameSettingsDialog)
        createNotificationChannel() // 創建通知頻道 (只需要做一次)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        switchSound = view.findViewById(R.id.switchSound)
        switchMusic = view.findViewById(R.id.switchMusic)
        switchNotification = view.findViewById(R.id.switchNotification)
        buttonCloseSettings = view.findViewById(R.id.buttonCloseSettings)

        // 載入並設定初始狀態
        switchSound.isChecked = sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true)
        switchMusic.isChecked = sharedPreferences.getBoolean(KEY_MUSIC_ENABLED, true)
        // 通知開關的初始狀態需要根據系統的通知設定來判斷
        switchNotification.isChecked = areNotificationsEnabled()


        // 設定監聽器
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_SOUND_ENABLED, isChecked).apply()
            settingsChangeListener?.onSoundSettingChanged(isChecked) // 通知 Activity
        }

        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_MUSIC_ENABLED, isChecked).apply()
            settingsChangeListener?.onMusicSettingChanged(isChecked) // 通知 Activity
        }

        // 通知開關的處理邏輯
        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 如果用戶嘗試開啟通知
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // 權限已授予，直接啟用頻道
                        setNotificationChannelEnabled(true)
                        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, true).apply()
                        Toast.makeText(requireContext(), "已啟用通知", Toast.LENGTH_SHORT).show()
                    } else {
                        // 權限未授予，請求權限
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        // 在請求權限後，將 switch 的狀態設為 OFF，待用戶操作後再根據結果更新
                        // 這會避免在用戶還沒回應權限請求前，UI 顯示為 ON 的暫態。
                        switchNotification.isChecked = false
                    }
                } else {
                    // 低於 Android 13
                    // 在此版本，用戶可能在系統設定中禁用了通知
                    // 檢查系統通知是否已禁用
                    if (!areNotificationsEnabled()) {
                        // 如果系統通知已禁用，引導用戶去設定，而不是直接說啟用
                        Toast.makeText(
                            requireContext(),
                            "請在系統設定中啟用通知",
                            Toast.LENGTH_LONG
                        ).show()
                        showPermissionDeniedDialog() // 重新使用這個 Dialog
                        switchNotification.isChecked = false // UI 保持為 OFF
                    } else {
                        // 系統通知是開啟的，應用程式內部啟用頻道
                        setNotificationChannelEnabled(true)
                        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, true).apply()
                        Toast.makeText(requireContext(), "已啟用通知", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // 如果用戶嘗試關閉通知
                sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, false).apply()
                setNotificationChannelEnabled(false) // 禁用通知頻道
                Toast.makeText(requireContext(), "已禁用通知", Toast.LENGTH_SHORT).show()
            }
        }


        buttonCloseSettings.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settingsChangeListener?.onApplyBlurEffect(true)
    }

    override fun onStop() {
        super.onStop()
        settingsChangeListener?.onApplyBlurEffect(false)
    }

    override fun onResume() {
        super.onResume()
        updateNotificationSwitchState()
    }

    override fun onDetach() {
        super.onDetach()
        settingsChangeListener = null
    }

    // ========== 通知相關輔助方法 ==========

    // 創建通知頻道 (Android 8.0 Oreo 及以上需要)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = NOTIFICATION_CHANNEL_NAME
            val descriptionText = "遊戲相關的通知，例如計時器完成、活動提醒等。"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 檢查應用程式的通知是否在系統層面被禁用
    private fun areNotificationsEnabled(): Boolean {
        val notificationManagerCompat = NotificationManagerCompat.from(requireContext())

        if (!notificationManagerCompat.areNotificationsEnabled()) {
            return false
        }

        // Android 8.0+ 再檢查「頻道」是否被關閉
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }

    // 啟用/禁用通知頻道 (模擬應用程式內部控制)
    private fun setNotificationChannelEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (channel != null) {
                channel.importance =
                    if (enabled) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_NONE
                notificationManager.createNotificationChannel(channel) // 重新創建以應用變更
            }
        }
        // 對於低於 Android 8.0 的版本，通常只能在發送通知前檢查 SharedPreferences
        // 或者引導用戶到應用程式的系統設定頁面進行更全面的控制
    }

    // 更新通知開關的 UI 狀態以匹配系統設定
    private fun updateNotificationSwitchState() {
        switchNotification.isChecked = areNotificationsEnabled()
        // 同步內部儲存狀態
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, areNotificationsEnabled())
            .apply()
    }

    // 提示用戶去系統設定中開啟通知
    private fun showPermissionDeniedDialog() {
        // 彈出一個 Dialog 提示用戶去設定中開啟權限
        // 這裡可以是一個 AlertDialog 或一個自定義的 DialogFragment
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("需要通知權限")
            .setMessage("為了接收遊戲通知，請在系統設定中開啟通知權限。")
            .setPositiveButton("前往設定") { dialog, which ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent) // 跳轉到應用程式的系統設定頁面
            }
            .setNegativeButton("取消") { dialog, which ->
                // 用戶選擇取消，保持開關為禁用狀態
            }
            .show()
    }
}