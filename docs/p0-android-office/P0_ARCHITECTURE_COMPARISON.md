# P0 — ARCHITECTURE COMPARISON
## EW Personal Office Ecosystem (`EW Documents` & `EW Sheets`)

**Project Root:** `F:\word và Excel`  
**Date:** 2026-11-04  
**Phase:** P0 — Research & Feasibility Only

---

## 1. MANDATORY ARCHITECTURAL CONSTRAINTS

Per Section 2 of the P0 Specification, the architecture MUST satisfy:
1. **Two Strictly Independent Apps:**
   - **App 01:** `EW Documents` (`com.eurowindow.documents`) — Android DOCX Reader & Editor.
   - **App 02:** `EW Sheets` (`com.eurowindow.sheets`) — Android XLSX Reader & Editor.
2. **Full Runtime Isolation:**
   - Separate Application IDs, Launcher Icons, Main Activities, Navigation Graphs, UI/UX, Toolbars, Settings, Document Workflows, APKs, and Release Lifecycles.
   - **Zero cross-app installation dependency:** Installing `EW Documents` MUST NOT be required for `EW Sheets` to operate, and vice versa.
   - **Zero third-party app dependency:** Users MUST NOT be forced to install a third-party "Engine APK" or external Office app for either app to function.
3. **Offline-First & Privacy-First:**
   - 100% local document processing in-process; no local TCP server port exposed to other apps; no cloud or network dependency.

---

## 2. CANDIDATE ARCHITECTURE OPTIONS

### Option A — Two Independent APKs, Each Bundling Its Own Native Engine (`Self-Contained Dual-APK`)
- **Structure:**
  - Monorepo or shared multi-module Gradle workspace containing `:core:*` shared Kotlin modules (`:core:storage`, `:core:ui`, `:core:database`, `:core:security`, `:core:common`), `:engine:collabora-lib` (build-time AAR module containing `liblo-native-code.so`, `libandroidapp.so`, and stripped Writer/Calc assets), `:app-documents` (`com.eurowindow.documents`), and `:app-sheets` (`com.eurowindow.sheets`).
  - At build time, `:app-documents` packages `liblo-native-code.so` + Writer-focused UI/templates into `ew-documents-arm64-v8a.apk`.
  - At build time, `:app-sheets` packages `liblo-native-code.so` + Calc-focused UI/templates into `ew-sheets-arm64-v8a.apk`.
- **Runtime Behavior:**
  - Each APK runs in its own Linux UID sandbox (`/data/user/0/com.eurowindow.documents` and `/data/user/0/com.eurowindow.sheets`), extracts its own `program/*.rdb` and font config into its private `dataDir`, and loads its own `libandroidapp.so` + `liblo-native-code.so` in-process.

### Option B — Two Independent APKs Consuming a Shared Build-Time Engine Distribution Artifact (`AAR / Maven Local Artifact`)
- **Structure:**
  - Separation of concerns between **Engine Build Lifecycle (Linux CI)** and **App Development Lifecycle (Windows 10 LTSC)**:
    1. **Engine Distribution Pipeline (Linux):** Compiles Collabora (`engine/` + `android/lib/`) for `arm64-v8a` and publishes a versioned, checksummed **Engine Distribution AAR** (`com.eurowindow.office:collabora-engine-arm64:26.04.x.aar`) to a local Maven directory (`F:\word và Excel\maven-repo`).
    2. **Shared Core Modules (Windows):** Build-time Kotlin libraries (`:core:saf`, `:core:datastore`, `:core:room`, `:core:designsystem`, `:core:crashrecovery`).
    3. **Two Independent App Projects/Modules (Windows):** `:app-documents` (`com.eurowindow.documents`) and `:app-sheets` (`com.eurowindow.sheets`) each consume the build-time `:core:*` modules and the prebuilt `collabora-engine-arm64` AAR, applying per-app asset filtering (e.g., excluding Impress/Draw templates and unrelated UI chrome) and per-app `applicationId` substitution (`@@APPLICATION_ID@@` in `fonts.conf` and `ClipboardFileProvider` authority `${applicationId}.clipboard`).
- **Runtime Behavior:**
  - Identical runtime independence to Option A: each resulting APK (`EW Documents.apk` and `EW Sheets.apk`) is 100% self-contained at runtime, requires no third app, and has zero runtime coupling to the other app, while avoiding recompiling C++ native code during Windows UI/UX development.

### Option C — One Shared Engine Service Application + Two Thin Frontend APKs (`3-APK Runtime Shared Engine`)
- **Structure:**
  - A third APK (`com.eurowindow.office.engine`) bundles `liblo-native-code.so` and exposes a cross-process `Service` (via AIDL / Messenger / ContentProvider or local socket), or loads code dynamically via `Context.createPackageContext("com.eurowindow.office.engine", CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY)`.
  - `EW Documents` and `EW Sheets` are thin 10 MB frontend APKs that bind to or load from `com.eurowindow.office.engine`.
- **Runtime Failure Modes:**
  - **Violates Product Principle & Section 2 Constraint:** *"Không bắt buộc cài thêm ứng dụng thứ ba."* If a user installs only `EW Documents.apk`, it fails to launch unless `com.eurowindow.office.engine.apk` is also installed.
  - **Android 11+ (API 30+) Package Visibility & SELinux Restrictions:** Loading native `.so` binaries across separate APKs with different signing or UID boundaries is restricted by Android SELinux (`untrusted_app` W^X / `dlopen` path restrictions on `/data/app/`), and rendering WebView/Canvas tiles across AIDL IPC introduces severe latency and crash recovery complexity.

