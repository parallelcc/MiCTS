cmake_minimum_required(VERSION 3.22)

project(micts_hyos_native LANGUAGES CXX)

if(NOT ANDROID)
    message(FATAL_ERROR "micts_hyos_native must use the Android NDK toolchain")
endif()

if(NOT "${ANDROID_ABI}" STREQUAL "arm64-v8a")
    message(FATAL_ERROR "Only arm64-v8a is supported; got '${ANDROID_ABI}'")
endif()

set(MICTS_NATIVE_TARGET micts_hyos_lsp)

add_library(${MICTS_NATIVE_TARGET} SHARED
    micts_native_hook.cpp
    launcher_cs_resolver.cpp
    lsposed_hook_backend.cpp
)

target_include_directories(${MICTS_NATIVE_TARGET} PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_features(${MICTS_NATIVE_TARGET} PRIVATE cxx_std_17)
target_compile_options(${MICTS_NATIVE_TARGET} PRIVATE
    -fno-exceptions
    -fno-rtti
    -fno-threadsafe-statics
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -mbranch-protection=standard
    -Wall
    -Wextra
    -Werror
)

target_link_options(${MICTS_NATIVE_TARGET} PRIVATE
    # Android 16+ warns about APKs whose native libraries are not 16 KB page
    # aligned; be explicit rather than relying on the NDK default.
    -Wl,-z,max-page-size=16384
    "-Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/exports-lsposed.map"
    -Wl,--gc-sections
    -Wl,--no-undefined
    -Wl,-z,relro
    -Wl,-z,now
)

target_link_libraries(${MICTS_NATIVE_TARGET} PRIVATE log dl)

set_target_properties(${MICTS_NATIVE_TARGET} PROPERTIES
    CXX_EXTENSIONS OFF
    OUTPUT_NAME "${MICTS_NATIVE_TARGET}"
)
