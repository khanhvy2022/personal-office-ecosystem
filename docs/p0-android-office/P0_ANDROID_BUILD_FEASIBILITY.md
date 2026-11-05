# P0 — ANDROID BUILD FEASIBILITY & WINDOWS/LINUX HYBRID STRATEGY
## EW Personal Office Ecosystem (`EW Documents` & `EW Sheets`)

**Project Root:** `F:\word và Excel`  
**Date:** 2026-11-04  
**Host OS Verified:** Microsoft Windows 10 IoT Enterprise LTSC (Version `10.0.19044`, Build `19044`)

---

## 1. EMPIRICAL AUDIT OF LOCAL WINDOWS HOST ENVIRONMENT

Executed via non-destructive PowerShell inspection on `2026-11-04T20:50:01+07:00`:

| Environment Component | Discovered State on Host | Path / Version Evidence |
|---|---|---|
| **Operating System** | Windows 10 IoT Enterprise LTSC 2021 | Build `19044` (`10.0.19044`) |
| **Target Drive `F:\`** | Available & Writable | `96.70 GB` free (`141.78 GB` used) |
| **Project Workspace** | Created & Verified | `F:\word và Excel` |
| **Git CLI** | Installed (Global PATH) | `git version 2.52.0.windows.1` |
| **Python** | Installed (Global PATH) | `Python 3.13.14` |
| **Portable JDK 17** | Installed in `F:\pdf\toolchain` | `F:\pdf\toolchain\jdk\jdk-17.0.20.1+1` |
| **Portable Gradle** | Installed in `F:\pdf\toolchain` | `F:\pdf\toolchain\gradle` (Gradle `8.10.2`) |
| **Portable Android SDK** | Installed in `F:\pdf\toolchain\android-sdk` | `platforms\android-35`, `build-tools\35.0.0`, `platform-tools\adb.exe`, `cmdline-tools` |
| **Android NDK (`29.0.14206865`)** | **Not yet installed** in `F:\pdf\toolchain\android-sdk\ndk` | Required for CMake JNI compilation if building `:lib` from C++ source |
| **Android Platform 37 / Build-Tools 37** | **Not yet installed** (`android-35` is present) | Upstream Collabora `android/app/build.gradle` uses `compileSdk 37` / `buildToolsVersion "37.0.0"` (can be aligned to API 35/36 or installed via `sdkmanager`) |
| **Physical Android 16 Device (`adb devices`)** | **No device currently attached** | `adb.exe devices -l` returned empty `List of devices attached` |
| **WSL / Windows System Changes** | **Untouched (0 modifications)** | Complies strictly with Section 7 & Section 12 constraints |

---

## 2. WHY COLLABORA NATIVE ENGINE CANNOT BE BUILT DIRECTLY ON WINDOWS WITHOUT LINUX

Per `CollaboraOnline/online.mirror/android/README.md` (lines 7–8) and `configure.ac` (lines 3075–3160):
1. **Autotools & `gbuild` POSIX Dependency:** Compiling `engine/` (`liblo-native-code.so`) requires GNU Autotools (`./autogen.sh`), GNU Make (`gmake`), `pkg-config`, POSIX shell pipelines, and Linux/macOS NDK cross-compilation toolchains (`aarch64-linux-android21-clang++`). Upstream explicitly states:
   > *"A computer running Linux or macOS. The native parts of the Android app cannot currently be built on Windows."*
2. **Symlink Creation in `configure.ac`:** Lines 3119–3160 of `configure.ac` create POSIX symbolic links (`ln -s "$LOBUILDDIR" lobuilddir-symlink`, `pocoinclude-symlink`, `pocolib-symlink`, `zstdinclude-symlink`, `zstdlib-symlink`, `ICU.dat`) and run `make -C browser DIST_FOLDER=...` (`generateCoolReleaseAssets` in `android/lib/build.gradle` lines 717–737).

---

## 3. OFFICIAL HYBRID BUILD STRATEGY (WINDOWS + LINUX CI / PREBUILT ARTIFACTS)

Because **installing WSL or modifying Windows system configuration is strictly prohibited**, we establish a two-tier reproducible build architecture that enables **100% of Kotlin, Jetpack Compose, UI, SAF, Room, and APK assembly work to execute natively on Windows 10 LTSC**:

### Tier 1 — Linux Build Machine / GitHub Actions CI (Engine Producer)
Used **only** when building or customizing `liblo-native-code.so`, `libandroidapp.so`, and `browser/dist` bundle from upstream Gerrit/GitHub source:
1. Clone `https://gerrit.collaboraoffice.com/online` (or `https://github.com/CollaboraOnline/online.mirror`).
2. Configure `engine/autogen.input`:
   ```ini
   --with-distro=CPAndroidAarch64
   --with-android-ndk=/opt/android-sdk/ndk/29.0.14206865
   --with-android-sdk=/opt/android-sdk
   --disable-branding
   ```
