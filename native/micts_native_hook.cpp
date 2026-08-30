// SPDX-License-Identifier: Apache-2.0
// Derived from MiuiBackGestureHook; see native/README.md for attribution.
//
// Restores the gesture-handle long press on HyperOS 4, where the launcher runs
// natively and no Java hook can reach it. Resolves the contextual-search graph
// out of the mapped image, hooks the long-press handler, and routes it into the
// launcher's own invoke helper — the China build's stock routing rejects it.
//
// Fails closed: anything unresolved leaves the launcher stock. See README.md.

#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>

#include "launcher_cs_resolver.h"
#include "lsposed_hook_backend.h"
#include "native_api.h"

namespace {

constexpr char kLogTag[] = "MiCTSNative";
constexpr char kLauncherProcessName[] = "com.miui.home";
constexpr char kSpawnerPath[] = "/system_ext/bin/hyos_spawner";
constexpr char kHyperRuntimeName[] = "libhyper_os_flutter.so";
constexpr char kDataLauncherLibraryTail[] =
        "/base.apk!/lib/arm64-v8a/libapp_launcher.so";
constexpr char kSystemLauncherLibraryTail[] =
        "/MiuiHome.apk!/lib/arm64-v8a/libapp_launcher.so";
constexpr char kLauncherEntrySymbol[] = "app_entry_point";

// The launcher gates Circle to Search on these; China builds declare neither.
constexpr char kPlatformContextualSearchFeature[] =
        "android.software.contextualsearch";
constexpr char kGoogleContextualSearchFeature[] =
        "com.google.android.feature.CONTEXTUAL_SEARCH";

struct NativeResult {
    // The public HyperOS PackageManager wrapper returns a 48-byte RResult
    // through x8.
    uint8_t bytes[48];
};

// An upward swipe past this many pixels is treated as a Home/Recents gesture
// rather than a long press, matching the launcher's own bias.
constexpr float kUpwardIntentPixels = 8.0f;

using ContextualLongPressHandlerFn = void (*)(void*, uint32_t);
using ContextualClosureCleanupFn = void (*)(void*);
using ContextualSearchInvokeFn = uint8_t (*)(uint32_t);
using PackageManagerHasSystemFeatureFn =
        NativeResult (*)(void*, const char*, size_t, uint32_t);
using MotionEventActionFn = int32_t (*)(void*);
using MotionEventLongFn = int64_t (*)(void*);
using MotionEventIntFn = int32_t (*)(void*);
using MotionEventFloatFn = float (*)(void*);

void* g_original_long_press_handler = nullptr;
void* g_contextual_search_invoke = nullptr;
void* g_original_has_system_feature = nullptr;
void* g_original_motion_get_action_masked = nullptr;
void* g_motion_get_down_time = nullptr;
void* g_motion_get_device_id = nullptr;
void* g_motion_get_source = nullptr;
void* g_motion_get_raw_y = nullptr;
void* g_motion_get_y = nullptr;
void* g_launcher_handle = nullptr;

// Snapshot of the gesture stream currently being tracked, keyed by the stream
// identity so a stale pointer can never be mistaken for the live gesture.
uint32_t g_motion_snapshot_valid = 0u;
int64_t g_motion_snapshot_down_time = 0;
int32_t g_motion_snapshot_device_id = 0;
int32_t g_motion_snapshot_source = 0;
uint32_t g_motion_snapshot_down_y_bits = 0u;
uint32_t g_motion_snapshot_current_y_bits = 0u;

void Log(int priority, const char* message) {
    __android_log_write(priority, kLogTag, message);
}

template <typename T>
T AtomicLoad(T* target) {
    return __atomic_load_n(target, __ATOMIC_ACQUIRE);
}

template <typename T>
void AtomicStore(T* target, T value) {
    __atomic_store_n(target, value, __ATOMIC_RELEASE);
}

// The diagnostic counters below stay `volatile` so a debugger or a tombstone
// reads the committed value; give the helpers a matching overload.
template <typename T>
void AtomicStore(volatile T* target, T value) {
    __atomic_store_n(target, value, __ATOMIC_RELEASE);
}

constexpr size_t ConstStringLength(const char* value) {
    size_t length = 0u;
    if (value == nullptr) return length;
    while (value[length] != '\0') ++length;
    return length;
}

constexpr bool StringsEqual(const char* left, const char* right) {
    if (left == nullptr || right == nullptr) return false;
    size_t index = 0u;
    while (left[index] != '\0' && right[index] != '\0') {
        if (left[index] != right[index]) return false;
        ++index;
    }
    return left[index] == right[index];
}

constexpr bool StartsWith(const char* value, const char* prefix) {
    if (value == nullptr || prefix == nullptr) return false;
    for (size_t index = 0u; prefix[index] != '\0'; ++index) {
        if (value[index] != prefix[index]) return false;
    }
    return true;
}

constexpr bool EndsWith(const char* value, const char* suffix) {
    if (value == nullptr || suffix == nullptr) return false;
    const size_t value_length = ConstStringLength(value);
    const size_t suffix_length = ConstStringLength(suffix);
    if (suffix_length > value_length) return false;
    const size_t start = value_length - suffix_length;
    for (size_t index = 0u; index < suffix_length; ++index) {
        if (value[start + index] != suffix[index]) return false;
    }
    return true;
}

constexpr bool IsLauncherLibraryPath(const char* path) {
    if (path == nullptr) return false;
    if (StringsEqual(path, "libapp_launcher.so")) return true;
    if (StartsWith(path, "/data/app/") &&
            EndsWith(path, kDataLauncherLibraryTail)) {
        return true;
    }
    return StartsWith(path, "/product/priv-app/MiuiHome/") &&
            EndsWith(path, kSystemLauncherLibraryTail);
}

static_assert(IsLauncherLibraryPath(
        "/product/priv-app/MiuiHome/MiuiHome.apk!/lib/arm64-v8a/"
        "libapp_launcher.so"));
static_assert(IsLauncherLibraryPath(
        "/data/app/~~token/com.miui.home-token/base.apk!/lib/arm64-v8a/"
        "libapp_launcher.so"));
static_assert(!IsLauncherLibraryPath(
        "/data/local/tmp/base.apk!/lib/arm64-v8a/libapp_launcher.so"));
static_assert(!IsLauncherLibraryPath(
        "/data/app/token/base.apk!/lib/armeabi-v7a/libapp_launcher.so"));

bool IsLauncherProcess() {
    const int fd = open("/proc/self/cmdline", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    char command_line[64]{};
    ssize_t result;
    do {
        result = read(fd, command_line, sizeof(command_line));
    } while (result < 0 && errno == EINTR);
    close(fd);
    if (result <= 0) return false;
    constexpr size_t expected_length = ConstStringLength(kLauncherProcessName);
    return static_cast<size_t>(result) > expected_length &&
            command_line[expected_length] == '\0' &&
            StringsEqual(command_line, kLauncherProcessName);
}

bool IsHyosSpawnerProcessFamily() {
    char executable[128]{};
    const ssize_t length =
            readlink("/proc/self/exe", executable, sizeof(executable) - 1u);
    if (length <= 0 || static_cast<size_t>(length) >= sizeof(executable)) {
        return false;
    }
    executable[length] = '\0';
    return StringsEqual(executable, kSpawnerPath);
}

// LSPosed initializes in the root spawner (cmdline `usap64`) before it forks
// the package process, so accept the spawner and the launcher child alike.
bool IsLauncherHookProcess() {
    return IsLauncherProcess() || IsHyosSpawnerProcessFamily();
}

/**
 * Opens Circle to Search through the launcher's own entry point — the one path
 * already wired to the platform service. MiCTS's system server hooks still see
 * the resulting call and apply the user's settings to it.
 */
uint32_t FloatBits(float value) {
    uint32_t bits = 0u;
    memcpy(&bits, &value, sizeof(bits));
    return bits;
}

float BitsFloat(uint32_t bits) {
    float value = 0.0f;
    memcpy(&value, &bits, sizeof(value));
    return value;
}

float ReadMotionY(void* event) {
    auto get_raw_y =
            reinterpret_cast<MotionEventFloatFn>(AtomicLoad(&g_motion_get_raw_y));
    if (get_raw_y != nullptr) return get_raw_y(event);
    auto get_y = reinterpret_cast<MotionEventFloatFn>(AtomicLoad(&g_motion_get_y));
    return get_y == nullptr ? 0.0f : get_y(event);
}

/** Tracks vertical travel, so a dwelling swipe is not taken for a long press. */
void PublishMotionSnapshot(void* event, int32_t action) {
    // This runs for every motion event the launcher dispatches, so do nothing
    // until a DOWN has started a snapshot worth updating.
    const bool is_down = static_cast<uint32_t>(action & 0xff) == 0u;
    if (!is_down &&
            __atomic_load_n(&g_motion_snapshot_valid, __ATOMIC_ACQUIRE) == 0u) {
        return;
    }
    auto get_down_time =
            reinterpret_cast<MotionEventLongFn>(AtomicLoad(&g_motion_get_down_time));
    auto get_device_id =
            reinterpret_cast<MotionEventIntFn>(AtomicLoad(&g_motion_get_device_id));
    auto get_source =
            reinterpret_cast<MotionEventIntFn>(AtomicLoad(&g_motion_get_source));
    if (event == nullptr || get_down_time == nullptr ||
            get_device_id == nullptr || get_source == nullptr ||
            (AtomicLoad(&g_motion_get_raw_y) == nullptr &&
             AtomicLoad(&g_motion_get_y) == nullptr)) {
        return;
    }
    const int64_t down_time = get_down_time(event);
    const int32_t device_id = get_device_id(event);
    const int32_t source = get_source(event);
    const float y = ReadMotionY(event);

    if (is_down) {
        __atomic_store_n(&g_motion_snapshot_valid, uint32_t{0}, __ATOMIC_RELEASE);
        __atomic_store_n(&g_motion_snapshot_down_time, down_time, __ATOMIC_RELAXED);
        __atomic_store_n(&g_motion_snapshot_device_id, device_id, __ATOMIC_RELAXED);
        __atomic_store_n(&g_motion_snapshot_source, source, __ATOMIC_RELAXED);
        __atomic_store_n(&g_motion_snapshot_down_y_bits, FloatBits(y),
                         __ATOMIC_RELAXED);
        __atomic_store_n(&g_motion_snapshot_current_y_bits, FloatBits(y),
                         __ATOMIC_RELAXED);
        __atomic_store_n(&g_motion_snapshot_valid, uint32_t{1}, __ATOMIC_RELEASE);
        return;
    }
    if (__atomic_load_n(&g_motion_snapshot_valid, __ATOMIC_ACQUIRE) == 0u ||
            __atomic_load_n(&g_motion_snapshot_down_time, __ATOMIC_RELAXED) !=
                    down_time ||
            __atomic_load_n(&g_motion_snapshot_device_id, __ATOMIC_RELAXED) !=
                    device_id ||
            __atomic_load_n(&g_motion_snapshot_source, __ATOMIC_RELAXED) != source) {
        return;
    }
    __atomic_store_n(&g_motion_snapshot_current_y_bits, FloatBits(y),
                     __ATOMIC_RELAXED);
}

int32_t HookMotionEventGetActionMasked(void* event) {
    auto original = reinterpret_cast<MotionEventActionFn>(
            AtomicLoad(&g_original_motion_get_action_masked));
    if (original == nullptr) return 0;
    const int32_t action = original(event);
    PublishMotionSnapshot(event, action);
    return action;
}

/** Whether the tracked stream has already travelled upward far enough. */
bool HasUpwardGestureIntent() {
    if (__atomic_load_n(&g_motion_snapshot_valid, __ATOMIC_ACQUIRE) == 0u) {
        return false;
    }
    const float down_y = BitsFloat(
            __atomic_load_n(&g_motion_snapshot_down_y_bits, __ATOMIC_ACQUIRE));
    const float current_y = BitsFloat(
            __atomic_load_n(&g_motion_snapshot_current_y_bits, __ATOMIC_ACQUIRE));
    return (down_y - current_y) >= kUpwardIntentPixels;
}

bool IsExactName(const char* value, size_t length, const char* expected) {
    if (value == nullptr || expected == nullptr) return false;
    const size_t expected_length = ConstStringLength(expected);
    return length == expected_length &&
            memcmp(value, expected, expected_length) == 0;
}

bool IsNativeSuccess(const NativeResult& result) {
    return (result.bytes[0] & uint8_t{1}) == 0u;
}

/** Reports the two contextual-search features as present; passes the rest. */
NativeResult HookPackageManagerHasSystemFeature(void* package_manager,
                                                const char* name,
                                                size_t name_length,
                                                uint32_t flags) {
    auto original = reinterpret_cast<PackageManagerHasSystemFeatureFn>(
            AtomicLoad(&g_original_has_system_feature));
    if (original == nullptr) return NativeResult{};
    NativeResult result = original(package_manager, name, name_length, flags);
    const bool contextual_feature =
            IsExactName(name, name_length, kPlatformContextualSearchFeature) ||
            IsExactName(name, name_length, kGoogleContextualSearchFeature);
    if (!contextual_feature) return result;
    if (IsNativeSuccess(result) && result.bytes[1] == uint8_t{0}) {
        // Result<bool> in a 48-byte wrapper: byte 0 is the tag, byte 1 the
        // value. Leave errors and already-true results alone.
        result.bytes[1] = uint8_t{1};
    }
    return result;
}

bool InvokeContextualSearch(uint32_t entry_point) {
    auto invoke = reinterpret_cast<ContextualSearchInvokeFn>(
            AtomicLoad(&g_contextual_search_invoke));
    if (invoke == nullptr) {
        Log(ANDROID_LOG_WARN, "contextual-search invoke is unavailable");
        return false;
    }
    const uint8_t result = invoke(entry_point);
    if (result == 0u) {
        Log(ANDROID_LOG_WARN, "launcher rejected the contextual-search request");
        return false;
    }
    __android_log_print(ANDROID_LOG_INFO, kLogTag,
                        "requested contextual search entryPoint=%u result=%u",
                        static_cast<unsigned int>(entry_point),
                        static_cast<unsigned int>(result));
    return true;
}

bool CleanupLongPressClosure(void* closure) {
    if (closure == nullptr) return false;
    void* storage = *reinterpret_cast<void**>(
            reinterpret_cast<uint8_t*>(closure) + 0x8u);
    if (storage == nullptr) return true;
    void* owner = *reinterpret_cast<void**>(
            reinterpret_cast<uint8_t*>(closure) + 0x10u);
    if (owner == nullptr) return false;
    const uintptr_t object_size = *reinterpret_cast<uintptr_t*>(
            reinterpret_cast<uint8_t*>(owner) + 0x10u);
    ContextualClosureCleanupFn cleanup =
            *reinterpret_cast<ContextualClosureCleanupFn*>(
                    reinterpret_cast<uint8_t*>(owner) + 0x28u);
    if (object_size == 0u || cleanup == nullptr) return false;
    const uintptr_t aligned_offset =
            (object_size - 1u) & ~static_cast<uintptr_t>(0xfu);
    cleanup(reinterpret_cast<uint8_t*>(storage) + aligned_offset + 0x10u);
    return true;
}

/**
 * Replaces DefaultLongPressHandler's terminal Fn: keeps its completion marker,
 * swaps only the routing China builds reject. Falls back to the original on any
 * layout surprise.
 */
void HookLongPressHandler(void* closure, uint32_t trigger_mode) {
    ContextualLongPressHandlerFn original =
            reinterpret_cast<ContextualLongPressHandlerFn>(
                    AtomicLoad(&g_original_long_press_handler));

    void* completion_state =
            closure == nullptr
                    ? nullptr
                    : AtomicLoad(reinterpret_cast<void**>(closure));
    if (completion_state == nullptr) {
        if (original != nullptr) original(closure, trigger_mode);
        return;
    }

    // Check the closure tail the captured function itself relies on; on a
    // layout mismatch stay stock rather than risk an invalid release.
    void* closure_storage = *reinterpret_cast<void**>(
            reinterpret_cast<uint8_t*>(closure) + 0x8u);
    void* closure_owner = *reinterpret_cast<void**>(
            reinterpret_cast<uint8_t*>(closure) + 0x10u);
    if (closure_storage != nullptr &&
            (closure_owner == nullptr ||
             *reinterpret_cast<uintptr_t*>(
                     reinterpret_cast<uint8_t*>(closure_owner) + 0x10u) == 0u ||
             *reinterpret_cast<ContextualClosureCleanupFn*>(
                     reinterpret_cast<uint8_t*>(closure_owner) + 0x28u) ==
                     nullptr)) {
        if (original != nullptr) original(closure, trigger_mode);
        return;
    }

    // A swipe toward Home or Recents can dwell long enough to arm the same
    // detector. Finish it the way the original does, but without routing, so
    // the swipe still completes.
    if (HasUpwardGestureIntent()) {
        __atomic_store_n(static_cast<uint8_t*>(completion_state) + 0x10u,
                         uint8_t{1}, __ATOMIC_RELEASE);
        if (!CleanupLongPressClosure(closure)) {
            Log(ANDROID_LOG_WARN,
                "upward long-press cancellation cleanup unavailable");
        }
        return;
    }

    // Mark the captured detector state complete before routing, exactly as the
    // stock closure does, so the launcher's own detector is never left stale.
    __atomic_store_n(static_cast<uint8_t*>(completion_state) + 0x10u,
                     uint8_t{1}, __ATOMIC_RELEASE);
    InvokeContextualSearch(1u);
    if (!CleanupLongPressClosure(closure)) {
        // Unreachable after the validation above. Keep the native callback as
        // the safety net if the object changed concurrently.
        Log(ANDROID_LOG_WARN, "long-press closure cleanup unavailable");
        if (original != nullptr) original(closure, trigger_mode);
    }
}

void InstallLauncherHook(void* app_entry_point) {
    if (AtomicLoad(&g_original_long_press_handler) != nullptr) return;

    Dl_info info{};
    if (dladdr(app_entry_point, &info) == 0 || info.dli_fbase == nullptr) {
            Log(ANDROID_LOG_ERROR, "could not locate the launcher image base");
        return;
    }
    auto* base = static_cast<uint8_t*>(info.dli_fbase);

    micts_launcher::ContextualSearchProfile profile{};
    micts_launcher::ResolveDiagnostics diagnostics{};
    if (!micts_launcher::ResolveContextualSearchProfile(base, &profile,
                                                        &diagnostics)) {
            __android_log_print(
                ANDROID_LOG_ERROR, kLogTag,
                "launcher contextual-search graph rejected: stage=%u "
                "support=%u invoke=%u longPress=%u span=0x%lx",
                static_cast<unsigned int>(diagnostics.stage),
                diagnostics.support_candidate_count,
                diagnostics.invoke_candidate_count,
                diagnostics.long_press_candidate_count,
                static_cast<unsigned long>(diagnostics.image_span));
        return;
    }

    // The long-press hook consumes the gesture rather than forwarding it, so
    // both prerequisites must be in place first: without motion tracking a
    // swipe gets swallowed as a long press, and without the feature probe the
    // launcher refuses the request anyway.
    void* handle = AtomicLoad(&g_launcher_handle);
    if (handle != nullptr) {
        AtomicStore(&g_motion_get_down_time,
                    dlsym(handle, "input_MotionEvent_getDownTime"));
        AtomicStore(&g_motion_get_device_id,
                    dlsym(handle, "input_MotionEvent_getDeviceId"));
        AtomicStore(&g_motion_get_source,
                    dlsym(handle, "input_MotionEvent_getSource"));
        AtomicStore(&g_motion_get_raw_y,
                    dlsym(handle, "input_MotionEvent_getRawY"));
        AtomicStore(&g_motion_get_y, dlsym(handle, "input_MotionEvent_getY"));
    }
    const bool motion_accessors_ready =
            AtomicLoad(&g_motion_get_down_time) != nullptr &&
            AtomicLoad(&g_motion_get_device_id) != nullptr &&
            AtomicLoad(&g_motion_get_source) != nullptr &&
            (AtomicLoad(&g_motion_get_raw_y) != nullptr ||
             AtomicLoad(&g_motion_get_y) != nullptr);
    if (!motion_accessors_ready ||
            InstallPltHook(base, "input_MotionEvent_getActionMasked",
                           reinterpret_cast<void*>(HookMotionEventGetActionMasked),
                           &g_original_motion_get_action_masked) != kHookSuccess ||
            AtomicLoad(&g_original_motion_get_action_masked) == nullptr) {
        Log(ANDROID_LOG_ERROR,
            "motion tracking unavailable; leaving the launcher stock rather "
            "than swallowing swipes as long presses");
        return;
    }

    if (InstallPltHook(base, "PackageManager_has_system_feature",
                       reinterpret_cast<void*>(
                               HookPackageManagerHasSystemFeature),
                       &g_original_has_system_feature) != kHookSuccess ||
            AtomicLoad(&g_original_has_system_feature) == nullptr) {
        Log(ANDROID_LOG_ERROR,
            "contextual-search feature probe hook unavailable; leaving the "
            "launcher stock rather than swallowing an unusable long press");
        return;
    }

    // Publish the invoke helper before the hook goes live, so the very first
    // long press already has a route.
    AtomicStore(&g_contextual_search_invoke,
                static_cast<void*>(base + profile.invoke_offset));

    if (InstallInlineHook(base + profile.long_press_handler_offset,
                          reinterpret_cast<void*>(HookLongPressHandler),
                          &g_original_long_press_handler) != kHookSuccess ||
            AtomicLoad(&g_original_long_press_handler) == nullptr) {
        AtomicStore(&g_original_long_press_handler, static_cast<void*>(nullptr));
        AtomicStore(&g_contextual_search_invoke, static_cast<void*>(nullptr));
        Log(ANDROID_LOG_ERROR, "launcher long-press inline hook failed");
        return;
    }

    __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "installed launcher long-press hook: longPress=0x%lx invoke=0x%lx "
            "support=0x%lx base=%p",
            static_cast<unsigned long>(profile.long_press_handler_offset),
            static_cast<unsigned long>(profile.invoke_offset),
            static_cast<unsigned long>(profile.support_offset), base);
}

void OnLibraryLoaded(const char* name, void* handle) {
    if (name == nullptr || handle == nullptr) return;

    if (EndsWith(name, kHyperRuntimeName)) {
        EnsureLsposedMadviseGuard(name);
    }
    if (!IsLauncherLibraryPath(name) || !IsLauncherHookProcess()) return;

    // Xiaomi issues MADV_DONTNEED over launcher code pages; without the guard
    // an installed trampoline can be silently discarded.
    if (!EnsureLsposedMadviseGuard()) {
        Log(ANDROID_LOG_ERROR, "launcher hook rejected without madvise guard");
        return;
    }
    void* app_entry_point = dlsym(handle, kLauncherEntrySymbol);
    if (app_entry_point == nullptr) return;
    AtomicStore(&g_launcher_handle, handle);
    InstallLauncherHook(app_entry_point);
}

// The launcher can preload its images before native_init runs, and the
// callback only covers later loads. RTLD_NOLOAD: never pull in a new library.
void BackfillLoadedLibrary(const char* name) {
    if (name == nullptr) return;
    void* handle = dlopen(name, RTLD_NOW | RTLD_NOLOAD);
    if (handle == nullptr) return;
    __android_log_print(ANDROID_LOG_INFO, kLogTag,
                        "backfilling already-loaded native image: %s", name);
    OnLibraryLoaded(name, handle);
    dlclose(handle);
}

}  // namespace

extern "C" __attribute__((visibility("default"), unused))
NativeOnModuleLoaded native_init(const NativeAPIEntries* entries) {
    const bool backend_ready = InitializeLsposedHookBackend(entries);
    const bool hyos_process = IsHyosSpawnerProcessFamily();
    const bool launcher_process = IsLauncherProcess();
    __android_log_print(
            ANDROID_LOG_INFO, kLogTag,
            "native_init: backend=%u hyos_exe=%u launcher_cmdline=%u",
            backend_ready ? 1u : 0u, hyos_process ? 1u : 0u,
            launcher_process ? 1u : 0u);
    if (!backend_ready) return nullptr;
    if (!hyos_process) {
        Log(ANDROID_LOG_WARN, "native entry rejected a non-HYOS process");
        return nullptr;
    }
    Log(ANDROID_LOG_INFO, "MiCTS native launcher entry initialized");
    if (IsLauncherHookProcess()) {
        BackfillLoadedLibrary("libapp_launcher.so");
        BackfillLoadedLibrary(kHyperRuntimeName);
    }
    return OnLibraryLoaded;
}
