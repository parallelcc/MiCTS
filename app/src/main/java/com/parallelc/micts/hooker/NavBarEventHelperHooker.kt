package com.parallelc.micts.hooker

import android.content.Context
import android.view.MotionEvent
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_GESTURE_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Field

class NavBarEventHelperHooker {
    companion object {
        private lateinit var mContext: Field

        fun hook(param: PackageReadyParam) {
            val navStubGestureEventManager = param.classLoader.loadClass("com.miui.home.recents.cts.NavBarEventHelper")
            mContext = navStubGestureEventManager.getDeclaredField("mContext").also { it.isAccessible = true }
            module!!.hook(navStubGestureEventManager.getDeclaredMethod("onLongPress", MotionEvent::class.java)).intercept(OnLongPressHooker())
        }

        class OnLongPressHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                if (prefs.getBoolean(KEY_GESTURE_TRIGGER, DEFAULT_CONFIG[KEY_GESTURE_TRIGGER] as Boolean)) {
                    val context = runCatching { mContext.get(chain.thisObject) as? Context }.getOrNull()
                    triggerCircleToSearch(
                        1,
                        context,
                        prefs.getBoolean(
                            KEY_VIBRATE,
                            DEFAULT_CONFIG[KEY_VIBRATE] as Boolean
                        )
                    )
                    return null
                }
                return chain.proceed()
            }
        }
    }
}