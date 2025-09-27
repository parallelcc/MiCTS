package com.parallelc.micts.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
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
        module?.log(errMsg) ?: Log.e(LOG_TAG, errMsg)
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
            module?.log(errMsg) ?: Log.e(LOG_TAG, errMsg)
        }
    }
    return result
}

class MainActivity : ComponentActivity() {
    suspend fun delayAndTrigger(delayMs: Long, vibrate: Boolean) {
        if (delayMs > 0) {
            delay(delayMs)
        }
        if (!triggerCircleToSearch(1, this, vibrate)) {
            Toast.makeText(this, getString(R.string.trigger_failed), Toast.LENGTH_SHORT).show()
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
