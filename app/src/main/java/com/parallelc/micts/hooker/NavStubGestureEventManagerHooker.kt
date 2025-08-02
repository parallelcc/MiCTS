package com.parallelc.micts.hooker

import android.content.Context
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_GESTURE_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Method

class NavStubGestureEventManagerHooker {
    companion object {
        private var getInstance: Method? = null

        fun hook(param: PackageLoadedParam) {
            val navStubGestureEventManager = param.classLoader.loadClass("com.miui.home.recents.gesture.NavStubGestureEventManager")
            getInstance = runCatching {
                param.classLoader.loadClass("com.miui.home.launcher.Application").getDeclaredMethod("getInstance")
            }.getOrNull()
            module!!.hook(navStubGestureEventManager.getDeclaredMethod("handleLongPressEvent"), HandleLongPressEventHooker::class.java)
        }
        @XposedHooker
        class HandleLongPressEventHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                    if (prefs.getBoolean(KEY_GESTURE_TRIGGER, DEFAULT_CONFIG[KEY_GESTURE_TRIGGER] as Boolean)) {
                        triggerCircleToSearch(
                            1,
                            getInstance?.invoke(null) as? Context,
                            prefs.getBoolean(
                                KEY_VIBRATE,
                                DEFAULT_CONFIG[KEY_VIBRATE] as Boolean
                            )
                        )
                        callback.returnAndSkip(null)
                    }
                }
            }
        }
    }
}