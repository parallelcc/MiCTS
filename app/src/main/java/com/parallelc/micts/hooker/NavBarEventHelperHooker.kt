package com.parallelc.micts.hooker

import android.content.Context
import android.view.MotionEvent
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
import java.lang.reflect.Field

class NavBarEventHelperHooker {
    companion object {
        private lateinit var mContext: Field

        fun hook(param: PackageLoadedParam) {
            val navStubGestureEventManager = param.classLoader.loadClass("com.miui.home.recents.cts.NavBarEventHelper")
            mContext = navStubGestureEventManager.getDeclaredField("mContext")
            mContext.isAccessible = true
            module!!.hook(navStubGestureEventManager.getDeclaredMethod("onLongPress", MotionEvent::class.java), OnLongPressHooker::class.java)
        }

        @XposedHooker
        class OnLongPressHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                    if (prefs.getBoolean(KEY_GESTURE_TRIGGER, DEFAULT_CONFIG[KEY_GESTURE_TRIGGER] as Boolean)) {
                        val context = runCatching { mContext.get(callback.thisObject) as? Context }.getOrNull()
                        triggerCircleToSearch(
                            1,
                            context,
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