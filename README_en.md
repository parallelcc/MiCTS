# MiCTS

[![Stars](https://img.shields.io/github/stars/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS) [![Downloads](https://img.shields.io/github/downloads/parallelcc/MiCTS/total)](https://github.com/parallelcc/MiCTS/releases) [![Release](https://img.shields.io/github/v/release/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS/releases/latest)

[简体中文](/README.md)&nbsp;&nbsp;|&nbsp;&nbsp;English

Trigger Circle to Search on any Android 9–15 device

*This app only aims to trigger Circle to Search and cannot handle issues that may occur after triggering successfully*

## How to Use

1. Install the latest version of [Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox), enable auto-start, disable background restrictions, and set Google as the default assistant app


2. Install and launch MiCTS
   - If you're lucky, Circle to Search will be triggered directly without root when launching MiCTS
   - If nothing happened, most likely it's because Google disabled Circle to Search for your device (you can confirm by checking the message `Omni invocation failed: not enabled` in Logcat). Try the following **with root**:
     - Activate the module in LSPosed, enable `Device spoof for Google` in the [MiCTS settings](#how-to-enter-settings), and force restart Google
     - If it still doesn't work, then change `com.google.android.apps.search.omnient.device` flag `45631784` to true using [GMS-Flags](https://github.com/polodarb/GMS-Flags)


3. Set up the trigger method
   - Launching MiCTS will trigger, so you can use other apps like Quick Ball, Xposed Edge, ShortX, etc., set launching MiCTS as the action to customize the trigger method
   - MiCTS provides a trigger tile, so you can add it to the Quick Settings panel and trigger by clicking it
   - For Xiaomi devices, MiCTS has built-in support for `Trigger by long press gesture handle` and `Trigger by long press home button`, which can be enabled in the MiCTS settings (need to activate the module and restart the phone after installing MiCTS)
   - For Samsung devices running Android 13 and above, you can download and install "Routines+" from the [Galaxy Store](https://galaxystore.samsung.com/detail/com.samung.android.app.routineplus) or [Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock). Then, go to Settings > Modes and Routines to create routines that launch MiCTS by Button action such as long-pressing the power button.
   

## Settings

### How to enter Settings
- Long press the MiCTS app icon to show the Settings option, then click to enter
- From the Modules page in LSPosed, click MiCTS, then click the settings icon to enter
- Long press the Quick Settings panel tile to enter

### App Settings
- Default trigger delay: The delay when triggering by launching MiCTS
- Tile trigger delay: The delay when triggering by the Quick Settings panel tile

### Module Settings
Need to activate the module in LSPosed
- System trigger service: The system service used by triggering. Only the services supported will be shown. Need to add System Framework to the scope in LSPosed
   - VIS: Supports on Android 9-15. Need to set Google as the default assistant app and the screen edge will flash when triggering for some devices. If the module is not activated, only this service will be used
   - CSHelper: Supports on Android 14 QPR3 and above. Don’t need to set Google as the default assistant app and the screen edge will not flash when triggering
   - CSService: Supports on Android 15 and above. A dedicated service for Circle to Search, same effect as CSHelper


- Trigger by long press gesture handle: Only supports on Xiaomi devices. Need to add System Launcher/POCO Launcher to the scope in LSPosed


- Trigger by long press home button: Only supports on Xiaomi devices. Need to add System Framework to the scope in LSPosed


- Device spoof for Google: Need to add Google to the scope in LSPosed
   - Manufacturer: Modify the `ro.product.manufacturer` value that Google reads
   - Brand: Modify the `ro.product.brand` value that Google reads
   - Model: Modify the `ro.product.model` value that Google reads
   - Device: Modify the `ro.product.device` value that Google reads

## FAQ

### Prompt "Trigger failed!"

Most likely because Google is not set as the default assistant, check it

### Google assistant appears instead of Circle to Search

Ensure that Google is the latest version

### Sometimes it doesn't trigger successfully, and the interface appears only after opening Google

This is likely due to the tombstone mechanism. Check if your device has related settings and add Google to the whitelist, such as selecting "No restrictions" in battery saver

This issue should not occur when the `System trigger service` is set to `CSHelper` in the Module Settings

## Star History

<a href="https://star-history.com/#parallelcc/micts&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
 </picture>
</a>