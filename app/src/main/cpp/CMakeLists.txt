cmake_minimum_required(VERSION 3.22.1)
project("bitwhisper_native")

# Both projects use ggml target names. Fetch llama first so its newer ggml
# implementation is the one linked by llama; whisper is then built against it.
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_COMMON OFF CACHE BOOL "" FORCE)

include(FetchContent)

FetchContent_Declare(llama
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG b9999
    GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(llama)
target_include_directories(llama PRIVATE ${llama_SOURCE_DIR}/ggml/include)

FetchContent_Declare(whisper
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG v1.9.4
    GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(whisper)

add_library(bitwhisper_native SHARED native-lib.cpp)
target_include_directories(bitwhisper_native PRIVATE
    ${whisper_SOURCE_DIR}/include
    ${llama_SOURCE_DIR}/include
    ${llama_SOURCE_DIR}/ggml/include)
target_link_libraries(bitwhisper_native PRIVATE whisper llama log)
target_compile_features(bitwhisper_native PRIVATE cxx_std_17)
