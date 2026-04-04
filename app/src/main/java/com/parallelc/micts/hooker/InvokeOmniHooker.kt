package com.parallelc.micts.hooker

import android.content.Context
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_GESTURE_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.Chain

class InvokeOmniHooker : Hooker {
    override fun intercept(chain: Chain): Any? {
        val prefs = module!!.getRemotePreferences(CONFIG_NAME)
        return if (prefs.getBoolean(KEY_GESTURE_TRIGGER, DEFAULT_CONFIG[KEY_GESTURE_TRIGGER] as Boolean)) {
            triggerCircleToSearch(
                chain.args[2] as Int,
                chain.args[0] as Context?,
                prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
            )
        } else {
            chain.proceed()
        }
    }
}