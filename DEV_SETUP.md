# BitWhisper dengan VS Code

VS Code dapat digunakan tanpa Android Studio, tetapi Android command-line toolchain tetap diperlukan.

## Toolchain

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35
- Android NDK 27.0.12077973
- CMake 3.22.1
- Gradle 8.9 atau Gradle Wrapper

## Extension VS Code

- Extension Pack for Java
- Kotlin Language
- Gradle for Java
- C/C++

## Environment variable

Linux/macOS:

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH
```

Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\\Program Files\\Java\\jdk-17"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\\Android\\Sdk"
```

## Build

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Whisper.cpp, llama.cpp, dan model GGUF masih perlu ditambahkan sebelum inference offline nyata aktif.

## PixelOperator font

Place `PixelOperator.ttf` as `app/src/main/res/font/pixeloperator.ttf` before using it from resources. The font can be obtained from https://www.dafont.com/pixel-operator.font; verify its license before redistribution.
