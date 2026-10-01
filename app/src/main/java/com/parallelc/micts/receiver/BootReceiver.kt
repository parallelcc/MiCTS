package com.parallelc.micts.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.util.ShizukuHelper

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MiCTS_BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            return
        }

        val defaultVal = (AppConfig.DEFAULT_CONFIG[AppConfig.KEY_HOME_TRIGGER] as? Boolean) ?: false
        var homeTriggerEnabled = false

        runCatching {
            val deContext = context.createDeviceProtectedStorageContext()
            val dePrefs = deContext.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)

            val userManager = context.getSystemService(UserManager::class.java)
            if (!dePrefs.contains(AppConfig.KEY_HOME_TRIGGER) && userManager?.isUserUnlocked == true) {
                val cePrefs = context.getSharedPreferences(AppConfig.CONFIG_NAME, Context.MODE_PRIVATE)
                val migratedVal = cePrefs.getBoolean(AppConfig.KEY_HOME_TRIGGER, defaultVal)
                dePrefs.edit().putBoolean(AppConfig.KEY_HOME_TRIGGER, migratedVal).commit()
            }

            homeTriggerEnabled = dePrefs.getBoolean(AppConfig.KEY_HOME_TRIGGER, defaultVal)
        }.onFailure { e ->
            Log.e(TAG, "Failed reading home trigger preference on $action", e)
        }

        if (!homeTriggerEnabled) {
            return
        }

        if (ShizukuHelper.hasWriteSecureSettings(context)) {
            val result = ShizukuHelper.applyAssistantSetting(context, enable = true)
            if (result.isSuccess) {
                Log.i(TAG, "Successfully restored assistant setting on $action")
            } else {
                Log.e(TAG, "Failed to restore assistant setting on $action", result.exceptionOrNull())
            }
        } else {
            Log.w(TAG, "Cannot restore assistant setting on $action: WRITE_SECURE_SETTINGS not granted")
        }
    }
}
