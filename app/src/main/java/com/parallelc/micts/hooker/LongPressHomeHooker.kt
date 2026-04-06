package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Context
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_HOME_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Field

class LongPressHomeHooker {
    companion object {
        private lateinit var mContext: Field
        private lateinit var mKeyCode: Field

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam) {
            val miuiSingleKeyRule = param.classLoader.loadClass("com.android.server.policy.MiuiSingleKeyRule")
            mContext = miuiSingleKeyRule.getDeclaredField("mContext").also { it.isAccessible = true }
            mKeyCode = miuiSingleKeyRule.getDeclaredField("mKeyCode").also { it.isAccessible = true }
            module!!.hook(
                miuiSingleKeyRule.getDeclaredMethod("onLongPress", Long::class.java)
            ).intercept(OnLongPressHooker())
            module!!.hook(
                miuiSingleKeyRule.getDeclaredMethod("supportLongPress")
            ).intercept(SupportLongPressHooker())
        }

        class OnLongPressHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                if (mKeyCode.getInt(chain.thisObject) == 3) {
                    val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                    if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                        val context = runCatching { mContext.get(chain.thisObject) as? Context }.getOrNull()
                        triggerCircleToSearch(
                            1,
                            context,
                            prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
                        )
                        return null
                    }
                }
                return chain.proceed()
            }
        }

        class SupportLongPressHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                if (mKeyCode.getInt(chain.thisObject) == 3) {
                    val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                    if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                        return true
                    }
                }
                return chain.proceed()
            }
        }
    }
}
