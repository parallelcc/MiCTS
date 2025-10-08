package com.parallelc.micts.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedService.OnScopeEventListener
import io.github.libxposed.service.XposedService.ServiceException
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("PrivateApi")
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private var appConfigPref : SharedPreferences
    private val _appConfig = MutableStateFlow<Map<String,Any>>(emptyMap())
    val appConfig: StateFlow<Map<String,Any>> = _appConfig
    private val _locale = MutableStateFlow<Locale>(Locale.ROOT)
    val locale: StateFlow<Locale> = _locale

    private val _xposedService = MutableStateFlow<XposedService?>(null)
    val xposedService: StateFlow<XposedService?> = _xposedService

    private lateinit var xposedConfigPref : SharedPreferences
    private val _xposedConfig = MutableStateFlow<Map<String,Any>>(emptyMap())
    val xposedConfig: StateFlow<Map<String,Any>> = _xposedConfig

    private val lastJobs: MutableMap<String, Job?> = mutableMapOf()

    var showAboutDialog = mutableStateOf(false)
    @OptIn(ExperimentalMaterial3Api::class)
    var topAppBarState = TopAppBarState(
        -Float.MAX_VALUE,
        0f,
        0f
    )
    var menuExpanded = mutableStateOf(false)
    var languageExpanded = mutableStateOf(false)
    var scrollState = ScrollState(0)
    var triggerServiceExpanded = mutableStateOf(false)

    init {
        appConfigPref = application.getSharedPreferences(AppConfig.CONFIG_NAME, MODE_PRIVATE)
        _appConfig.value = AppConfig.DEFAULT_CONFIG + appConfigPref.all.filterValues { it != null }.mapValues { it.value as Any }
        _locale.value = Language.entries[_appConfig.value[AppConfig.KEY_LANGUAGE] as Int].toLocale()
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        xposedConfigPref = service.getRemotePreferences(XposedConfig.CONFIG_NAME)
                        _xposedConfig.value = XposedConfig.DEFAULT_CONFIG + xposedConfigPref.all.filterValues { it != null }.mapValues { it.value as Any }
                        _xposedService.value = service
                        val scope = service.scope
                        _xposedConfig.value.forEach {
                            checkScope(scope, it.key, it.value)
                        }
                        updateXposedConfig(XposedConfig.KEY_VIBRATE, _appConfig.value[AppConfig.KEY_VIBRATE]!!)
                    }.onFailure { e ->
                        if (e is ServiceException) {
                            _xposedService.value = null
                        }
                    }
                }
            }

            override fun onServiceDied(service: XposedService) {
                viewModelScope.launch(Dispatchers.IO) {
                    _xposedService.value = null
                }
            }
        })
    }

    fun updateAppConfig(key: String, value: Any) {
        if (key == AppConfig.KEY_LANGUAGE) {
            _locale.value = Language.entries[value as Int].toLocale()
        } else {
            _appConfig.value = _appConfig.value.toMutableMap().apply {
                this[key] = value
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (value) {
                is Long -> appConfigPref.edit().putLong(key, value).apply()
                is Int -> appConfigPref.edit().putInt(key, value).apply()
                is Boolean -> appConfigPref.edit().putBoolean(key, value).apply()
            }
        }
    }

    fun checkScope(scope: List<String>, key: String, value: Any) {
        if (value as? Boolean != true &&
            (key != XposedConfig.KEY_TRIGGER_SERVICE || value as Int == TriggerService.VIS.ordinal)) {
            return
        }
        when (key) {
            XposedConfig.KEY_TRIGGER_SERVICE, XposedConfig.KEY_GESTURE_TRIGGER -> {
                if (scope.contains("system") == false) {
                    _xposedService.value?.requestScope("system", object: OnScopeEventListener{})
                }
            }
            XposedConfig.KEY_HOME_TRIGGER -> {
                if (Build.BRAND == "Xiaomi" && scope.contains("com.miui.home") == false) {
                    _xposedService.value?.requestScope("com.miui.home", object : OnScopeEventListener {})
                }
                if (Build.BRAND == "POCO" && scope.contains("com.mi.android.globallauncher") == false) {
                    _xposedService.value?.requestScope("com.mi.android.globallauncher", object: OnScopeEventListener{})
                }
                if (Build.MANUFACTURER == "meizu" && scope.contains("com.android.systemui") == false) {
                    _xposedService.value?.requestScope("com.android.systemui", object : OnScopeEventListener {})
                }
            }
            XposedConfig.KEY_DEVICE_SPOOF -> {
                if (scope.contains("com.google.android.googlequicksearchbox") == false) {
                    _xposedService.value?.requestScope("com.google.android.googlequicksearchbox", object : OnScopeEventListener {})
                }
            }
        }

    }

    fun updateXposedConfig(key: String, value: Any) {
        _xposedConfig.value = _xposedConfig.value.toMutableMap().apply {
            this[key] = value
        }
        lastJobs[key]?.cancel()
        lastJobs[key] = viewModelScope.launch(Dispatchers.IO) {
            when (value) {
                is String -> {
                    delay(500)
                    xposedConfigPref.edit().putString(key, value).apply()
                }
                is Boolean -> {
                    xposedConfigPref.edit().putBoolean(key, value).apply()
                }
                is Int -> {
                    xposedConfigPref.edit().putInt(key, value).apply()
                }
            }
            if (_xposedService.value == null) return@launch
            val scope = _xposedService.value!!.scope
            runCatching {
                checkScope(scope, key, value)
            }.onFailure { e ->
                if (e is ServiceException) {
                    _xposedService.value = null
                }
            }
        }
    }
}