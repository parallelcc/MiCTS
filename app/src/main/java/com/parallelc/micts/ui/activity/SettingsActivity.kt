package com.parallelc.micts.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.MainApplication
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.config.Theme
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig
import com.parallelc.micts.ui.component.CascadeMenuItem
import com.parallelc.micts.ui.component.CascadingMenuPopup
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        viewModel = (application as MainApplication).settingsViewModel
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val themeController = remember(themeMode) { ThemeController(themeMode) }
            val systemInDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> true
                ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> false
                else -> systemInDark
            }
            SideEffect {
                val barStyle = if (isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                               else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            MiuixTheme(controller = themeController) {
                val locale by viewModel.locale.collectAsState()
                val config = Configuration(resources.configuration).apply { setLocale(locale) }
                CompositionLocalProvider(LocalContext provides createConfigurationContext(config)) {
                    SettingsScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    var showAboutDialog by remember { viewModel.showAboutDialog }
    var menuExpanded by remember { viewModel.menuExpanded }

    val appIcon = LocalContext.current.packageManager.getApplicationIcon(LocalContext.current.applicationInfo.packageName)

    val menuItems = listOf(
        CascadeMenuItem(
            text = stringResource(R.string.language),
            subItems = Language.entries.map { lang ->
                CascadeMenuItem(
                    text = stringResource(lang.id),
                    onSelected = { viewModel.updateAppConfig(AppConfig.KEY_LANGUAGE, lang.ordinal) },
                )
            },
        ),
        CascadeMenuItem(
            text = stringResource(R.string.theme),
            subItems = Theme.entries.map { theme ->
                val baseName = stringResource(theme.baseId)
                CascadeMenuItem(
                    text = if (theme.isMonet) "Monet ($baseName)" else baseName,
                    onSelected = { viewModel.updateAppConfig(AppConfig.KEY_THEME, theme.ordinal) },
                )
            },
        ),
        CascadeMenuItem(
            text = stringResource(R.string.about),
            onSelected = { showAboutDialog = true },
        ),
    )

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = BuildConfig.APP_NAME,
                scrollBehavior = scrollBehavior,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        CascadingMenuPopup(
                            show = menuExpanded,
                            items = menuItems,
                            onDismissRequest = { menuExpanded = false },
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SettingsPage(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            viewModel = viewModel,
            scrollBehavior = scrollBehavior
        )

        OverlayDialog(
            show = showAboutDialog,
            onDismissRequest = { showAboutDialog = false },
            title = BuildConfig.APP_NAME,
            content = {
                Column {
                    Row {
                        Image(
                            painter = rememberDrawablePainter(drawable = appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp)
                        )
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(BuildConfig.APP_NAME)
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MiuixTheme.textStyles.body2
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(buildAnnotatedString {
                        withLink(LinkAnnotation.Url(url = "https://github.com/parallelcc/micts")) {
                            withStyle(style = SpanStyle(color = MiuixTheme.colorScheme.primary)) {
                                append("https://github.com/parallelcc/micts")
                            }
                        }
                    })
                }
            }
        )
    }
}

@Composable
fun SettingsPage(
    modifier: Modifier,
    viewModel: SettingsViewModel,
    scrollBehavior: ScrollBehavior,
) {
    val appConfig by viewModel.appConfig.collectAsState()
    val xposedService by viewModel.xposedService.collectAsState()
    val xposedConfig by viewModel.xposedConfig.collectAsState()

    LazyColumn(
        modifier = modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        overscrollEffect = null,
    ) {
        item {
            SmallTitle(
                text = stringResource(R.string.app_settings),
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                SliderSettingItem(
                    title = stringResource(R.string.default_trigger_delay),
                    value = (appConfig[AppConfig.KEY_DEFAULT_DELAY] as Long).toFloat(),
                    onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_DEFAULT_DELAY, it.toLong()) },
                    valueRange = 0f..2000f
                )
                SliderSettingItem(
                    title = stringResource(R.string.tile_trigger_delay),
                    value = (appConfig[AppConfig.KEY_TILE_DELAY] as Long).toFloat(),
                    onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_TILE_DELAY, it.toLong()) },
                    valueRange = 0f..2000f
                )
                BasicComponent(
                    title = stringResource(R.string.vibrate),
                    endActions = {
                        Switch(
                            checked = appConfig[AppConfig.KEY_VIBRATE] as Boolean,
                            onCheckedChange = {
                                viewModel.updateAppConfig(AppConfig.KEY_VIBRATE, it)
                                xposedService?.run { viewModel.updateXposedConfig(XposedConfig.KEY_VIBRATE, it) }
                            }
                        )
                    }
                )
                BasicComponent(
                    title = stringResource(R.string.async_trigger),
                    endActions = {
                        Switch(
                            checked = appConfig[AppConfig.KEY_ASYNC_TRIGGER] as Boolean,
                            onCheckedChange = { viewModel.updateAppConfig(AppConfig.KEY_ASYNC_TRIGGER, it) }
                        )
                    }
                )
            }

            SmallTitle(
                text = stringResource(R.string.module_settings),
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            )
            if (xposedService == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    BasicComponent(
                        title = stringResource(R.string.access_xposed_service_failed),
                        startAction = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.error
                            )
                        },
                        endActions = {
                            val context = LocalContext.current
                            IconButton(onClick = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                exitProcess(1)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                )
                            }
                        }
                    )
                }
            } else {
                val isXiaomi = Build.MANUFACTURER == "Xiaomi"
                val isMeizu = Build.MANUFACTURER == "meizu"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (BuildConfig.APP_NAME == "MiCTS") {
                        var triggerServiceExpanded by remember { viewModel.triggerServiceExpanded }
                        val selectedOption = TriggerService.entries[xposedConfig[XposedConfig.KEY_TRIGGER_SERVICE] as Int].name
                        val options = TriggerService.getSupportedServices()

                        BasicComponent(
                            title = stringResource(R.string.system_trigger_service),
                            onClick = if (options.size > 1) { { triggerServiceExpanded = true } } else null,
                            endActions = {
                                Text(
                                    text = selectedOption,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                                if (options.size > 1) {
                                    DropdownArrowEndAction(MiuixTheme.colorScheme.onSurfaceVariantActions)
                                    OverlayListPopup(
                                        show = triggerServiceExpanded,
                                        alignment = PopupPositionProvider.Align.End,
                                        onDismissRequest = { triggerServiceExpanded = false }
                                    ) {
                                        ListPopupColumn {
                                            options.forEachIndexed { index, option ->
                                                DropdownImpl(
                                                    text = option.name,
                                                    optionSize = options.size,
                                                    isSelected = option.ordinal == xposedConfig[XposedConfig.KEY_TRIGGER_SERVICE] as Int,
                                                    onSelectedIndexChange = {
                                                        triggerServiceExpanded = false
                                                        viewModel.updateXposedConfig(XposedConfig.KEY_TRIGGER_SERVICE, option.ordinal)
                                                    },
                                                    index = index
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    if (isXiaomi) {
                        BasicComponent(
                            title = stringResource(R.string.trigger_by_long_press_gesture_handle),
                            endActions = {
                                Switch(
                                    checked = xposedConfig[XposedConfig.KEY_GESTURE_TRIGGER] as Boolean,
                                    onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_GESTURE_TRIGGER, it) }
                                )
                            }
                        )
                    }

                    if (isXiaomi || isMeizu) {
                        BasicComponent(
                            title = stringResource(R.string.trigger_by_long_press_home_button),
                            endActions = {
                                Switch(
                                    checked = xposedConfig[XposedConfig.KEY_HOME_TRIGGER] as Boolean,
                                    onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_HOME_TRIGGER, it) }
                                )
                            }
                        )
                    }

                    BasicComponent(
                        title = stringResource(R.string.device_spoof_for_google),
                        endActions = {
                            Switch(
                                checked = xposedConfig[XposedConfig.KEY_DEVICE_SPOOF] as Boolean,
                                onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_DEVICE_SPOOF, it) }
                            )
                        }
                    )

                    if (xposedConfig[XposedConfig.KEY_DEVICE_SPOOF] as Boolean) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ModelSpoofFields(xposedConfig, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSettingItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    BasicComponent(
        title = title,
        endActions = {
            Text(
                text = "${value.toInt()} ms",
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        bottomAction = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = 39
            )
        }
    )
}

@Composable
fun ModelSpoofFields(
    xposedConfig: Map<String, Any?>,
    viewModel: SettingsViewModel,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_MANUFACTURER] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_MANUFACTURER, it) },
            label = stringResource(R.string.manufacturer),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_BRAND] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_BRAND, it) },
            label = stringResource(R.string.brand),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_MODEL] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_MODEL, it) },
            label = stringResource(R.string.model),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_DEVICE] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_DEVICE, it) },
            label = stringResource(R.string.device),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
