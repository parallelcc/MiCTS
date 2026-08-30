// SPDX-License-Identifier: Apache-2.0
// Derived from MiuiBackGestureHook; see native/README.md for attribution.

#pragma once

#include <stdint.h>

struct NativeAPIEntries {
    uint32_t version;
    int (*hookFunc)(void* target, void* replacement, void** backup);
    int (*unhookFunc)(void* target);
};

using NativeOnModuleLoaded = void (*)(const char* name, void* handle);
