// SPDX-License-Identifier: Apache-2.0
// Derived from MiuiBackGestureHook; see native/README.md for attribution.
//
// Locates the feature-support probe, the invoke helper and the long-press Fn
// closure in the mapped launcher image by matching code structure, not offsets.
// The ELF view, instruction decoders and fingerprints come from upstream's
// resolver; only its contextual-search family is kept.

#include "launcher_cs_resolver.h"

#include <elf.h>
#include <string.h>

namespace micts_launcher {
namespace {

constexpr size_t kMaxLoadSegments = 16u;
constexpr uintptr_t kMaxImageSpan = 0x4000000u;

constexpr char kBundleDrop[] = "Bundle_drop";
constexpr char kPackageManagerDefault[] = "PackageManager_default";
constexpr char kPackageManagerHasSystemFeature[] =
        "PackageManager_has_system_feature";

// Legacy shape (HyperOS 4 launcher 8.01.02.4371 family).
constexpr uint32_t kSupportPrologue[] = {
        0xd10383ffu, 0xa90c7bfdu, 0xa90d4ff4u, 0x910303fdu,
};
constexpr uint32_t kInvokePrologue[] = {
        0xd10543ffu, 0xa9117bfdu, 0xa9125ffcu, 0xa91357f6u,
        0xa9144ff4u, 0x910443fdu, 0x9100c3f6u, 0xb90007e0u,
};
constexpr uint32_t kLongPressPrologue[] = {
        0xd102c3ffu, 0xa9097bfdu, 0xa90a4ff4u, 0x910243fdu,
};

// Modern shape: 8.01.02.5465 and 8.01.02.6144 moved the contextual-search Rust
// graph to a smaller support helper and changed the Fn closure frame. These
// fingerprints stay structural — the resolver proves the two exact feature
// strings, the helper's PackageManager call, and the completion release store.
constexpr uint32_t kSupportModernPrologue[] = {
        0xd10243ffu, 0xa9067bfdu, 0xf9003bf5u, 0xa9084ff4u,
        0x910183fdu,
};
constexpr uint32_t kFeatureHelperModernPrologue[] = {
        0xd10203ffu, 0xa9067bfdu, 0xa9074ff4u, 0x910183fdu,
        0xaa0003f3u, 0x9100c3e8u,
};
constexpr uint32_t kInvokeModernPrologue[] = {
        0xd10403ffu, 0xa90c7bfdu, 0xf9006bf7u, 0xa90e57f6u,
        0xa90f4ff4u, 0x910303fdu, 0x2a0003f3u,
};
constexpr uint32_t kLongPressModernPrologue[] = {
        0xd10183ffu, 0xa9047bfdu, 0xa9054ff4u, 0x910103fdu,
};

struct LoadSegment {
    uintptr_t start;
    uintptr_t end;
    uint32_t flags;
};

struct ElfView {
    const uint8_t* base;
    LoadSegment loads[kMaxLoadSegments];
    size_t load_count;
    uintptr_t image_span;
    uintptr_t string_table;
    size_t string_table_size;
    uintptr_t symbol_table;
    uintptr_t jump_relocations;
    size_t jump_relocations_size;
};

struct RequiredImports {
    uintptr_t bundle_drop;
    uintptr_t package_manager_default;
    uintptr_t package_manager_has_system_feature;
};

bool AddOverflows(uintptr_t left, uintptr_t right) {
    return right > UINTPTR_MAX - left;
}

bool Contains(const ElfView& view, uintptr_t offset, size_t size,
              uint32_t required_flags, uint32_t forbidden_flags = 0u) {
    if (size == 0u || AddOverflows(offset, size)) return false;
    const uintptr_t end = offset + size;
    for (size_t index = 0u; index < view.load_count; ++index) {
        const LoadSegment& load = view.loads[index];
        if (offset >= load.start && end <= load.end &&
                (load.flags & required_flags) == required_flags &&
                (load.flags & forbidden_flags) == 0u) {
            return true;
        }
    }
    return false;
}

bool ReadInstruction(const ElfView& view, uintptr_t offset,
                     uint32_t* instruction) {
    if (instruction == nullptr ||
            !Contains(view, offset, sizeof(*instruction), PF_R | PF_X)) {
        return false;
    }
    memcpy(instruction, view.base + offset, sizeof(*instruction));
    return true;
}

bool InstructionEquals(const ElfView& view, uintptr_t offset,
                       uint32_t expected) {
    uint32_t instruction = 0u;
    return ReadInstruction(view, offset, &instruction) &&
            instruction == expected;
}

bool MatchesWords(const ElfView& view, uintptr_t offset,
                  const uint32_t* words, size_t count) {
    if (words == nullptr || count == 0u ||
            !Contains(view, offset, count * sizeof(uint32_t), PF_R | PF_X)) {
        return false;
    }
    return memcmp(view.base + offset, words, count * sizeof(uint32_t)) == 0;
}

bool ReadOnlyBytesEqual(const ElfView& view, uintptr_t offset,
                        const char* expected, size_t size) {
    return expected != nullptr &&
            Contains(view, offset, size, PF_R, PF_W) &&
            memcmp(view.base + offset, expected, size) == 0;
}

bool NormalizeDynamicPointer(const ElfView& view, Elf64_Addr value,
                             uintptr_t* offset) {
    if (offset == nullptr) return false;
    const uintptr_t raw = static_cast<uintptr_t>(value);
    const uintptr_t base_address = reinterpret_cast<uintptr_t>(view.base);
    if (raw >= base_address && raw - base_address < view.image_span) {
        *offset = raw - base_address;
        return true;
    }
    if (raw < view.image_span) {
        *offset = raw;
        return true;
    }
    return false;
}

bool ParseElf(const uint8_t* base, ElfView* output) {
    if (base == nullptr || output == nullptr) return false;
    Elf64_Ehdr header{};
    memcpy(&header, base, sizeof(header));
    if (memcmp(header.e_ident, ELFMAG, SELFMAG) != 0 ||
            header.e_ident[EI_CLASS] != ELFCLASS64 ||
            header.e_ident[EI_DATA] != ELFDATA2LSB ||
            header.e_type != ET_DYN || header.e_machine != EM_AARCH64 ||
            header.e_phentsize != sizeof(Elf64_Phdr) ||
            header.e_phnum == 0u || header.e_phnum > 64u ||
            header.e_phoff > 0x1000u ||
            AddOverflows(header.e_phoff,
                         static_cast<uintptr_t>(header.e_phnum) *
                                 sizeof(Elf64_Phdr)) ||
            header.e_phoff + static_cast<uintptr_t>(header.e_phnum) *
                            sizeof(Elf64_Phdr) > 0x1000u) {
        return false;
    }

    ElfView view{};
    view.base = base;
    const auto* program_headers =
            reinterpret_cast<const Elf64_Phdr*>(base + header.e_phoff);
    uintptr_t dynamic_offset = 0u;
    size_t dynamic_size = 0u;
    bool headers_covered = false;
    const uintptr_t headers_end = header.e_phoff +
            static_cast<uintptr_t>(header.e_phnum) * sizeof(Elf64_Phdr);
    for (size_t index = 0u; index < header.e_phnum; ++index) {
        Elf64_Phdr program_header{};
        memcpy(&program_header, program_headers + index,
               sizeof(program_header));
        if (program_header.p_type == PT_LOAD) {
            if (view.load_count >= kMaxLoadSegments ||
                    program_header.p_memsz == 0u ||
                    AddOverflows(program_header.p_vaddr,
                                 program_header.p_memsz)) {
                return false;
            }
            const uintptr_t start = program_header.p_vaddr;
            const uintptr_t end = start + program_header.p_memsz;
            if (end > kMaxImageSpan) return false;
            view.loads[view.load_count++] = {start, end, program_header.p_flags};
            if (start == 0u && headers_end <= program_header.p_filesz) {
                headers_covered = true;
            }
            if (end > view.image_span) view.image_span = end;
        } else if (program_header.p_type == PT_DYNAMIC) {
            if (program_header.p_memsz == 0u) return false;
            dynamic_offset = program_header.p_vaddr;
            dynamic_size = static_cast<size_t>(program_header.p_memsz);
        }
    }
    if (!headers_covered || view.load_count == 0u || view.image_span == 0u ||
            dynamic_size < sizeof(Elf64_Dyn) ||
            !Contains(view, dynamic_offset, dynamic_size, PF_R)) {
        return false;
    }

    Elf64_Addr string_table = 0u;
    Elf64_Addr symbol_table = 0u;
    Elf64_Addr jump_relocations = 0u;
    size_t string_table_size = 0u;
    size_t symbol_entry_size = 0u;
    size_t jump_relocations_size = 0u;
    size_t relocation_entry_size = sizeof(Elf64_Rela);
    Elf64_Sxword relocation_kind = 0;
    bool terminated = false;
    const size_t dynamic_count = dynamic_size / sizeof(Elf64_Dyn);
    for (size_t index = 0u; index < dynamic_count; ++index) {
        Elf64_Dyn entry{};
        memcpy(&entry, view.base + dynamic_offset + index * sizeof(Elf64_Dyn),
               sizeof(entry));
        if (entry.d_tag == DT_NULL) {
            terminated = true;
            break;
        }
        switch (entry.d_tag) {
            case DT_STRTAB: string_table = entry.d_un.d_ptr; break;
            case DT_STRSZ: string_table_size = entry.d_un.d_val; break;
            case DT_SYMTAB: symbol_table = entry.d_un.d_ptr; break;
            case DT_SYMENT: symbol_entry_size = entry.d_un.d_val; break;
            case DT_JMPREL: jump_relocations = entry.d_un.d_ptr; break;
            case DT_PLTRELSZ: jump_relocations_size = entry.d_un.d_val; break;
            case DT_PLTREL: relocation_kind = entry.d_un.d_val; break;
            case DT_RELAENT: relocation_entry_size = entry.d_un.d_val; break;
            default: break;
        }
    }
    if (!terminated || string_table_size == 0u ||
            symbol_entry_size != sizeof(Elf64_Sym) ||
            relocation_kind != DT_RELA ||
            relocation_entry_size != sizeof(Elf64_Rela) ||
            jump_relocations_size == 0u ||
            jump_relocations_size % sizeof(Elf64_Rela) != 0u ||
            !NormalizeDynamicPointer(view, string_table, &view.string_table) ||
            !NormalizeDynamicPointer(view, symbol_table, &view.symbol_table) ||
            !NormalizeDynamicPointer(view, jump_relocations,
                                     &view.jump_relocations)) {
        return false;
    }
    view.string_table_size = string_table_size;
    view.jump_relocations_size = jump_relocations_size;
    if (!Contains(view, view.string_table, view.string_table_size, PF_R) ||
            !Contains(view, view.symbol_table, sizeof(Elf64_Sym), PF_R) ||
            !Contains(view, view.jump_relocations, view.jump_relocations_size,
                      PF_R)) {
        return false;
    }
    *output = view;
    return true;
}

bool BoundedStringEquals(const char* value, size_t available,
                         const char* expected) {
    if (value == nullptr || expected == nullptr) return false;
    size_t index = 0u;
    while (expected[index] != '\0') {
        if (index >= available || value[index] != expected[index]) return false;
        ++index;
    }
    return index < available && value[index] == '\0';
}

/** Returns the sole JUMP_SLOT GOT offset for an exact imported symbol name. */
bool FindImportGot(const ElfView& view, const char* expected,
                   uintptr_t* result) {
    if (result == nullptr) return false;
    uintptr_t matched = 0u;
    uint32_t match_count = 0u;
    const size_t count = view.jump_relocations_size / sizeof(Elf64_Rela);
    for (size_t index = 0u; index < count; ++index) {
        Elf64_Rela relocation{};
        memcpy(&relocation,
               view.base + view.jump_relocations + index * sizeof(Elf64_Rela),
               sizeof(relocation));
        const size_t symbol_index = ELF64_R_SYM(relocation.r_info);
        if (symbol_index > (SIZE_MAX - view.symbol_table) / sizeof(Elf64_Sym)) {
            return false;
        }
        const uintptr_t symbol_offset =
                view.symbol_table + symbol_index * sizeof(Elf64_Sym);
        if (!Contains(view, symbol_offset, sizeof(Elf64_Sym), PF_R)) {
            return false;
        }
        Elf64_Sym symbol{};
        memcpy(&symbol, view.base + symbol_offset, sizeof(symbol));
        if (symbol.st_name >= view.string_table_size) return false;
        const char* name = reinterpret_cast<const char*>(
                view.base + view.string_table + symbol.st_name);
        if (!BoundedStringEquals(name, view.string_table_size - symbol.st_name,
                                 expected)) {
            continue;
        }
        uintptr_t got_offset = 0u;
        if (!NormalizeDynamicPointer(view, relocation.r_offset, &got_offset) ||
                !Contains(view, got_offset, sizeof(uintptr_t), PF_R)) {
            return false;
        }
        matched = got_offset;
        ++match_count;
    }
    if (match_count != 1u) return false;
    *result = matched;
    return true;
}

bool ResolveImports(const ElfView& view, RequiredImports* imports) {
    return imports != nullptr &&
            FindImportGot(view, kBundleDrop, &imports->bundle_drop) &&
            FindImportGot(view, kPackageManagerDefault,
                          &imports->package_manager_default) &&
            FindImportGot(view, kPackageManagerHasSystemFeature,
                          &imports->package_manager_has_system_feature);
}

bool DecodeAdrp(uint32_t instruction, uintptr_t pc, uint32_t reg,
                uintptr_t* target_page) {
    if (target_page == nullptr || reg > 31u ||
            (instruction & 0x9f00001fu) != (0x90000000u | reg)) {
        return false;
    }
    int64_t immediate = static_cast<int64_t>(
            ((instruction >> 29u) & 0x3u) |
            (((instruction >> 5u) & 0x7ffffu) << 2u));
    if ((immediate & (int64_t{1} << 20u)) != 0) {
        immediate -= int64_t{1} << 21u;
    }
    const int64_t page = static_cast<int64_t>(pc & ~uintptr_t{0xfffu});
    const int64_t target = page + immediate * int64_t{4096};
    if (target < 0 || static_cast<uint64_t>(target) > UINTPTR_MAX) return false;
    *target_page = static_cast<uintptr_t>(target);
    return true;
}

bool DecodeAddImmediate(uint32_t instruction, uint32_t destination,
                        uint32_t source, uintptr_t* immediate) {
    if (immediate == nullptr ||
            (instruction & 0xffc003ffu) !=
                    (0x91000000u | (source << 5u) | destination)) {
        return false;
    }
    *immediate = (instruction >> 10u) & 0xfffu;
    return true;
}

bool DecodeLdr64Immediate(uint32_t instruction, uint32_t destination,
                          uint32_t source, uintptr_t* immediate) {
    if (immediate == nullptr ||
            (instruction & 0xffc003ffu) !=
                    (0xf9400000u | (source << 5u) | destination)) {
        return false;
    }
    *immediate = ((instruction >> 10u) & 0xfffu) * sizeof(uintptr_t);
    return true;
}

bool DecodeAddressPair(const ElfView& view, uintptr_t instruction_offset,
                       uint32_t reg, uintptr_t* target) {
    uint32_t adrp = 0u;
    uint32_t add = 0u;
    uintptr_t page = 0u;
    uintptr_t immediate = 0u;
    return ReadInstruction(view, instruction_offset, &adrp) &&
            ReadInstruction(view, instruction_offset + 4u, &add) &&
            DecodeAdrp(adrp, instruction_offset, reg, &page) &&
            DecodeAddImmediate(add, reg, reg, &immediate) &&
            !AddOverflows(page, immediate) &&
            ((*target = page + immediate), true);
}

bool DecodeBlTarget(const ElfView& view, uintptr_t instruction_offset,
                    uintptr_t* target) {
    uint32_t instruction = 0u;
    if (target == nullptr ||
            !ReadInstruction(view, instruction_offset, &instruction) ||
            (instruction & 0xfc000000u) != 0x94000000u) {
        return false;
    }
    int64_t immediate = instruction & 0x03ffffffu;
    if ((immediate & (int64_t{1} << 25u)) != 0) {
        immediate -= int64_t{1} << 26u;
    }
    const int64_t destination =
            static_cast<int64_t>(instruction_offset) + immediate * int64_t{4};
    if (destination < 0 || static_cast<uint64_t>(destination) > UINTPTR_MAX ||
            !Contains(view, static_cast<uintptr_t>(destination), 16u,
                      PF_R | PF_X)) {
        return false;
    }
    *target = static_cast<uintptr_t>(destination);
    return true;
}

bool DecodePltGot(const ElfView& view, uintptr_t plt_offset,
                  uintptr_t* got_offset) {
    uint32_t adrp = 0u;
    uint32_t ldr = 0u;
    uint32_t add = 0u;
    uint32_t branch = 0u;
    uintptr_t page = 0u;
    uintptr_t load_immediate = 0u;
    uintptr_t add_immediate = 0u;
    return got_offset != nullptr &&
            ReadInstruction(view, plt_offset, &adrp) &&
            ReadInstruction(view, plt_offset + 4u, &ldr) &&
            ReadInstruction(view, plt_offset + 8u, &add) &&
            ReadInstruction(view, plt_offset + 12u, &branch) &&
            DecodeAdrp(adrp, plt_offset, 16u, &page) &&
            DecodeLdr64Immediate(ldr, 17u, 16u, &load_immediate) &&
            DecodeAddImmediate(add, 16u, 16u, &add_immediate) &&
            load_immediate == add_immediate && branch == 0xd61f0220u &&
            !AddOverflows(page, load_immediate) &&
            ((*got_offset = page + load_immediate), true);
}

bool CallTargetsImport(const ElfView& view, uintptr_t call_offset,
                       uintptr_t expected_got) {
    uintptr_t plt = 0u;
    uintptr_t got = 0u;
    return DecodeBlTarget(view, call_offset, &plt) &&
            DecodePltGot(view, plt, &got) && got == expected_got;
}

bool IsFeatureHelperModern(const ElfView& view, const RequiredImports& imports,
                           uintptr_t offset) {
    return MatchesWords(view, offset, kFeatureHelperModernPrologue,
                        sizeof(kFeatureHelperModernPrologue) /
                                sizeof(kFeatureHelperModernPrologue[0])) &&
            InstructionEquals(view, offset + 0x18u, 0xaa0103e0u) &&
            InstructionEquals(view, offset + 0x1cu, 0xaa0203e1u) &&
            InstructionEquals(view, offset + 0x20u, 0xaa0303e2u) &&
            InstructionEquals(view, offset + 0x24u, 0x2a1f03e3u) &&
            CallTargetsImport(view, offset + 0x28u,
                              imports.package_manager_has_system_feature);
}

bool IsSupportModernCandidate(const ElfView& view,
                              const RequiredImports& imports,
                              uintptr_t offset) {
    uintptr_t first_string = 0u;
    uintptr_t second_string = 0u;
    uintptr_t helper = 0u;
    return MatchesWords(view, offset, kSupportModernPrologue,
                        sizeof(kSupportModernPrologue) /
                                sizeof(kSupportModernPrologue[0])) &&
            CallTargetsImport(view, offset + 0x14u,
                              imports.package_manager_default) &&
            DecodeAddressPair(view, offset + 0x1cu, 2u, &first_string) &&
            ReadOnlyBytesEqual(view, first_string,
                               "android.software.contextualsearch", 33u) &&
            DecodeBlTarget(view, offset + 0x30u, &helper) &&
            DecodeBlTarget(view, offset + 0x70u, &first_string) &&
            first_string == helper &&
            IsFeatureHelperModern(view, imports, helper) &&
            DecodeAddressPair(view, offset + 0x58u, 2u, &second_string) &&
            ReadOnlyBytesEqual(view, second_string,
                               "com.google.android.feature.CONTEXTUAL_SEARCH",
                               44u);
}

bool IsSupportCandidate(const ElfView& view, const RequiredImports& imports,
                        uintptr_t offset) {
    uintptr_t first_string_page = 0u;
    uintptr_t first_string_immediate = 0u;
    uintptr_t second_string_page = 0u;
    uintptr_t second_string_immediate = 0u;
    uint32_t instruction = 0u;
    const bool legacy =
            MatchesWords(view, offset, kSupportPrologue,
                         sizeof(kSupportPrologue) /
                                 sizeof(kSupportPrologue[0])) &&
            CallTargetsImport(view, offset + 0x10u,
                              imports.package_manager_default) &&
            ReadInstruction(view, offset + 0x14u, &instruction) &&
            DecodeAdrp(instruction, offset + 0x14u, 1u, &first_string_page) &&
            ReadInstruction(view, offset + 0x18u, &instruction) &&
            DecodeAddImmediate(instruction, 1u, 1u, &first_string_immediate) &&
            !AddOverflows(first_string_page, first_string_immediate) &&
            Contains(view, first_string_page + first_string_immediate, 33u,
                     PF_R, PF_X) &&
            InstructionEquals(view, offset + 0x1cu, 0x910143e8u) &&
            InstructionEquals(view, offset + 0x20u, 0x52800422u) &&
            InstructionEquals(view, offset + 0x24u, 0x2a1f03e3u) &&
            InstructionEquals(view, offset + 0x28u, 0xaa0003f3u) &&
            CallTargetsImport(view, offset + 0x2cu,
                              imports.package_manager_has_system_feature) &&
            ReadInstruction(view, offset + 0xb4u, &instruction) &&
            DecodeAdrp(instruction, offset + 0xb4u, 1u, &second_string_page) &&
            ReadInstruction(view, offset + 0xb8u, &instruction) &&
            DecodeAddImmediate(instruction, 1u, 1u, &second_string_immediate) &&
            !AddOverflows(second_string_page, second_string_immediate) &&
            Contains(view, second_string_page + second_string_immediate, 44u,
                     PF_R, PF_X) &&
            InstructionEquals(view, offset + 0xc0u, 0x910143e8u) &&
            InstructionEquals(view, offset + 0xc4u, 0xaa1303e0u) &&
            InstructionEquals(view, offset + 0xc8u, 0x52800582u) &&
            InstructionEquals(view, offset + 0xccu, 0x2a1f03e3u) &&
            CallTargetsImport(view, offset + 0xd0u,
                              imports.package_manager_has_system_feature);
    return legacy || IsSupportModernCandidate(view, imports, offset);
}

bool ResolveSupport(const ElfView& view, const RequiredImports& imports,
                    uintptr_t* support_offset, uint32_t* candidate_count) {
    uintptr_t matched = 0u;
    uint32_t matches = 0u;
    for (size_t segment_index = 0u; segment_index < view.load_count;
         ++segment_index) {
        const LoadSegment& load = view.loads[segment_index];
        if ((load.flags & (PF_R | PF_X)) != (PF_R | PF_X) ||
                load.end - load.start < 0xd4u) {
            continue;
        }
        const uintptr_t start = (load.start + 3u) & ~uintptr_t{3u};
        for (uintptr_t offset = start; offset <= load.end - 0xd4u;
             offset += 4u) {
            if (!IsSupportCandidate(view, imports, offset)) continue;
            matched = offset;
            ++matches;
        }
    }
    if (candidate_count != nullptr) *candidate_count = matches;
    if (matches != 1u || support_offset == nullptr) return false;
    *support_offset = matched;
    return true;
}

bool IsInvokeCandidate(const ElfView& view, uintptr_t offset,
                       uintptr_t support_offset) {
    uintptr_t called_support = 0u;
    uint32_t branch = 0u;
    const bool legacy =
            MatchesWords(view, offset, kInvokePrologue,
                         sizeof(kInvokePrologue) /
                                 sizeof(kInvokePrologue[0])) &&
            DecodeBlTarget(view, offset + 0x20u, &called_support) &&
            called_support == support_offset &&
            ReadInstruction(view, offset + 0x28u, &branch) &&
            (branch & 0xfff8001fu) == 0x36000000u;
    if (legacy) return true;
    return MatchesWords(view, offset, kInvokeModernPrologue,
                        sizeof(kInvokeModernPrologue) /
                                sizeof(kInvokeModernPrologue[0])) &&
            InstructionEquals(view, offset + 0x1cu, 0x9100e3f4u) &&
            InstructionEquals(view, offset + 0x20u, 0xb9000fe0u) &&
            DecodeBlTarget(view, offset + 0x24u, &called_support) &&
            called_support == support_offset;
}

bool ResolveInvoke(const ElfView& view, uintptr_t support_offset,
                   uintptr_t* invoke_offset, uint32_t* candidate_count) {
    uintptr_t matched = 0u;
    uint32_t matches = 0u;
    for (size_t segment_index = 0u; segment_index < view.load_count;
         ++segment_index) {
        const LoadSegment& load = view.loads[segment_index];
        if ((load.flags & (PF_R | PF_X)) != (PF_R | PF_X) ||
                load.end - load.start < 0x2cu) {
            continue;
        }
        const uintptr_t start = (load.start + 3u) & ~uintptr_t{3u};
        for (uintptr_t offset = start; offset <= load.end - 0x2cu;
             offset += 4u) {
            if (!IsInvokeCandidate(view, offset, support_offset)) continue;
            matched = offset;
            ++matches;
        }
    }
    if (candidate_count != nullptr) *candidate_count = matches;
    if (matches != 1u || invoke_offset == nullptr) return false;
    *invoke_offset = matched;
    return true;
}

bool IsLongPressCandidate(const ElfView& view, const RequiredImports& imports,
                          uintptr_t offset) {
    uintptr_t fallback = 0u;
    uint32_t branch = 0u;
    const bool legacy =
            MatchesWords(view, offset, kLongPressPrologue,
                         sizeof(kLongPressPrologue) /
                                 sizeof(kLongPressPrologue[0])) &&
            InstructionEquals(view, offset + 0x14u, 0xaa0003f3u) &&
            InstructionEquals(view, offset + 0x58u, 0x2a0103f4u) &&
            InstructionEquals(view, offset + 0xe0u, 0xd63f0100u) &&
            InstructionEquals(view, offset + 0xe4u, 0x2a1403e1u) &&
            InstructionEquals(view, offset + 0xe8u, 0xf9400268u) &&
            InstructionEquals(view, offset + 0xecu, 0x52800029u) &&
            InstructionEquals(view, offset + 0xf0u, 0x91004108u) &&
            InstructionEquals(view, offset + 0xf4u, 0x089ffd09u) &&
            InstructionEquals(view, offset + 0xf8u, 0xf9400668u) &&
            ReadInstruction(view, offset + 0xfcu, &branch) &&
            (branch & 0xff00001fu) == 0xb4000008u &&
            InstructionEquals(view, offset + 0x100u, 0xf9400a69u) &&
            InstructionEquals(view, offset + 0x104u, 0xf940092au) &&
            InstructionEquals(view, offset + 0x108u, 0xf9401529u) &&
            InstructionEquals(view, offset + 0x10cu, 0xd100054au) &&
            InstructionEquals(view, offset + 0x110u, 0x927ced4au) &&
            InstructionEquals(view, offset + 0x114u, 0x8b0a0108u) &&
            InstructionEquals(view, offset + 0x118u, 0x91004100u) &&
            InstructionEquals(view, offset + 0x11cu, 0xd63f0120u) &&
            DecodeBlTarget(view, offset + 0x134u, &fallback) &&
            fallback != offset &&
            ReadInstruction(view, offset + 0x138u, &branch) &&
            (branch & 0xff00001fu) == 0xb4000000u &&
            CallTargetsImport(view, offset + 0x13cu, imports.bundle_drop);
    if (legacy) return true;

    // Newer DefaultLongPressHandler keeps the same captured-completion
    // protocol but uses a compact frame and invokes XiaoAi before the
    // Bundle_drop fallback. Match the release store and closure dispatch as
    // one contiguous graph; this excludes unrelated FnOnce shims.
    uintptr_t xiaoai = 0u;
    return MatchesWords(view, offset, kLongPressModernPrologue,
                        sizeof(kLongPressModernPrologue) /
                                sizeof(kLongPressModernPrologue[0])) &&
            InstructionEquals(view, offset + 0x14u, 0x2a0103f3u) &&
            InstructionEquals(view, offset + 0x18u, 0xaa0003f4u) &&
            InstructionEquals(view, offset + 0x74u, 0xf9400288u) &&
            InstructionEquals(view, offset + 0x78u, 0x52800029u) &&
            InstructionEquals(view, offset + 0x7cu, 0x91004108u) &&
            InstructionEquals(view, offset + 0x80u, 0x089ffd09u) &&
            InstructionEquals(view, offset + 0x84u, 0xf9400688u) &&
            ReadInstruction(view, offset + 0x88u, &branch) &&
            (branch & 0xff00001fu) == 0xb4000008u &&
            InstructionEquals(view, offset + 0x8cu, 0xf9400a89u) &&
            InstructionEquals(view, offset + 0x90u, 0x2a1303e1u) &&
            InstructionEquals(view, offset + 0x94u, 0xf940092au) &&
            InstructionEquals(view, offset + 0x98u, 0xf9401529u) &&
            InstructionEquals(view, offset + 0x9cu, 0xd100054au) &&
            InstructionEquals(view, offset + 0xa0u, 0x927ced4au) &&
            InstructionEquals(view, offset + 0xa4u, 0x8b0a0108u) &&
            InstructionEquals(view, offset + 0xa8u, 0x91004100u) &&
            InstructionEquals(view, offset + 0xacu, 0xd63f0120u) &&
            DecodeBlTarget(view, offset + 0xb8u, &xiaoai) && xiaoai != offset &&
            ReadInstruction(view, offset + 0xbcu, &branch) &&
            (branch & 0xff00001fu) == 0xb4000000u &&
            CallTargetsImport(view, offset + 0xc0u, imports.bundle_drop);
}

bool ResolveLongPress(const ElfView& view, const RequiredImports& imports,
                      uintptr_t* handler_offset, uint32_t* candidate_count) {
    uintptr_t matched = 0u;
    uint32_t matches = 0u;
    for (size_t segment_index = 0u; segment_index < view.load_count;
         ++segment_index) {
        const LoadSegment& load = view.loads[segment_index];
        if ((load.flags & (PF_R | PF_X)) != (PF_R | PF_X) ||
                load.end - load.start < 0x140u) {
            continue;
        }
        const uintptr_t start = (load.start + 3u) & ~uintptr_t{3u};
        for (uintptr_t offset = start; offset <= load.end - 0x140u;
             offset += 4u) {
            if (!IsLongPressCandidate(view, imports, offset)) continue;
            matched = offset;
            ++matches;
        }
    }
    if (candidate_count != nullptr) *candidate_count = matches;
    if (matches != 1u || handler_offset == nullptr) return false;
    *handler_offset = matched;
    return true;
}

}  // namespace

bool ResolveContextualSearchProfile(const uint8_t* base,
                                    ContextualSearchProfile* profile,
                                    ResolveDiagnostics* diagnostics) {
    ResolveDiagnostics local{};
    local.stage = ResolveStage::kParsingElf;
    if (diagnostics != nullptr) *diagnostics = local;
    if (base == nullptr || profile == nullptr) return false;

    ElfView view{};
    if (!ParseElf(base, &view)) {
        local.stage = ResolveStage::kRejectedElf;
        if (diagnostics != nullptr) *diagnostics = local;
        return false;
    }
    local.image_span = view.image_span;

    local.stage = ResolveStage::kResolvingImports;
    RequiredImports imports{};
    if (!ResolveImports(view, &imports)) {
        local.stage = ResolveStage::kRejectedImports;
        if (diagnostics != nullptr) *diagnostics = local;
        return false;
    }

    local.stage = ResolveStage::kResolvingContextualSearch;
    uintptr_t support = 0u;
    uintptr_t invoke = 0u;
    uintptr_t handler = 0u;
    const bool resolved =
            ResolveSupport(view, imports, &support,
                           &local.support_candidate_count) &&
            ResolveInvoke(view, support, &invoke,
                          &local.invoke_candidate_count) &&
            ResolveLongPress(view, imports, &handler,
                             &local.long_press_candidate_count);
    if (!resolved) {
        local.stage = ResolveStage::kRejectedContextualSearch;
        if (diagnostics != nullptr) *diagnostics = local;
        return false;
    }

    profile->support_offset = support;
    profile->invoke_offset = invoke;
    profile->long_press_handler_offset = handler;
    local.stage = ResolveStage::kComplete;
    if (diagnostics != nullptr) *diagnostics = local;
    return true;
}

}  // namespace micts_launcher
