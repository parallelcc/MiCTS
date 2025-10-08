package com.parallelc.micts.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.MainApplication
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        viewModel = (application as MainApplication).settingsViewModel
        setContent {
            MiCTSTheme {
                val locale by viewModel.locale.collectAsState()
                val config = Configuration(resources.configuration).apply { setLocale(locale) }
                CompositionLocalProvider(LocalContext provides createConfigurationContext(config)) {
                    SettingsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    var showAboutDialog by remember { viewModel.showAboutDialog }

    var topAppBarState = remember { viewModel.topAppBarState }
    val appIcon = LocalContext.current.packageManager.getApplicationIcon(LocalContext.current.applicationInfo.packageName)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(BuildConfig.APP_NAME)
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { viewModel.menuExpanded }
                    var languageExpanded by remember { viewModel.languageExpanded }
                    IconButton(onClick = { menuExpanded = !menuExpanded }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                        val context = LocalContext.current
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            CompositionLocalProvider(LocalContext provides context) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language)) },
                                onClick = {
                                    menuExpanded = false
                                    languageExpanded = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                onClick = {
                                    menuExpanded = false
                                    showAboutDialog = true
                                }
                            )
                                }
                        }
                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false },
                        ) {
                            CompositionLocalProvider(LocalContext provides context) {
                                Language.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.id)) },
                                        onClick = {
                                            viewModel.updateAppConfig(AppConfig.KEY_LANGUAGE, option.ordinal)
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }

                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        SettingsPage(modifier = Modifier.fillMaxSize().padding(paddingValues), viewModel)
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
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
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Text(buildAnnotatedString {
                    withLink(LinkAnnotation.Url(url = "https://github.com/parallelcc/micts")) {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("https://github.com/parallelcc/micts")
                        }
                    }
                })
            },
            confirmButton = {},
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = modifier.verticalScroll(remember { viewModel.scrollState })
    ) {
        val appConfig by viewModel.appConfig.collectAsState()
        val xposedService by viewModel.xposedService.collectAsState()
        val xposedConfig by viewModel.xposedConfig.collectAsState()

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.app_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        SliderSettingItem(
            title = stringResource(R.string.default_trigger_delay),
            value = (appConfig[AppConfig.KEY_DEFAULT_DELAY] as Long).toFloat(),
            onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_DEFAULT_DELAY, it.toLong())},
            valueRange = 0f..2000f
        )

        SliderSettingItem(
            title = stringResource(R.string.tile_trigger_delay),
            value = (appConfig[AppConfig.KEY_TILE_DELAY] as Long).toFloat(),
            onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_TILE_DELAY, it.toLong())},
            valueRange = 0f..2000f
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.vibrate)) },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_VIBRATE] as Boolean,
                    onCheckedChange = {
                        viewModel.updateAppConfig(AppConfig.KEY_VIBRATE, it)
                        xposedService?.run { viewModel.updateXposedConfig(XposedConfig.KEY_VIBRATE, it) }
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.async_trigger)) },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_ASYNC_TRIGGER] as Boolean,
                    onCheckedChange = {
                        viewModel.updateAppConfig(AppConfig.KEY_ASYNC_TRIGGER, it)
                    }
                )
            }
        )

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.module_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        if (xposedService == null) {
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                headlineContent = { Text(stringResource(R.string.access_xposed_service_failed)) },
                trailingContent = {
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
            return@Column
        }

        if (BuildConfig.APP_NAME == "MiCTS") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.system_trigger_service)) },
                trailingContent = {
                    Box {
                        var triggerServiceExpanded by remember { viewModel.triggerServiceExpanded }
                        val selectedOption = TriggerService.entries[xposedConfig[XposedConfig.KEY_TRIGGER_SERVICE] as Int].name
                        val options = TriggerService.getSupportedServices()

                        TextButton(onClick = { triggerServiceExpanded = true }) {
                            Text(text = selectedOption)
                        }

                        if (options.size <= 1) return@Box

                        DropdownMenu(
                            expanded = triggerServiceExpanded,
                            onDismissRequest = { triggerServiceExpanded = false }
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        triggerServiceExpanded = false
                                        viewModel.updateXposedConfig(XposedConfig.KEY_TRIGGER_SERVICE, option.ordinal)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }

        val isXiaomi = Build.MANUFACTURER == "Xiaomi"
        val isMeizu = Build.MANUFACTURER == "meizu"
        if (isXiaomi) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.trigger_by_long_press_gesture_handle)) },
                trailingContent = {
                    Switch(
                        checked = xposedConfig[XposedConfig.KEY_GESTURE_TRIGGER] as Boolean,
                        onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_GESTURE_TRIGGER, it) }
                    )
                }
            )
        }

        if (isXiaomi || isMeizu) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.trigger_by_long_press_home_button)) },
                trailingContent = {
                    Switch(
                        checked = xposedConfig[XposedConfig.KEY_HOME_TRIGGER] as Boolean,
                        onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_HOME_TRIGGER, it) }
                    )
                }
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.device_spoof_for_google)) },
            trailingContent = {
                Switch(
                    checked = xposedConfig[XposedConfig.KEY_DEVICE_SPOOF] as Boolean,
                    onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_DEVICE_SPOOF, it) }
                )
            }
        )

        if (xposedConfig[XposedConfig.KEY_DEVICE_SPOOF] as Boolean) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ModelSpoofFields(xposedConfig, viewModel)
            }
            Spacer(modifier = Modifier.height(8.dp))
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
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${value.toInt()} ms",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                textAlign = TextAlign.End
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 39
        )
    }
}

@Composable
fun ModelSpoofFields(
    xposedConfig: Map<String, Any?>,
    viewModel: SettingsViewModel,
) {
     Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_MANUFACTURER] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_MANUFACTURER, it) },
            label = { Text(stringResource(R.string.manufacturer)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_BRAND] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_BRAND, it) },
            label = { Text(stringResource(R.string.brand)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_MODEL] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_MODEL, it) },
            label = { Text(stringResource(R.string.model)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_DEVICE] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_DEVICE, it) },
            label = { Text(stringResource(R.string.device)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}