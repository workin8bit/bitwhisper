cmake_minimum_required(VERSION 3.22.1)
project("bitwhisper_native")

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_COMMON OFF CACHE BOOL "" FORCE)
include(FetchContent)
FetchContent_Declare(whisper
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG v1.7.4
    GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(whisper)

add_library(bitwhisper_native SHARED native-lib.cpp)
target_include_directories(bitwhisper_native PRIVATE ${whisper_SOURCE_DIR}/include)
target_link_libraries(bitwhisper_native PRIVATE whisper log)
target_compile_features(bitwhisper_native PRIVATE cxx_std_17)
