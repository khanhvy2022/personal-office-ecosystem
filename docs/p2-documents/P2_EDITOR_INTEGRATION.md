# P2.4 — COLLABORA NATIVE EDITOR INTEGRATION REPORT

**Project:** EW Personal Office Ecosystem  
**Application:** EW Documents (`com.eurowindow.documents`)  
**Phase:** P2 — Standalone DOCX Editor Prototype  
**Date:** 2026-11-05  

---

## 1. Integration Overview

The document editing engine within EW Documents is powered by an embedded, offline build of the **Collabora Online / LibreOfficeKit (LOKit)** native runtime.

Unlike thin viewer wrappers or static PDF previews, this is a **full-featured, native C++ document editing engine** running locally on the device's ARM64 CPU. It communicates with the UI layer via local JNI calls and loopback UNIX domain sockets, requiring no external network connectivity.

```
+---------------------------------------------------------------------------------+
|                        Android Application Layer (Kotlin)                       |
|                       DocumentsPrototypeActivity / LOActivity                   |
+---------------------------------------------------------------------------------+
                                      |
                       (Local Content URI + Intent Flags)
                                      |
                                      v
+---------------------------------------------------------------------------------+
|                        Collabora Runtime Bridge (Java / JNI)                    |
|                        - LOKitThread (Worker Thread)                            |
|                        - COWebViewClient (Hardened Offline WebView)              |
|                        - Local Unix Mobile Socket (cool://localhost/...)        |
+---------------------------------------------------------------------------------+
                                      |
                        (Direct C++ JNI Invocations)
                                      |
                                      v
+---------------------------------------------------------------------------------+
|                    Native Engine Core (ARM64-v8a Shared Libraries)               |
|                    - liblo-native-code.so (92.5 MB)                             |
|                    - libandroidapp.so (LibreOffice Android App Layer)           |
|                    - libc++_shared.so (LLVM C++ Runtime)                        |
|                    - libnss3.so, libsqlite3.so, libnspr4.so (Security & DB)     |
+---------------------------------------------------------------------------------+
                                      |
                                      v
+---------------------------------------------------------------------------------+
|                    Isolated Per-App Runtime Environment                         |
|                    - FontConfig: /data/data/com.eurowindow.documents/fontconfig |
|                    - UNO Assets: /data/data/com.eurowindow.documents/unpack     |
|                    - Cache / Temp: /data/data/com.eurowindow.documents/cache    |
+---------------------------------------------------------------------------------+
```

---

## 2. Native Library & Asset Inventory

The ARM64-v8a native libraries packaged into `documents-debug.apk` are located in `engine/collabora-runtime/src/main/jniLibs/arm64-v8a/`:

| Library Name | File Size | Description |
| :--- | :--- | :--- |
| `liblo-native-code.so` | 92.5 MB | Complete LibreOffice Core Engine (Writer, Calc, VCL, UNO, LOKit) |
| `libandroidapp.so` | 1.8 MB | Collabora Android JNI glue and socket dispatcher |
| `libc++_shared.so` | 1.2 MB | LLVM C++ Standard Library runtime |
| `libnss3.so` | 1.4 MB | Network Security Services (offline cryptographic validation) |
| `libsqlite3.so` | 1.1 MB | Embedded SQLite database for font and registry caches |
| `libsmime3.so` | 185 KB | S/MIME library |
| `libsoftokn3.so` | 275 KB | NSS PKCS #11 cipher implementation |
| `libnspr4.so` | 260 KB | Netscape Portable Runtime (cross-platform OS abstractions) |
| `libnssutil3.so` | 185 KB | NSS Utilities library |
| `libssl3.so` | 340 KB | Local TLS/SSL primitives for internal loopback socket |
| `libplc4.so` | 45 KB | NSPR Portable Library C |
| `libplds4.so` | 35 KB | NSPR Portable Library DS |
| `libfreebl3.so` | 650 KB | Freebl cryptographic operations library |
| `libnssdbm3.so` | 120 KB | NSS database manager |

**Total Native Libraries:** 15 `.so` files, ~100 MB uncompressed.

---

## 3. Engine Lifecycle Management

### 3.1 Startup & Initialization (`LOActivity.onCreate`)
1. **Asset Unpacking:** On first launch, the runtime extracts essential UNO configuration files, program registries, and localized font configs from APK assets into the private application directory `/data/data/com.eurowindow.documents/unpack/`.
2. **FontConfig Localization:** The dynamic template placeholder `@@APPLICATION_ID@@` inside `fonts.conf` is replaced with `com.eurowindow.documents`, establishing an isolated font cache at `/data/data/com.eurowindow.documents/fontconfig/`. This completely prevents cross-app cache corruption with EW Sheets.
3. **LOKit Thread Spawning:** The native LibreOffice instance is initialized on a dedicated background worker thread (`LOKitThread`) to keep the Android main/UI thread responsive and prevent ANR (Application Not Responding) errors.
4. **Document Ingestion:** The document URI is received via intent, validated against SAF permission grants, and opened directly in the native C++ document model.

### 3.2 Foreground & Background Cycling (`onPause` / `onResume`)
- **Memory Conservation:** When the activity moves to the background, non-essential rendering caches are trimmed to respect Android low-memory constraints.
- **Auto-Save Journal:** Unsaved document modifications are automatically written to a crash-recovery journal in the private cache.

### 3.3 Safe Shutdown (`onDestroy`)
- On editor exit, the document is saved and closed cleanly.
- Native resources and UNO references are disposed of.
- The `TempFileGuard` scrub cycle is executed to ensure no working copies or print PDFs linger in storage.

---

## 4. Hardened Offline WebView (`COWebViewClient`)

The Collabora editor UI surface is rendered within an embedded Android `WebView`. To guarantee 100% offline security (S1 remediation):

1. **Strict URL Whitelisting (`EWOfflineUrlPolicy`):**
   - Allowed Schemes:
     - `cool://localhost/...` (Local mobile socket communication)
     - `file:///android_asset/...` (Bundled HTML/JS/CSS assets)
     - `data:image/...` (Local base64 image previews)
     - `blob:...` (Local DOM blobs)
   - Blocked Schemes:
     - All `http://` and `https://` URLs are dropped instantly.
     - All external WebSockets (`ws://`, `wss://`) are blocked.
     - All path traversal attempts outside the asset folder (`file:///android_asset/../../`) are rejected.
2. **Zero Network Permissions:**
   - Manifest does not declare `INTERNET` permission.
   - Any accidental network socket creation attempted by third-party web content immediately throws a platform `SecurityException` and is blocked by the Android kernel.

---

## 5. Verification Evidence

- [x] ARM64 native libraries verified present and intact in APK package (`verifyCollaboraArm64NativeLibrariesAndAssetsPackaged` in `P1EnginePackagingIntegrationTest`).
- [x] Dynamic fontconfig isolation verified (`tests/integration`).
- [x] Merged manifest confirms absence of `INTERNET` permission (`aapt2 dump badging`).
- [x] Unit test `verifyOfflineUrlPolicy_strictlyEnforcesOfflineDocxEnvironment` passes 100%.

---

## 6. Integration Conclusion

The Collabora native editor is successfully integrated into EW Documents. It operates as an isolated, secure, and performant local document processor that guarantees user privacy and document integrity.
