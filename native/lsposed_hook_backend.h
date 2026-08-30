// SPDX-License-Identifier: Apache-2.0
// Derived from MiuiBackGestureHook; see native/README.md for attribution.

#pragma once

#include <stddef.h>

#include "native_api.h"

constexpr int kHookSuccess = 0;
constexpr int kHookFailed = 1;

bool InitializeLsposedHookBackend(const NativeAPIEntries* entries);
bool EnsureLsposedMadviseGuard(const char* runtime_name = nullptr);
int InstallPltHook(void* base_addr, const char* symbol, void* hook_handler,
                   void** original);
int InstallInlineHook(void* target, void* replacement, void** original);
// Drops LSPosed's own record of an inline hook. Needed before reinstalling one
// whose patched text was discarded underneath it.
int RemoveInlineHook(void* target);
