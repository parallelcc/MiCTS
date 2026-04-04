package com.parallelc.micts

import android.content.Context
import android.os.Build
import android.util.Log
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_DEVICE_SPOOF
import com.parallelc.micts.config.XposedConfig.KEY_SPOOF_BRAND
import com.parallelc.micts.config.XposedConfig.KEY_SPOOF_DEVICE
import com.parallelc.micts.config.XposedConfig.KEY_SPOOF_MANUFACTURER
import com.parallelc.micts.config.XposedConfig.KEY_SPOOF_MODEL
import com.parallelc.micts.hooker.CSMSHooker
import com.parallelc.micts.hooker.InvokeOmniHooker
import com.parallelc.micts.hooker.LongPressHomeHooker
import com.parallelc.micts.hooker.NavBarActionsConfigHooker
import com.parallelc.micts.hooker.NavBarEventHelperHooker
import com.parallelc.micts.hooker.NavStubGestureEventManagerHooker
import com.parallelc.micts.hooker.NavStubViewHooker
import com.parallelc.micts.hooker.VIMSHooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

var module: ModuleMain? = null

class ModuleMain : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        module = this
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        super.onSystemServerStarting(param)

        if (BuildConfig.APP_NAME == "MiCTS") {
            if (TriggerService.getSupportedServices().contains(TriggerService.CSHelper)) {
                runCatching {
                    VIMSHooker.hook(param)
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook VIMS fail", e)
                }
            }

            if (TriggerService.getSupportedServices().contains(TriggerService.CSService)) {
                runCatching {
                    CSMSHooker.hook(param)
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook CSMS fail", e)
                }
            }
        }

        if (Build.MANUFACTURER == "Xiaomi") {
            runCatching {
                LongPressHomeHooker.hook(param)
            }.onFailure { e ->
                log(Log.ERROR, "MiCTS", "hook LongPressHome fail", e)
            }
        }
    }

    override fun onPackageReady(param: PackageReadyParam) {
        super.onPackageReady(param)
        if (!param.isFirstPackage) return

        val prefs = getRemotePreferences(CONFIG_NAME)

        when (param.packageName) {
            "com.miui.home", "com.mi.android.globallauncher" -> {
                val skipHookTouch = runCatching {
                    NavStubGestureEventManagerHooker.hook(param)
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook NavStubGestureEventManager fail", e)
                }.recoverCatching {
                    val circleToSearchHelper = param.classLoader.loadClass("com.miui.home.recents.cts.CircleToSearchHelper")
                    hook(
                        circleToSearchHelper.getDeclaredMethod("invokeOmni", Context::class.java, Int::class.java, Int::class.java)
                    ).intercept(InvokeOmniHooker())
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook CircleToSearchHelper fail", e)
                }.recoverCatching {
                    NavBarEventHelperHooker.hook(param)
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook NavBarEventHelper fail", e)
                }.isSuccess

                runCatching {
                    NavStubViewHooker.hook(param, skipHookTouch)
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook NavStubView fail", e)
                }
            }
            "com.google.android.googlequicksearchbox" -> {
                if (!prefs.getBoolean(KEY_DEVICE_SPOOF, DEFAULT_CONFIG[KEY_DEVICE_SPOOF] as Boolean)) return
                val buildClass = param.classLoader.loadClass("android.os.Build")
                val MANUFACTURER = buildClass.getDeclaredField("MANUFACTURER")
                MANUFACTURER.isAccessible = true
                MANUFACTURER.set(null, prefs.getString(KEY_SPOOF_MANUFACTURER, DEFAULT_CONFIG[KEY_SPOOF_MANUFACTURER] as String))
                val BRAND = buildClass.getDeclaredField("BRAND")
                BRAND.isAccessible = true
                BRAND.set(null, prefs.getString(KEY_SPOOF_BRAND, DEFAULT_CONFIG[KEY_SPOOF_BRAND] as String))
                val MODEL = buildClass.getDeclaredField("MODEL")
                MODEL.isAccessible = true
                MODEL.set(null, prefs.getString(KEY_SPOOF_MODEL, DEFAULT_CONFIG[KEY_SPOOF_MODEL] as String))
                val DEVICE = buildClass.getDeclaredField("DEVICE")
                DEVICE.isAccessible = true
                DEVICE.set(null, prefs.getString(KEY_SPOOF_DEVICE, DEFAULT_CONFIG[KEY_SPOOF_DEVICE] as String))
            }
            "com.android.systemui" -> {
                if (Build.MANUFACTURER != "meizu") return
                runCatching {
                    NavBarActionsConfigHooker.hook(param)
                }.onFailure { e ->
                    log(Log.ERROR, "MiCTS", "hook NavBarActionsConfig fail", e)
                }
            }
        }
    }
}