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

class BaseMiuiPWMHooker {
    companion object {
        private lateinit var mContext: Field
        private lateinit var mHandler: Field
        private lateinit var mKeyLongPressTimeout: Field

        @Volatile
        private var pendingTrigger: Runnable? = null

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam) {
            val phoneWM = param.classLoader.loadClass("com.android.server.policy.PhoneWindowManager")
            val baseMiuiPWM = param.classLoader.loadClass("com.android.server.policy.BaseMiuiPhoneWindowManager")
            mContext = phoneWM.getDeclaredField("mContext").also { it.isAccessible = true }
            mHandler = baseMiuiPWM.getDeclaredField("mHandler").also { it.isAccessible = true }
            mKeyLongPressTimeout = baseMiuiPWM.getDeclaredField("mKeyLongPressTimeout").also { it.isAccessible = true }
            module!!.hook(
                baseMiuiPWM.getDeclaredMethod("postKeyLongPress", Int::class.java, Boolean::class.java)
            ).intercept(PostKeyLongPressHooker())
            module!!.hook(
                baseMiuiPWM.getDeclaredMethod("removeKeyLongPress", Int::class.java)
            ).intercept(RemoveKeyLongPressHooker())
        }

        class PostKeyLongPressHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                if (chain.args[0] as Int == 3) {
                    val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                    if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                        val context = runCatching { mContext.get(chain.thisObject) as? Context }.getOrNull()
                        val handler = mHandler.get(chain.thisObject) as? android.os.Handler ?: return null
                        val delay = mKeyLongPressTimeout.getInt(chain.thisObject).toLong()
                        val runnable = Runnable {
                            triggerCircleToSearch(
                                1,
                                context,
                                prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
                            )
                        }
                        pendingTrigger = runnable
                        handler.postDelayed(runnable, delay)
                        return null
                    }
                }
                return chain.proceed()
            }
        }

        class RemoveKeyLongPressHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                if (chain.args[0] as Int == 3) {
                    val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                    if (prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                        val handler = mHandler.get(chain.thisObject) as? android.os.Handler
                        pendingTrigger?.let { handler?.removeCallbacks(it) }
                        pendingTrigger = null
                        return null
                    }
                }
                return chain.proceed()
            }
        }
    }
}
