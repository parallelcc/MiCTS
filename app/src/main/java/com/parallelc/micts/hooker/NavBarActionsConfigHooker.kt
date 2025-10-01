package com.parallelc.micts.hooker

import android.content.Context
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_HOME_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker

class NavBarActionsConfigHooker {
    companion object {
        fun hook(param: PackageLoadedParam) {
            val navBarActionsConfig =
                param.classLoader.loadClass("com.flyme.systemui.navigationbar.actions.NavBarActionsConfig")
            module!!.hook(
                navBarActionsConfig.getDeclaredMethod(
                    "helpStartAI",
                    Context::class.java,
                    String::class.java
                ),
                HelpStartAiHooker::class.java
            )
        }
    }

    @XposedHooker
    class HelpStartAiHooker : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                val prefs = module!!.getRemotePreferences(CONFIG_NAME)
                if (!prefs.getBoolean(KEY_HOME_TRIGGER, DEFAULT_CONFIG[KEY_HOME_TRIGGER] as Boolean)) {
                    return
                }
                triggerCircleToSearch(
                    1,
                    callback.args[0] as? Context,
                    prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
                )
                callback.returnAndSkip(null)
            }
        }
    }
}