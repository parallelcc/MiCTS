package com.parallelc.micts.hooker

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Binder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig.CONFIG_NAME
import com.parallelc.micts.config.XposedConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.XposedConfig.KEY_GESTURE_TRIGGER
import com.parallelc.micts.config.XposedConfig.KEY_TRIGGER_SERVICE
import com.parallelc.micts.config.XposedConfig.KEY_VIBRATE
import com.parallelc.micts.module
import com.parallelc.micts.ui.activity.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method
import java.util.concurrent.Executors

object NativeLauncherTriggerHooker {
    private const val ANDROID_17 = 37

    private val LAUNCHER_PACKAGES = setOf(
        "com.miui.home",
        "com.mi.android.globallauncher",
    )

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= ANDROID_17

    /** Prevents a bridge-initiated request from being mistaken for a launcher request. */
    private val selfInvocation = ThreadLocal<Boolean>()

    private var launcherUid: Int = -1
    private var launcherUidResolved = false
    private val triggerExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "native-launcher-trigger").apply { isDaemon = true }
    }

    fun <T> asSelfInvocation(block: () -> T): T {
        val previous = selfInvocation.get()
        selfInvocation.set(true)
        return try {
            block()
        } finally {
            if (previous == null) selfInvocation.remove() else selfInvocation.set(previous)
        }
    }

    @SuppressLint("PrivateApi")
    fun hook(param: SystemServerStartingParam) {
        val stub = param.classLoader.loadClass(
            "com.android.server.contextualsearch.ContextualSearchManagerService\$ContextualSearchManagerStub"
        )
        val starts = findStartMethods(stub)
        if (starts.isEmpty()) {
            throw NoSuchMethodException(
                "${stub.name}.startContextualSearch/startContextualSearchForApp"
            )
        }
        starts.forEach { start ->
            module!!.hook(start).intercept(StartHooker(start.name))
        }
    }

    /** QPR1 added configs; HyperOS 4's native launcher uses the app-specific entry point. */
    private fun findStartMethods(stub: Class<*>): List<Method> = stub.declaredMethods.filter {
        if (it.returnType != Void.TYPE) return@filter false
        val parameters = it.parameterTypes
        when (it.name) {
            "startContextualSearch" -> when (parameters.size) {
                1 -> parameters[0] == Int::class.javaPrimitiveType
                2 -> parameters[0] == Int::class.javaPrimitiveType &&
                    parameters[1].name == "android.app.contextualsearch.ContextualSearchConfig"
                else -> false
            }
            "startContextualSearchForApp" -> parameters.size == 1 &&
                parameters[0].name == "android.app.contextualsearch.ContextualSearchConfig"
            else -> false
        }
    }

    private fun isLauncherCaller(uid: Int): Boolean {
        if (launcherUidResolved) return uid == launcherUid
        val context = runCatching {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication")
                .invoke(null) as? Context
        }.getOrNull() ?: return false
        val resolved = LAUNCHER_PACKAGES.firstNotNullOfOrNull { pkg ->
            runCatching { context.packageManager.getPackageUid(pkg, 0) }.getOrNull()
        } ?: return false // Not resolvable yet — retry on the next request.
        launcherUid = resolved
        launcherUidResolved = true
        return uid == launcherUid
    }

    // This hook is installed only on Android 17; the system server owns VIBRATE.
    @SuppressLint("MissingPermission", "NewApi")
    private fun vibrate() {
        runCatching {
            val context = Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication")
                .invoke(null) as? Context ?: return
            val vibrator = context.getSystemService(Vibrator::class.java) ?: return
            if (!vibrator.hasVibrator()) return
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setFlags(128)
                .build()
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), attributes)
        }.onFailure { e ->
            module!!.log(Log.ERROR, BuildConfig.APP_NAME, "native launcher trigger vibrate fail", e)
        }
    }

    /** What to do with a contextual-search request seen in the system server. */
    private enum class Decision {
        /** Not ours — let the platform handle it unchanged. */
        PASS_THROUGH,

        /** The launcher's request: let it through, but with our bypass applied. */
        BRIDGE,

        /** Already handled (or deliberately dropped); stop the platform call. */
        CONSUME,
    }

    class StartHooker(private val methodName: String) : Hooker {
        override fun intercept(chain: Chain): Any? {
            val decision = runCatching { decide(chain) }.getOrElse { error ->
                module!!.log(
                    Log.ERROR, BuildConfig.APP_NAME,
                    "native launcher trigger gate failed; passing the request through",
                    error
                )
                Decision.PASS_THROUGH
            }
            return when (decision) {
                Decision.PASS_THROUGH -> chain.proceed()
                Decision.BRIDGE -> CSMSHooker.withPermissionBypass { chain.proceed() }
                Decision.CONSUME -> null
            }
        }

        private fun decide(chain: Chain): Decision {
            if (selfInvocation.get() == true) return Decision.PASS_THROUGH
            if (!isLauncherCaller(Binder.getCallingUid())) return Decision.PASS_THROUGH

            module!!.log(
                Log.INFO,
                BuildConfig.APP_NAME,
                "native launcher trigger intercepted via $methodName"
            )

            val prefs = module!!.getRemotePreferences(CONFIG_NAME)
            if (!prefs.getBoolean(
                    KEY_GESTURE_TRIGGER,
                    DEFAULT_CONFIG[KEY_GESTURE_TRIGGER] as Boolean
                )
            ) {
                module!!.log(
                    Log.INFO,
                    BuildConfig.APP_NAME,
                    "native launcher trigger is disabled; ignoring"
                )
                return Decision.CONSUME
            }
            if (prefs.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)) {
                vibrate()
            }
            val selected = if (BuildConfig.APP_NAME == "VISTrigger") {
                TriggerService.VIS.ordinal
            } else {
                prefs.getInt(
                    KEY_TRIGGER_SERVICE,
                    DEFAULT_CONFIG[KEY_TRIGGER_SERVICE] as Int
                )
            }
            if (selected == TriggerService.CSService.ordinal) return Decision.BRIDGE

            val entryPoint = chain.args.firstOrNull { it is Int } as? Int ?: 1
            triggerExecutor.execute {
                // The haptic already played above; do not repeat it.
                asSelfInvocation { triggerCircleToSearch(entryPoint, null, false) }
            }
            return Decision.CONSUME
        }
    }
}
