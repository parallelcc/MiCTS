package com.parallelc.micts.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.RecognitionService
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig.CONFIG_NAME
import com.parallelc.micts.config.AppConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.AppConfig.KEY_ASYNC_TRIGGER
import com.parallelc.micts.config.AppConfig.KEY_DEFAULT_DELAY
import com.parallelc.micts.config.AppConfig.KEY_TILE_DELAY
import com.parallelc.micts.config.AppConfig.KEY_VIBRATE
import com.parallelc.micts.module
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lsposed.hiddenapibypass.HiddenApiBypass

const val LOG_TAG = BuildConfig.APP_NAME

@SuppressLint("PrivateApi")
fun triggerCircleToSearch(entryPoint: Int, context: Context?, vibrate: Boolean): Boolean {
    val result =  runCatching {
        val bundle = Bundle()
        if (BuildConfig.APP_NAME == "MiCTS") {
            bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime())
            bundle.putInt("omni.entry_point", entryPoint)
            bundle.putBoolean("micts_trigger", true)
        }
        val iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val vis = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "voiceinteraction")
        val vims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, vis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7, "hyperOS_home") as Boolean
        } else {
            HiddenApiBypass.invoke(iVimsClass, vims, "showSessionFromSession", null, bundle, 7) as Boolean
        }
    }.onFailure { e ->
        val errMsg = "triggerCircleToSearch invoke omni failed: " + e.stackTraceToString()
        module?.log(Log.ERROR, LOG_TAG, errMsg) ?: Log.e(LOG_TAG, errMsg)
    }.getOrDefault(false)
    if (result && vibrate && context != null) {
        runCatching {
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).run {
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setFlags(128)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), attr)
                } else {
                    vibrate(longArrayOf(0, 1, 75, 76), -1, attr)
                }
            }
        }.onFailure { e ->
            val errMsg = "triggerCircleToSearch vibrate failed: " + e.stackTraceToString()
            module?.log(Log.ERROR, LOG_TAG, errMsg) ?: Log.e(LOG_TAG, errMsg)
        }
    }
    return result
}

/**
 * Some ROMs (e.g. ColorOS 16) keep the assistant role on Google but leave voice_interaction_service
 * empty (it is cleared again on reboot), so no VoiceInteractionService is running and
 * showSessionFromSession always returns false. Restore it from the assistant setting.
 * Requires WRITE_SECURE_SETTINGS granted via adb.
 * @return true if the setting was rewritten and the service needs time to bind
 */
fun restoreVoiceInteractionService(context: Context): Boolean {
    return runCatching {
        val resolver = context.contentResolver
        if (!Settings.Secure.getString(resolver, "voice_interaction_service").isNullOrEmpty()) return false
        val assistant = Settings.Secure.getString(resolver, "assistant")
        val component = assistant?.let { ComponentName.unflattenFromString(it) } ?: return false
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(LOG_TAG, "voice_interaction_service is empty, run: adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS} " +
                "(if denied, reinstall with: adb install -r -g <apk>)")
            return false
        }
        Settings.Secure.putString(resolver, "voice_interaction_service", assistant)
        if (Settings.Secure.getString(resolver, "voice_recognition_service").isNullOrEmpty()) {
            context.packageManager.queryIntentServices(
                Intent(RecognitionService.SERVICE_INTERFACE).setPackage(component.packageName),
                PackageManager.MATCH_DISABLED_COMPONENTS
            ).firstOrNull()?.serviceInfo?.let {
                Settings.Secure.putString(resolver, "voice_recognition_service", ComponentName(it.packageName, it.name).flattenToString())
            }
        }
        Log.i(LOG_TAG, "restored voice_interaction_service to $assistant")
        true
    }.onFailure { e ->
        Log.e(LOG_TAG, "restoreVoiceInteractionService failed: " + e.stackTraceToString())
    }.getOrDefault(false)
}

fun isVoiceInteractionServiceMissing(context: Context): Boolean {
    return runCatching {
        val resolver = context.contentResolver
        Settings.Secure.getString(resolver, "voice_interaction_service").isNullOrEmpty() &&
            !Settings.Secure.getString(resolver, "assistant").isNullOrEmpty()
    }.getOrDefault(false)
}

class MainActivity : ComponentActivity() {
    suspend fun delayAndTrigger(delayMs: Long, vibrate: Boolean) {
        val restored = restoreVoiceInteractionService(this)
        if (delayMs > 0) {
            delay(delayMs)
        }
        var result = triggerCircleToSearch(1, this, vibrate)
        // The service binds asynchronously after the setting changes, retry until it is ready
        var retries = if (restored) 20 else 0
        while (!result && retries-- > 0) {
            delay(100)
            result = triggerCircleToSearch(1, this, vibrate)
        }
        if (!result) {
            if (isVoiceInteractionServiceMissing(this)) {
                Toast.makeText(this, getString(R.string.voice_interaction_service_missing), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.trigger_failed), Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val prefs = getSharedPreferences(CONFIG_NAME, MODE_PRIVATE)
        val key = if (intent.getBooleanExtra("from_tile", false)) KEY_TILE_DELAY else KEY_DEFAULT_DELAY
        val delayMs = prefs.getLong(key, DEFAULT_CONFIG[key] as Long)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)

        if (prefs.getBoolean(KEY_ASYNC_TRIGGER, DEFAULT_CONFIG[KEY_ASYNC_TRIGGER] as Boolean)) {
            lifecycleScope.launch {
                delayAndTrigger(delayMs, vibrate)
            }
        } else {
            runBlocking {
                delayAndTrigger(delayMs, vibrate)
            }
        }
    }
}
