package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Context
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_HOME_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Field

class LongPressHomeHooker {
    companion object {
        private lateinit var mContext: Field
        private lateinit var mKeyCode: Field

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerLoadedParam) {
            val miuiSingleKeyRule = param.classLoader.loadClass("com.android.server.policy.MiuiSingleKeyRule")
            module!!.hook(
                miuiSingleKeyRule.getDeclaredMethod("supportLongPress"),
                SupportLongPressHooker::class.java
            )
            module!!.hook(
                miuiSingleKeyRule.getDeclaredMethod("onLongPress", Long::class.java),
                OnLongPressHooker::class.java
            )
            mContext = miuiSingleKeyRule.getDeclaredField("mContext")
            mContext.isAccessible = true
            mKeyCode = miuiSingleKeyRule.getDeclaredField("mKeyCode")
            mKeyCode.isAccessible = true
        }

        @XposedHooker
        class SupportLongPressHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    if (mKeyCode.getInt(callback.thisObject) == 3) {
                        val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                        if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                            callback.returnAndSkip(true)
                        }
                    }
                }
            }
        }

        @XposedHooker
        class OnLongPressHooker : Hooker {
            companion object {
                @JvmStatic
                @BeforeInvocation
                fun before(callback: BeforeHookCallback) {
                    if (mKeyCode.getInt(callback.thisObject) == 3) {
                        val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                        if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                            val context = runCatching { mContext.get(callback.thisObject) as? Context }.getOrNull()
                            triggerCircleToSearch(
                                1,
                                context,
                                prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
                            )
                            callback.returnAndSkip(null)
                        }
                    }
                }
            }
        }
    }
}
