package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Context
import android.os.IBinder
import android.util.Log
import com.parallelc.micts.module
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method

class CSMSHooker {
    companion object {
        private var enforcePermission: Method? = null
        private var getContextualSearchPackageName: Method? = null
        private var contextualSearchPackageName: Int = 0

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam) {
            val rString = param.classLoader.loadClass("com.android.internal.R\$string")
            contextualSearchPackageName = rString.getField("config_defaultContextualSearchPackageName").getInt(null)
            val systemServer = param.classLoader.loadClass("com.android.server.SystemServer")
            module!!.hook(systemServer.getDeclaredMethod("deviceHasConfigString", Context::class.java, Int::class.java))
                .intercept(DeviceHasConfigStringHooker())

            val csms = param.classLoader.loadClass("com.android.server.contextualsearch.ContextualSearchManagerService")
            enforcePermission = csms.getDeclaredMethod("enforcePermission", String::class.java)
            getContextualSearchPackageName = csms.getDeclaredMethod("getContextualSearchPackageName")
        }

        @SuppressLint("PrivateApi")
        fun startContextualSearch(entryPoint: Int): Boolean {
            var hooks = mutableListOf<HookHandle>()
            return runCatching {
                hooks += module!!.hook(enforcePermission!!).intercept(EnforcePermissionHooker())
                hooks += module!!.hook(getContextualSearchPackageName!!).intercept(GetCSPackageNameHooker())

                val icsmClass = Class.forName("android.app.contextualsearch.IContextualSearchManager")
                val cs = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "contextual_search")
                val icsm = Class.forName("android.app.contextualsearch.IContextualSearchManager\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, cs)
                icsmClass.getDeclaredMethod("startContextualSearch", Int::class.java).invoke(icsm, entryPoint)
            }.onFailure { e ->
                module!!.log(Log.ERROR, "MiCTS", "invoke startContextualSearch fail", e)
            }.also {
                hooks.forEach { hook -> hook.unhook() }
            }.isSuccess
        }

        class DeviceHasConfigStringHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                return if (chain.args[1] == contextualSearchPackageName) {
                    true
                } else {
                    chain.proceed()
                }
            }
        }

        class EnforcePermissionHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                return null
            }
        }

        class GetCSPackageNameHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                return "com.google.android.googlequicksearchbox"
            }
        }
    }
}