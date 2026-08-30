# HyperOS 4 launcher payload

An LSPosed native entry that restores the "long press the gesture handle"
trigger on Android 17 / HyperOS 4.

## Why this exists

HyperOS 4 runs `com.miui.home` as a pure native process — it has no ART runtime
at all, so it carries no `[anon:dalvik-*]` mappings and LSPosed's Java callbacks
never fire for it. Every Java hook MiCTS and VISTrigger use for that trigger
(`NavStubView`, `NavStubGestureEventManager`, `NavBarEventHelper`,
`CircleToSearchHelper`) therefore stops working: the classes are not merely
renamed, the whole gesture pipeline moved into `libapp_launcher.so`.

This payload inline hooks the launcher's contextual-search long-press handler
instead, and routes the gesture into the launcher's own contextual-search entry
point. The resulting call reaches `ContextualSearchManagerService` in the system
server, where `NativeLauncherTriggerHooker` applies the module's settings.
MiCTS can keep the contextual-search route (with `CSMSHooker` supplying the
permission and provider) or redirect through `VoiceInteractionManagerService`;
VISTrigger always redirects the request to the configured Voice Interaction
Service.

Nothing here is version-pinned: the addresses are resolved out of the mapped
image at runtime by matching the code's structure, so an unknown launcher build
either resolves exactly or fails closed and leaves the launcher stock.

## Attribution

Most of this directory is taken from or derived from **MiuiBackGestureHook**,
which is licensed under Apache-2.0:

- Upstream: <https://github.com/wxxsfxyzm/MiuiBackGestureHook>
- Licence text: [LICENSE-Apache-2.0](LICENSE-Apache-2.0)

| File | Relationship to upstream |
| --- | --- |
| `native_api.h` | unmodified copy |
| `lsposed_hook_backend.h` | unmodified copy |
| `lsposed_hook_backend.cpp` | copied, then modified (see below) |
| `launcher_cs_resolver.{h,cpp}` | derived from `runtime_profile_resolver.{h,cpp}` |
| `micts_native_hook.cpp` | derived from `miui_home_native_hook.cpp` |

Apache-2.0 is one-way compatible with the GPL-3.0 this project uses, so these
files may be distributed as part of MiCTS.

### What changed

`lsposed_hook_backend.cpp`

- Dropped the LSPlt dependency. The `madvise` guard now reuses this file's own
  `PltHookRaw`, which removed the archive/direct split and its
  `/proc/self/maps` parsing along with it.
- Guards are tracked per image base, so both copies of
  `libhyper_os_flutter.so` (the system one and the one mapped from the APK) get
  protected.
- Renamed the log tag so it does not collide with upstream's when both modules
  are installed on the same device.

`launcher_cs_resolver.{h,cpp}`

- Kept only the contextual-search family. The side-boundary, runtime-singleton,
  RString and XiaoAi resolvers are dropped — MiCTS needs none of them.

`micts_native_hook.cpp`

- Kept the launcher/spawner process identification, the long-press closure
  protocol, the `PackageManager` feature-probe override and the motion snapshot
  used for upward-intent detection.
- Dropped everything specific to upstream's own purpose: back-gesture
  arbitration, return-home, the Dart state hooks and the authenticated
  broadcast bridge.
- The terminal routing calls the launcher's own contextual-search invoke rather
  than upstream's private broadcast bridge.

## Scope

arm64-v8a only, and only inside the `hyos_spawner` process family. The build
restricts the native ABI rather than the APK's, so 32-bit devices still install
and use the pure Java trigger paths.
