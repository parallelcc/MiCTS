package com.parallelc.micts.hooker

import android.content.Context
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_HOME_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class NavBarActionsConfigHooker {
    companion object {
        fun hook(param: PackageReadyParam) {
            val navBarActionsConfig =
                param.classLoader.loadClass("com.flyme.systemui.navigationbar.actions.NavBarActionsConfig")
            module!!.hook(
                navBarActionsConfig.getDeclaredMethod(
                    "helpStartAI",
                    Context::class.java,
                    String::class.java
                )
            ).intercept(HelpStartAiHooker())
        }
    }

    class HelpStartAiHooker : Hooker {
        override fun intercept(chain: Chain): Any? {
            val prefs = module!!.getRemotePreferences(CONFIG_NAME)
            if (!prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                return chain.proceed()
            }
            triggerCircleToSearch(
                1,
                chain.args[0] as? Context,
                prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
            )
            return null
        }
    }
}