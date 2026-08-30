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
import java.lang.reflect.Method

class LongPressHomeHooker {
    companion object {
        private lateinit var mContext: Field
        private lateinit var mKeyCode: Field

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam) {
            // HyperOS 4 split the single-key rules into one class per key and
            // overrides the callbacks there, so hooking the shared base class no
            // longer intercepts anything. Prefer the Home rule and fall back to
            // the base class on older releases that still implement it directly.
            val rule = listOf(
                "com.android.server.input.shortcut.singlekeyrule.HomeKeyRule",
                "com.android.server.policy.MiuiSingleKeyRule",
            ).firstNotNullOfOrNull { name ->
                runCatching { param.classLoader.loadClass(name) }.getOrNull()
            } ?: throw ClassNotFoundException("no MIUI single-key rule class")

            mContext = findField(rule, "mContext")
            mKeyCode = findField(rule, "mKeyCode")

            val longPress = findCallback(rule, "onMiuiLongPress", "onLongPress")
                ?: throw NoSuchMethodException("${rule.name}.onMiuiLongPress")
            module!!.hook(longPress).intercept(OnLongPressHooker())

            val supports = listOfNotNull(
                findCallback(rule, "supportLongPress"),
                findCallback(rule, "miuiSupportLongPress"),
            )
            if (supports.isEmpty()) {
                throw NoSuchMethodException("${rule.name}.supportLongPress")
            }
            supports.forEach { module!!.hook(it).intercept(SupportLongPressHooker()) }
        }

        private fun findField(owner: Class<*>, name: String): Field {
            var current: Class<*>? = owner
            while (current != null) {
                runCatching { current!!.getDeclaredField(name) }.getOrNull()?.let {
                    it.isAccessible = true
                    return it
                }
                current = current.superclass
            }
            throw NoSuchFieldException("${owner.name}.$name")
        }

        /** Finds the first declared method matching any of [names], nearest class first. */
        private fun findCallback(owner: Class<*>, vararg names: String): Method? {
            var current: Class<*>? = owner
            while (current != null) {
                for (name in names) {
                    current.declaredMethods
                        .filter { it.name == name }
                        // Each callback is declared once; if a release ever
                        // overloads one, prefer the simplest form.
                        .minByOrNull { it.parameterTypes.size }
                        ?.let { return it }
                }
                current = current.superclass
            }
            return null
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
