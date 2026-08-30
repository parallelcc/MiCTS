// SPDX-License-Identifier: Apache-2.0
// Derived from MiuiBackGestureHook; see native/README.md for attribution.

#pragma once

#include <stddef.h>
#include <stdint.h>

namespace micts_launcher {

enum class ResolveStage : uint32_t {
    kNotStarted = 0,
    kParsingElf = 1,
    kResolvingImports = 2,
    kResolvingContextualSearch = 3,
    kComplete = 4,
    kRejectedElf = 101,
    kRejectedImports = 102,
    kRejectedContextualSearch = 103,
};

struct ContextualSearchProfile {
    // Offsets are relative to the mapped launcher image base.
    uintptr_t long_press_handler_offset;
    uintptr_t invoke_offset;
    uintptr_t support_offset;
};

struct ResolveDiagnostics {
    ResolveStage stage;
    uint32_t support_candidate_count;
    uint32_t invoke_candidate_count;
    uint32_t long_press_candidate_count;
    uintptr_t image_span;
};

/**
  * Resolves the contextual-search graph out of the mapped `libapp_launcher.so`.
  * Every address is backed by a mapped executable segment and the expected
  * imported call graph, so an unknown build either resolves exactly or fails
  * closed. No version-specific offsets are involved.
  */
bool ResolveContextualSearchProfile(const uint8_t* base,
                                    ContextualSearchProfile* profile,
                                    ResolveDiagnostics* diagnostics);

}  // namespace micts_launcher
