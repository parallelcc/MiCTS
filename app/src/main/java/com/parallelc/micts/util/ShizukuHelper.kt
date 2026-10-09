package com.parallelc.micts.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.parallelc.micts.ui.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    const val GOOGLE_ASSISTANT_SERVICE =
        "$GOOGLE_PACKAGE/com.google.android.voiceinteraction.GsaVoiceInteractionService"

    sealed class Status {
        data object Active : Status()
        data object ResetAfterBoot : Status()
        data object Inactive : Status()
        data object ShizukuNotRunning : Status()
        data object PermissionNeeded : Status()
    }

    fun getAssistantComponent(context: Context): String =
        ComponentName(context, MainActivity::class.java).flattenToString()

    fun isShizukuRunning(): Boolean {
        return runCatching {
            Shizuku.pingBinder()
        }.getOrDefault(false)
    }

    fun hasShizukuPermission(): Boolean {
        return if (!isShizukuRunning()) {
            false
        } else {
            runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        }
    }

    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun isHomeTriggerActive(context: Context): Boolean {
        val currentAssistant = runCatching {
            Settings.Secure.getString(context.contentResolver, "assistant")
        }.getOrNull() ?: return false
        val target = ComponentName(context, MainActivity::class.java)
        return ComponentName.unflattenFromString(currentAssistant) == target
    }

    fun isGoogleDefaultAssistant(context: Context): Boolean {
        val resolver = context.contentResolver
        val voiceService = runCatching {
            Settings.Secure.getString(resolver, "voice_interaction_service")
        }.getOrNull()
        if (voiceService?.contains(GOOGLE_PACKAGE) == true) return true

        val assistant = runCatching {
            Settings.Secure.getString(resolver, "assistant")
        }.getOrNull()
        if (assistant?.contains(GOOGLE_PACKAGE) == true) return true

        if (isHomeTriggerActive(context)) return true

        return false
    }

    fun restoreVoiceInteractionServiceIfNeeded(context: Context) {
        val currentVoiceService = runCatching {
            Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
        }.getOrNull()
        if (currentVoiceService.isNullOrEmpty() || !currentVoiceService.contains(GOOGLE_PACKAGE)) {
            runCatching {
                Settings.Secure.putString(
                    context.contentResolver,
                    "voice_interaction_service",
                    GOOGLE_ASSISTANT_SERVICE
                )
            }
        }
    }

    fun getStatus(context: Context, isEnabledPref: Boolean): Status {
        if (isHomeTriggerActive(context)) {
            return Status.Active
        }
        if (isEnabledPref) {
            return if (hasWriteSecureSettings(context)) {
                Status.ResetAfterBoot
            } else if (!isShizukuRunning()) {
                Status.ShizukuNotRunning
            } else if (!hasShizukuPermission()) {
                Status.PermissionNeeded
            } else {
                Status.ResetAfterBoot
            }
        }
        return Status.Inactive
    }

    fun requestShizukuPermission(requestCode: Int = 1001, onResult: (Boolean) -> Unit) {
        if (!isShizukuRunning()) {
            onResult(false)
            return
        }
        if (hasShizukuPermission()) {
            onResult(true)
            return
        }

        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(reqCode: Int, grantResult: Int) {
                if (reqCode == requestCode) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Shizuku.removeRequestPermissionResultListener(listener)
            Log.e(TAG, "Failed to request Shizuku permission", e)
            onResult(false)
        }
    }

    private val newProcessMethod by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    private fun execRemoteProcess(cmd: Array<String>): Process {
        return newProcessMethod.invoke(null, cmd, null, null) as Process
    }

    suspend fun grantWriteSecureSettings(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (hasWriteSecureSettings(context)) {
            return@withContext Result.success(Unit)
        }
        if (!isShizukuRunning() || !hasShizukuPermission()) {
            return@withContext Result.failure(IllegalStateException("Shizuku is not available or permission denied"))
        }

        runCatching {
            val cmd = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
            val process = execRemoteProcess(arrayOf("sh", "-c", cmd))
            val errorMsg: String
            val exitCode: Int
            try {
                errorMsg = process.errorStream.bufferedReader().use { it.readText() }
                exitCode = process.waitFor()
            } finally {
                process.destroy()
            }
            if (exitCode != 0) {
                if (errorMsg.contains("SecurityException") || errorMsg.contains("Permission Denial")) {
                    throw SecurityException("XIAOMI_SECURITY_SETTING_REQUIRED: $errorMsg")
                }
                throw RuntimeException("Grant failed (code $exitCode): $errorMsg")
            }
        }
    }

    fun applyAssistantSetting(context: Context, enable: Boolean): Result<Unit> {
        return runCatching {
            val resolver = context.contentResolver
            if (enable) {
                if (!isGoogleDefaultAssistant(context)) {
                    throw IllegalStateException("GOOGLE_NOT_DEFAULT_ASSISTANT")
                }
                val component = getAssistantComponent(context)
                val ok1 = Settings.Secure.putString(resolver, "assistant", component)
                val ok2 = Settings.Secure.putString(resolver, "voice_interaction_service", GOOGLE_ASSISTANT_SERVICE)
                if (!ok1 || !ok2 || !isHomeTriggerActive(context)) {
                    throw SecurityException("XIAOMI_SECURITY_SETTING_REQUIRED")
                }
            } else {
                val ok1 = Settings.Secure.putString(resolver, "assistant", GOOGLE_ASSISTANT_SERVICE)
                val ok2 = Settings.Secure.putString(resolver, "voice_interaction_service", GOOGLE_ASSISTANT_SERVICE)
                if (!ok1 || !ok2) {
                    throw SecurityException("XIAOMI_SECURITY_SETTING_REQUIRED")
                }
            }
        }
    }
}