3. Build `engine/` (`make`) and `online` (`./autogen.sh --enable-androidapp --with-android-abi=arm64-v8a --disable-branding && make`).
4. Package the resulting `:lib` module into a standalone, signed, SHA-256 checksummed **Engine Distribution AAR** (`collabora-engine-arm64-26.04.x.aar`) containing:
   - `jni/arm64-v8a/libandroidapp.so`
   - `jni/arm64-v8a/liblo-native-code.so`
   - `jni/arm64-v8a/libc++_shared.so`
   - `jni/arm64-v8a/libfreebl3.so`, `libnspr4.so`, `libnss3.so`, `libnssckbi.so`, `libnssdbm3.so`, `libnssutil3.so`, `libplc4.so`, `libplds4.so`, `libsmime3.so`, `libsoftokn3.so`, `libsqlite3.so`, `libssl3.so`
   - `assets/dist/` (pre-built HTML/JS bundle so `make -C browser` is NOT needed on Windows!)
   - `assets/program/` & `assets/unpack/` & `assets/share/` (UNO `.rdb` types, services, filter configs, and OFL-1.1 fonts).

### Tier 2 — Fast Bootstrap Alternative for P1 Prototype (Extracting Official F-Droid `arm64-v8a` Binary Artifacts)
Because Collabora officially publishes reproducible, MPL-2.0 licensed `arm64-v8a` builds on its F-Droid repository (`https://www.collaboraoffice.com/downloads/fdroid/repo/index-v1.json`, currently `com.collabora.libreoffice` v`26.04.4.2`, fingerprint `573258C84E149B5F4D9299E7434B2B69A8410372921D4AE586BA91EC767892CC`):
- For **P1 Native Engine Prototype**, we can either:
  - **Path 1A (Linux CI Build):** Run a GitHub Actions Ubuntu workflow to compile `collabora-engine-arm64.aar` from source with `--disable-branding`.
  - **Path 1B (Prebuilt Binary Repackaging on Windows):** Download the official signed `arm64-v8a` release artifact from Collabora's F-Droid repo, extract the MPL-2.0 `lib/arm64-v8a/*.so` binaries and non-branded `assets/program`, `assets/share`, `assets/unpack`, and `assets/dist` (stripping `CollaboraOnline-branding` assets and GPL dictionaries), and wire them directly into our clean-room Windows Gradle library module `:engine:collabora-runtime` alongside the MPL-2.0 Java classes (`LOActivity`, `COWebView`, `COWebViewClient`, `MobileSocket`, `PrintAdapter`, `ClipboardFileProvider`).
- **Crucial Advantage of Path 1B:** Allows **100% local compilation of both APKs (`EW Documents` and `EW Sheets`) directly on this Windows 10 IoT Enterprise LTSC host** using the existing `F:\pdf\toolchain` (JDK 17 + Gradle 8.10.2 + Android SDK 35) without waiting 3 hours for a Linux C++ build!

---

## 4. TARGET DEVICE & ANDROID BASELINE VERIFICATION

| Target Requirement | Verification Status | Technical Evidence |
|---|---|---|
| **Architecture: `arm64-v8a`** | **VERIFIED (`PASS`)** | Supported via `--with-distro=CPAndroidAarch64` and `abiFilters += setOf("arm64-v8a")`. |
| **Baseline OS: Android 8.0+ (`minSdk 26`)** | **VERIFIED (`PASS`)** | Both Collabora (`android/app/build.gradle` line 336, `android/lib/build.gradle` line 453, and F-Droid `index-v1.json` `minSdkVersion: 26`) and Kern (`build.gradle.kts` line 18 `minSdk = 26`) explicitly support Android 8.0 (API 26) as their minimum SDK. |
| **Target OS: Android 16 (API 36/37)** | **PARTIAL (Build Config Verified; Physical Device Not Attached)** | Collabora `android/app/build.gradle` targets `targetSdk 37` / `compileSdk 37` (Android 16+), including edge-to-edge `WindowInsets.Type.systemBars() | WindowInsets.Type.ime() | WindowInsets.Type.systemOverlays()` handling in `LOActivity.java` (line 545). However, physical Android 16 phone testing via ADB could not be executed in P0 because `adb devices` shows no device connected. |

---

## 5. GATE G3 VERDICT

- **Gate G3 (Android Native Build Feasibility Verified):** **PARTIAL**
  - **Why PARTIAL instead of PASS:** Upstream Android `arm64-v8a` (`minSdk 26`, `targetSdk 37`) build configuration, CMake JNI linkage, and Windows portable Android SDK/JDK/Gradle toolchain (`F:\pdf\toolchain`) are verified with hard evidence. However, native C++ engine compilation from source requires a Linux build environment (or prebuilt binary extraction in P1), and no physical Android 16 phone is currently connected via ADB. Per Gate rules (*"Không chuyển PARTIAL thành PASS bằng suy luận"*), G3 is recorded honestly as **PARTIAL**.
