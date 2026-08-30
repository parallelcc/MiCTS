package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Context
import android.os.IBinder
import android.util.Log
import com.parallelc.micts.module
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method

class CSMSHooker {
    companion object {
        private const val CONTEXTUAL_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"

        private var enforcePermission: Method? = null
        private var getContextualSearchPackageName: Method? = null
        private var contextualSearchPackageName: Int = 0

        /**
         * Marks the calls MiCTS is responsible for. The permission and provider
         * hooks stay installed but only act while this is set, so unrelated
         * requests keep the platform's own behaviour.
         */
        private val bypass = ThreadLocal<Boolean>()

        @SuppressLint("PrivateApi")
        fun hookServiceBootstrap(param: SystemServerStartingParam) {
            val rString = param.classLoader.loadClass("com.android.internal.R\$string")
            contextualSearchPackageName = rString.getField("config_defaultContextualSearchPackageName").getInt(null)
            val systemServer = param.classLoader.loadClass("com.android.server.SystemServer")
            module!!.hook(systemServer.getDeclaredMethod("deviceHasConfigString", Context::class.java, Int::class.java))
                .intercept(DeviceHasConfigStringHooker())
        }

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam) {
            hookServiceBootstrap(param)
            val csms = param.classLoader.loadClass("com.android.server.contextualsearch.ContextualSearchManagerService")
            enforcePermission = csms.getDeclaredMethod("enforcePermission", String::class.java)
            getContextualSearchPackageName = csms.getDeclaredMethod("getContextualSearchPackageName")
            module!!.hook(enforcePermission!!).intercept(EnforcePermissionHooker())
            module!!.hook(getContextualSearchPackageName!!).intercept(GetCSPackageNameHooker())
        }

        /**
         * Runs [block] with the permission check bypassed and the provider forced
         * to the Google app — neither MiCTS nor the launcher holds
         * ACCESS_CONTEXTUAL_SEARCH.
         */
        fun <T> withPermissionBypass(block: () -> T): T {
            val previous = bypass.get()
            bypass.set(true)
            return try {
                block()
            } finally {
                if (previous == null) bypass.remove() else bypass.set(previous)
            }
        }

        @SuppressLint("PrivateApi")
        fun startContextualSearch(entryPoint: Int): Boolean {
            return runCatching {
                val icsmClass = Class.forName("android.app.contextualsearch.IContextualSearchManager")
                val cs = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "contextual_search")
                val icsm = Class.forName("android.app.contextualsearch.IContextualSearchManager\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, cs)
                // QPR1 added a ContextualSearchConfig parameter and dropped the
                // single-argument form; take whichever this platform declares.
                val (start, arguments) = runCatching {
                    icsmClass.getDeclaredMethod("startContextualSearch", Int::class.java) to
                        arrayOf<Any?>(entryPoint)
                }.getOrElse {
                    val configClass =
                        Class.forName("android.app.contextualsearch.ContextualSearchConfig")
                    icsmClass.getDeclaredMethod(
                        "startContextualSearch", Int::class.java, configClass
                    ) to arrayOf<Any?>(entryPoint, null)
                }
                withPermissionBypass {
                    // The caller already applied the module's settings.
                    NativeLauncherTriggerHooker.asSelfInvocation {
                        start.invoke(icsm, *arguments)
                    }
                }
            }.onFailure { e ->
                module!!.log(Log.ERROR, "MiCTS", "invoke startContextualSearch fail", e)
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
                return if (bypass.get() == true) null else chain.proceed()
            }
        }

        class GetCSPackageNameHooker : Hooker {
            override fun intercept(chain: Chain): Any? {
                return if (bypass.get() == true) CONTEXTUAL_SEARCH_PACKAGE else chain.proceed()
            }
        }
    }
}
