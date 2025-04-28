# MiCTS

[![Stars](https://img.shields.io/github/stars/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS) [![Downloads](https://img.shields.io/github/downloads/parallelcc/MiCTS/total)](https://github.com/parallelcc/MiCTS/releases) [![Release](https://img.shields.io/github/v/release/parallelcc/MiCTS)](https://github.com/parallelcc/MiCTS/releases/latest) [![DeepWiki](https://img.shields.io/badge/DeepWiki-parallelcc%2FMiCTS-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/parallelcc/MiCTS)

简体中文&nbsp;&nbsp;|&nbsp;&nbsp;[English](https://github.com/parallelcc/MiCTS/blob/main/README_en.md)

在任意Android 9–15设备上触发圈定即搜（Circle to Search）功能

*本应用只负责触发圈定即搜，无法处理触发成功后可能出现的问题*

## 使用方法

1. 安装最新版[Google](https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox)应用，开启自启动，关闭后台限制，将默认助理应用设置为Google


2. 安装并打开MiCTS
    - 如果幸运的话，在不需要root的情况下，打开MiCTS就会直接触发圈定即搜
    - 如果没有反应，大概率是因为Google对你的设备禁用了圈定即搜功能（可以通过在Logcat日志中查找`Omni invocation failed: not enabled`确认），在有root的情况下，可以尝试以下方法：
        - 在LSPosed里激活模块，在[MiCTS设置](#进入设置的方式)里开启`Google机型伪装`后，强制重启Google
        - 如果还是不行，使用[GMS-Flags](https://github.com/polodarb/GMS-Flags)，将`com.google.android.apps.search.omnient.device`的flag`45631784`设为true


3. 设置触发方式
    - 打开MiCTS即可触发，因此可以利用其他软件，比如悬浮球、Xposed Edge、ShortX等，将动作设置为打开MiCTS，实现自定义触发方式
    - MiCTS提供了一个触发磁贴，可将其添加到快速设置面板里，通过点击磁贴触发
    - 对于小米设备，MiCTS内置了长按小白条触发和长按Home键触发的功能，可以在MiCTS设置里开启（安装MiCTS后需要激活模块并重启手机才能使用）
    - 对于Android版本>=13的三星设备，可以从[三星应用商店](https://galaxystore.samsung.com/detail/com.samung.android.app.routineplus)或[Good Lock](https://galaxystore.samsung.com/detail/com.samsung.android.goodlock)里下载安装“日常程序+”，然后在“设置-模式和日常程序”里，创建日常程序通过按钮操作实现长按电源按钮等方式来启动MiCTS

## 设置

### 进入设置的方式
- 长按MiCTS应用图标，出现设置选项，点击进入
- 从LSPosed模块页面，点击MiCTS，再点击设置图标进入
- 长按快速设置面板的磁贴进入

### 应用设置
- 默认触发延迟：通过打开MiCTS触发的延迟
- 磁贴触发延迟：通过点击快速设置面板的磁贴触发的延迟

### 模块设置
需要在LSPosed里激活模块

- 系统触发服务：触发所使用的系统服务，只会显示当前支持的选项，依赖作用域选择系统框架
   - VIS：支持Android 9–15，需要将默认助理应用设置为Google，触发时一些设备的屏幕边缘会闪，没有激活模块的情况下只能使用此服务
   - CSHelper：支持Android 14 QPR3及以上，不需要设置默认助理应用，触发时屏幕边缘不会闪
   - CSService：支持Android 15及以上，圈定即搜专用的服务，效果同CSHelper


- 长按小白条触发：仅支持小米设备，依赖作用域选择系统桌面


- 长按Home键触发：仅支持小米设备，依赖作用域选择系统框架

 
- Google机型伪装：依赖作用域选择Google
   - 制造商：修改Google读取到的ro.product.manufacturer
   - 品牌：修改Google读取到的ro.product.brand
   - 型号：修改Google读取到的ro.product.model
   - 设备：修改Google读取到的ro.product.device 

## 常见问题

### 提示“触发失败！”

大概率是没有将Google设为默认助理，检查一下

### 触发出来是Google助理，不是圈定即搜

Google不是最新版，更新一下

### 有时无法成功触发，手动打开Google后才会出现刚才圈定即搜的界面

原因应该是墓碑机制导致的，看看手机有没有相关的设置可以把Google加到白名单里，比如电池优化选择无限制等，在模块设置里`系统触发服务`使用`CSHelper`应该没有这个问题

## Star History

<a href="https://star-history.com/#parallelcc/micts&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=parallelcc/micts&type=Date" />
 </picture>
</a>