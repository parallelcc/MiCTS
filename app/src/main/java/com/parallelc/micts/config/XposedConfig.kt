package com.parallelc.micts.config

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Build

enum class TriggerService(val isSupported: Boolean) {
    VIS(true),
    @SuppressLint("DiscouragedApi")
    CSHelper(Resources.getSystem().getIdentifier("config_defaultContextualSearchKey", "string", "android") != 0),
    @SuppressLint("PrivateApi")
    CSService(
        try {
            Class.forName("android.app.contextualsearch.ContextualSearchManager")
            true
        } catch (_: Exception) {
            false
        }
    );

    companion object {
        fun getSupportedServices(): List<TriggerService> {
            return TriggerService.entries.filter { it.isSupported }
        }
    }
}

object XposedConfig {
    const val CONFIG_NAME = "xposed_config"
    const val KEY_TRIGGER_SERVICE = "trigger_service"
    const val KEY_GESTURE_TRIGGER = "gesture_trigger"
    const val KEY_HOME_TRIGGER = "home_trigger"
    const val KEY_DEVICE_SPOOF = "device_spoof"
    const val KEY_SPOOF_MANUFACTURER = "spoof_manufacturer"
    const val KEY_SPOOF_BRAND = "spoof_brand"
    const val KEY_SPOOF_MODEL = "spoof_model"
    const val KEY_SPOOF_DEVICE = "spoof_device"
    const val KEY_VIBRATE = "vibrate"

    val DEFAULT_CONFIG = mapOf<String, Any>(
        KEY_TRIGGER_SERVICE to TriggerService.getSupportedServices().last().ordinal,
        KEY_GESTURE_TRIGGER to (Build.MANUFACTURER == "Xiaomi"),
        KEY_HOME_TRIGGER to (Build.MANUFACTURER == "Xiaomi" || Build.MANUFACTURER == "meizu"),
        KEY_DEVICE_SPOOF to true,
        KEY_SPOOF_MANUFACTURER to "Google",
        KEY_SPOOF_BRAND to "google",
        KEY_SPOOF_MODEL to "Pixel 9 Pro",
        KEY_SPOOF_DEVICE to "caiman",
        KEY_VIBRATE to false,
    )
}