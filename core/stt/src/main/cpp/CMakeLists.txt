cmake_minimum_required(VERSION 3.10)
# debug APK에서도 whisper·ggml은 최적화 빌드 (-O0이면 수십 배 느려 측정 불가)
set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
project(opic_whisper)

set(CMAKE_CXX_STANDARD 17)
# core/stt/src/main/cpp → 저장소 루트의 third_party/whisper.cpp (v1.9.5, VERSION.md 참고)
set(WHISPER_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../../third_party/whisper.cpp)

set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ARM_ARCH armv8.2-a+fp16+dotprod CACHE STRING "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_library(opic_whisper SHARED
    ${WHISPER_LIB_DIR}/src/whisper.cpp
    ${WHISPER_LIB_DIR}/src/aneforge/whisper-aneforge.cpp
    ${CMAKE_SOURCE_DIR}/whisper_jni.c
)
target_compile_definitions(opic_whisper PUBLIC GGML_USE_CPU)
target_compile_definitions(opic_whisper PRIVATE WHISPER_VERSION="1.9.5")

# S23+(Snapdragon 8 Gen 2)는 fp16·dotprod 지원
set(OPIC_ARCH -march=armv8.2-a+fp16+dotprod)
target_compile_options(opic_whisper PRIVATE ${OPIC_ARCH} -O3 -fvisibility=hidden)
target_link_options(opic_whisper PRIVATE -Wl,--gc-sections)

include(FetchContent)
FetchContent_Declare(ggml SOURCE_DIR ${WHISPER_LIB_DIR}/ggml)
FetchContent_MakeAvailable(ggml)
target_compile_options(ggml PRIVATE ${OPIC_ARCH} -O3)

find_library(LOG_LIB log)
target_link_libraries(opic_whisper ${LOG_LIB} android ggml)

target_include_directories(opic_whisper PRIVATE
    ${WHISPER_LIB_DIR}/include
    ${WHISPER_LIB_DIR}/src
    ${WHISPER_LIB_DIR}/ggml/include
)