---

## 3. MULTI-CRITERIA EVALUATION MATRIX

| Criteria | Requirement Weight | Option A: Dual Self-Contained Monolithic Build | Option B: Dual Self-Contained APKs via Shared Build-Time Engine AAR | Option C: Shared Runtime Engine APK + Two Thin Frontends |
|---|---|---|---|---|
| **App Independence** | **Critical** | **PASS (10/10)** — Zero runtime dependency between apps or third-party APKs. | **PASS (10/10)** — Zero runtime dependency; each APK is 100% standalone. | **FAIL (2/10)** — Requires installing a 3rd Engine APK; breaks standalone constraint. |
| **Offline Operation** | **Critical** | **PASS (10/10)** — 100% in-process `FakeSocket`. | **PASS (10/10)** — 100% in-process `FakeSocket`. | **PARTIAL (6/10)** — Requires inter-app IPC / socket binding across processes. |
| **Privacy & Security** | **Critical** | **PASS (10/10)** — Strict Linux UID sandbox per app (`/data/user/0/<pkg>`). | **PASS (10/10)** — Strict Linux UID sandbox per app; no cross-app data leakage. | **FAIL (4/10)** — Cross-app file descriptor passing & shared engine cache risks data leakage. |
| **APK Size** | **High** | **MODERATE (5/10)** — ~242 MB per APK if unstripped (~484 MB total for both). | **GOOD (7/10)** — ~155–185 MB per `arm64-v8a` APK after stripping unused dicts/fonts/Impress assets. | **HIGH (9/10)** — ~240 MB shared engine + two ~10 MB frontends (~260 MB total). |
| **Performance** | **High** | **PASS (9/10)** — Zero IPC overhead; direct JNI + `FakeSocket`. | **PASS (9/10)** — Zero IPC overhead; direct JNI + `FakeSocket` + independent OS processes. | **POOR (4/10)** — Cross-process bitmap/tile serialization overhead; single engine bottleneck. |
| **Stability & Isolation** | **Critical** | **PASS (9/10)** — A crash in `EW Sheets` never affects `EW Documents`. | **PASS (10/10)** — Complete process, memory (`lokInitialized`), and `cacheDir` isolation between apps. | **FAIL (3/10)** — `COKit` uses process-global C++ singletons (`lokInitialized`); concurrent DOCX + XLSX editing in one engine process causes fatal state collisions! |
| **Maintenance** | **High** | **MODERATE (6/10)** — Tightly couples C++ engine build to Android app Gradle build (`variables.gradle`). | **EXCELLENT (9/10)** — Decouples Linux C++ engine build from Windows Kotlin/Compose app builds via versioned AAR. | **POOR (3/10)** — Complex AIDL protocol versioning across 3 APKs. |
| **Update Complexity** | **High** | **PASS (8/10)** — Independent APK updates. | **PASS (9/10)** — Each app updates its APK independently and bumps Engine AAR version when ready. | **FAIL (2/10)** — Updating the Engine APK can break one or both frontends simultaneously. |
| **Engine Compatibility** | **Critical** | **PASS (9/10)** — Matches upstream Collabora `LOActivity` model. | **PASS (10/10)** — Matches upstream Collabora `android/lib` library model while supporting Windows host development. | **FAIL (1/10)** — Upstream Collabora `androidapp.cpp` is hardcoded for single-document in-process execution (`static std::string fileURL; static int fakeClientFd;`). |

---

## 4. CRITICAL TECHNICAL FINDING ON ENGINE MULTI-PROCESS ISOLATION

In `CollaboraOnline/online.mirror/android/lib/src/main/cpp/androidapp.cpp` (lines 315–320 and 623–635):
```cpp
int coolwsd_server_socket_fd = -1;
const char* user_name;
static std::string fileURL;
static int fakeClientFd;
```
Collabora's native Android bridge (`libandroidapp.so` + `liblo-native-code.so`) uses **process-global static variables** (`lokInitialized`, `coolwsd_server_socket_fd`, `fileURL`, `fakeClientFd`, `g_loActivityObj`).
- In **Option C** (a single shared runtime engine process serving both `EW Documents` and `EW Sheets`), opening a DOCX in `EW Documents` and switching to `EW Sheets` to open an XLSX would overwrite `g_loActivityObj` and `fileURL`, immediately closing or corrupting the active session!
- In **Option B** (and **Option A**), because `com.eurowindow.documents` and `com.eurowindow.sheets` are **two separate Android applications with separate Application IDs and separate Linux processes**, Android spawns two completely isolated JVM/JNI memory spaces. Users can have `EW Documents` and `EW Sheets` open side-by-side in Android split-screen mode editing a DOCX and an XLSX simultaneously with **zero C++ global state collision**.

---

## 5. SUMMARY CONCLUSION

- **Option C** is **REJECTED** due to violating the standalone-app requirement, introducing cross-app security risks, and colliding with Collabora's process-global C++ singletons.
- **Option B (Two Independent Self-Contained APKs Consuming a Shared Build-Time Engine AAR + Shared Build-Time Core Modules)** is **SELECTED** as the official architecture, combining the 100% runtime independence of Option A with clean separation between Linux native engine compilation and Windows 10 LTSC Kotlin/Compose application development.
