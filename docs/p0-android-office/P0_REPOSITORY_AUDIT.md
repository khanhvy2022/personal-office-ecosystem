# P0 — REPOSITORY AUDIT REPORT
## EW Personal Office Ecosystem (`EW Documents` & `EW Sheets`)

**Project Root:** `F:\word và Excel`  
**Audit Date:** 2026-11-04  
**Phase:** P0 — Research & Feasibility Only  
**Audit Method:** Direct GitHub REST API inspection, source tree analysis (`configure.ac`, `build.gradle`, `CMakeLists.txt.in`, `AndroidManifest.xml`, `LOActivity.java`, `androidapp.cpp`, `WordDocument.kt`, `ExcelDocument.kt`), F-Droid official binary repository inspection (`index-v1.json`), and SBOM/License verification (`engine/readlicense_oo/license/license.xml`, `THIRDPARTYLICENSES`).

---

## 1. EXECUTIVE SUMMARY OF CANDIDATE REPOSITORIES

| Audit Dimension | Primary Candidate: Collabora (`CollaboraOnline/online.mirror`) | Alternative Candidate 1: Kern (`verycareful/kern`) | Alternative Candidate 2: ONLYOFFICE (`ONLYOFFICE/*`) |
|---|---|---|---|
| **Repository URL** | https://github.com/CollaboraOnline/online.mirror (Code Mirror) & https://github.com/CollaboraOnline/online (Issue Tracker) | https://github.com/verycareful/kern | https://github.com/ONLYOFFICE/core & https://github.com/ONLYOFFICE/editors-webview-android |
| **Canonical Upstream** | `https://gerrit.collaboraoffice.com/online` (Single Monorepo including `engine/`) | `https://github.com/verycareful/kern` | `https://github.com/ONLYOFFICE` |
| **Repository Ownership** | **Collabora Productivity Limited** (`CollaboraOnline` Organization) | Individual developer (`verycareful`) | **Ascensio System SIA** (`ONLYOFFICE` Organization) |
| **Latest Stable Release** | `26.04.4.2` (VersionCode `937`, F-Droid repo) / Tag `helm-collabora-online-1.3.7` (2026-10-23) | `0.1.14.3` (VersionCode `36`, published 2026-11-03T13:24:08Z) | `v10.0.0` (`ONLYOFFICE/core` merged 2026-10-15); no standalone offline Android engine release |
| **Latest Relevant Commit** | `a1d7358c4556f10b2f4bb7cb44550e7f57e5214b` (2026-11-04T09:13:11Z) on `online.mirror` | `0358f2c8b99b42483a0c74253d6f1b86451d97fa` (2026-11-03T13:17:56Z) | `ed63e94de814ff1d4ea28cddfaba6939bc9df9ed` (2026-10-15T13:23:05Z) on `ONLYOFFICE/core` |
| **Maintenance Activity** | **Very High** — Daily enterprise commits across `engine/`, `wsd/`, `kit/`, `browser/`, and `android/` | **Active (Alpha)** — Created 2026-06-03, frequent commits by single maintainer | **High on Server Core / Low on Public Android** — `editors-webview-android` last pushed 2026-05-14 |
| **Open Issues** | `876` open issues on `CollaboraOnline/online` (`0` on read-only mirror) | `2` open issues (`6` stars, `0` forks) | `27` open issues on `ONLYOFFICE/core`; `0` on `editors-webview-android` |
| **Android Support** | **Production-grade Native + Hybrid** (`android/app` + `android/lib` AAR wrapping `liblo-native-code.so` & `libandroidapp.so`) | **Alpha Native Kotlin/Compose** (`Apache POI 5.5.1` pure JVM for DOCX/XLSX) | **Cloud WebView Wrapper or Proprietary Mobile App** (No public offline self-contained Android SDK) |
| **Min / Target Android SDK** | `minSdk 26` (Android 8.0), `targetSdk 37`, `compileSdk 37` (`CPAndroidCommon.conf` sets NDK API level 21) | `minSdk 26` (Android 8.0), `targetSdk 35`, `compileSdk 35` | `editors-webview-android` is a legacy WebView demo |
| **Supported ABIs** | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` | `arm64-v8a`, `armeabi-v7a`, `x86_64` (for PDF native bridge; POI is pure JVM) | N/A for public offline engine |
| **Offline Capabilities** | **100% Offline** via in-process `FakeSocket` (`net/FakeSocket.cpp`) + custom WebView scheme (`cool://`) | **100% Offline** via local Apache POI DOM manipulation | **Requires Remote Document Server** in `editors-webview-android` |
| **Primary License** | **MPL-2.0** (Mozilla Public License 2.0) — Weak file-level copyleft | **AGPL-3.0** (GNU Affero General Public License v3.0) — Strong copyleft | **AGPL-3.0** (GNU Affero General Public License v3.0) |
| **APK Size (`arm64-v8a`)** | **242.61 MB** (Release `26.04.4.2`) / **245.29 MB** (Snapshot `26.04.6.0-971`); optimizable to ~135–165 MB per single-purpose app | ~25–35 MB (Apache POI + Compose + bundled JS preview assets) | N/A |
| **Suitability Verdict** | **RECOMMENDED PRIMARY ENGINE** (Full fidelity, MPL-2.0, 100% offline, supports 2 independent APKs) | **SUPPLEMENTAL REFERENCE ONLY** (Lacks formula recalculation, page layout, DOCX->PDF export, Charts; AGPL-3.0) | **REJECTED FOR P0/P1** (AGPL-3.0, server-centric architecture, no public standalone offline Android build guide) |

---

## 2. DETAILED AUDIT — PRIMARY CANDIDATE: COLLABORA (`CollaboraOnline/online.mirror`)

### 2.1 Repository Ownership, Structure & Maintenance
- **Ownership:** Maintained by **Collabora Productivity Limited** (`CollaboraOnline`).
- **Monorepo Consolidation (2026 Architecture):** As documented in `android/README.md`, Collabora consolidated its repositories into a single Gerrit monorepo (`https://gerrit.collaboraoffice.com/online`), mirrored read-only at `https://github.com/CollaboraOnline/online.mirror` (created `2026-05-17T17:15:04Z`, pushed daily as of `2026-11-04T13:40:00Z`).
- **Subdirectory Layout:**
  - `engine/`: The full Collabora Office / LibreOffice core source tree (formerly `CollaboraOnline/core`).
  - `wsd/`, `kit/`, `common/`, `net/`: Collabora Online WebSocket Daemon & LibreOfficeKit (`COKit`) C++ bridge, compiled directly into the Android native library (`libandroidapp.so`) using in-memory `FakeSocket`.
  - `browser/`: HTML5/JavaScript/Canvas rendering and UI client (`cool.html`, `bundle.js`), compiled into `android/lib/src/{debug,release}/assets/dist/`.
  - `android/lib/`: Reusable Android Library module (`com.android.library`, namespace `org.libreoffice.androidlib`) containing `LOActivity.java`, `COWebView.java`, `COWebViewClient.java`, `MobileSocket.java`, `PrintAdapter.java`, and JNI C++ glue (`androidapp.cpp`).
  - `android/app/`: Thin launcher and file browser shell (`com.android.application`, namespace `org.libreoffice.androidapp`).

### 2.2 Latest Releases & Commits
- **F-Droid Production Binary Repository (`https://www.collaboraoffice.com/downloads/fdroid/repo/index-v1.json`):**
  - **Stable Package (`com.collabora.libreoffice`):** Version `26.04.4.2` (`versionCode: 937`).
    - `arm64-v8a` APK: **242.61 MB** (`254,394,xxx` bytes), `minSdkVersion: 26`, `targetSdkVersion: 37`.
    - `armeabi-v7a` APK: **178.62 MB**.
    - Universal (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) APK: **788.63 MB**.
  - **Snapshot Package (`com.collabora.libreoffice.snapshot`):** Version `26.04.6.0-971` (`versionCode: 971`), `arm64-v8a` APK: **245.29 MB**.
- **Latest Monorepo Commits Verified:**
  - `a1d7358c4556f10b2f4bb7cb44550e7f57e5214b` (`2026-11-04T09:13:11Z`): `wsd: keep a refused subscription listed as failed`
  - `32ddeed260a310392c333877b3f0349c59eba29d` (`2026-10-29T13:17:58Z`): `Impress vector rendering: Draw light fills dark on a dark view.`
  - `d309e70f6625abb03cd8476c0ea42753b35335e7` (`2026-10-27T09:35:05Z`): `github: close pull requests that change source code`

### 2.3 Native Dependencies, Minimum SDK & ABIs
- **Minimum Android SDK:**
  - `android/app/build.gradle` and `android/lib/build.gradle` explicitly set `minSdk 26` (Android 8.0 Oreo) and `targetSdk 37` (`compileSdk 37`).
  - `engine/distro-configs/CPAndroidCommon.conf` sets `--with-android-api-level=21` for the lower-level C/C++ cross-compilation sysroot, which is 100% compatible with `minSdk 26`.
- **Supported ABIs:**
  - `arm64-v8a` (`--with-distro=CPAndroidAarch64`)
  - `armeabi-v7a` (`--with-distro=CPAndroid`)
  - `x86_64` (`--with-distro=CPAndroidX86_64`)
  - `x86` (`--with-distro=CPAndroidX86`)
- **Native Shared Libraries Packaged in APK (`CMakeLists.txt.in`):**
  1. `liblo-native-code.so`: Monolithic statically-linked LibreOfficeKit (`COKit`) core engine containing Writer (`sw`), Calc (`sc`), Impress (`sd`), DrawingML/OOXML filters (`oox`, `writerfilter`), PDF export/import (`pdfium`, `vcl`), Cairo/Skia, HarfBuzz, ICU, libxml2, libxslt, Hunspell, etc.
  2. `libandroidapp.so`: JNI bridge compiled by Gradle CMake (`android/lib/src/main/cpp/CMakeLists.txt.in`), statically linking `libPocoNet.a`, `libPocoUtil.a`, `libPocoXML.a`, `libPocoJSON.a`, `libPocoFoundation.a`, `libexpat.a`, `libzstd.a`, `liblibpng.a`, and dynamically linking `liblo-native-code.so`.
  3. **Mozilla NSS / SQLite dynamic libraries** copied from `${LOBUILDDIR_ABI}/instdir/program/`: `libfreebl3.so`, `libnspr4.so`, `libnss3.so`, `libnssckbi.so`, `libnssdbm3.so`, `libnssutil3.so`, `libplc4.so`, `libplds4.so`, `libsmime3.so`, `libsoftokn3.so`, `libsqlite3.so`, `libssl3.so`, plus `libc++_shared.so`.

### 2.4 Proof of Server-less 100% Offline Execution
A common misconception is that Collabora Online on Android runs a local TCP/HTTP server or requires remote server connectivity. **Direct source code inspection proves this is false:**
1. **In-Memory Pipe Communication (`FakeSocket.cpp`):** In `android/lib/src/main/cpp/androidapp.cpp` (lines 522–650), `createCOOLWSD()` spawns an in-process thread (`ProcUtil::setThreadName("app")`) that communicates with the JavaScript frontend using `FakeSocket` (`fakeSocketPipe2`, `fakeSocketConnect`, `fakeSocketPoll`, `fakeSocketRead`, `fakeSocketWriteQueue`) — an in-memory buffer queue that never opens a POSIX `AF_INET` TCP/IP socket.
2. **Custom WebView Interception (`COWebViewClient.java`):** `LOActivity` loads `file:///android_asset/dist/cool.html` into `COWebView`. All requests to `cool://mobilesocket/...` and `cool://media/...` are intercepted in-process by `COWebViewClient.shouldInterceptRequest()` and routed to `MobileSocket.java` and `Java_org_libreoffice_androidlib_LOActivity_postMobileMessageNative()`.
3. **Local Document Processing:** When opening a `content://` URI from Android SAF, `LOActivity.copyFileToTemp()` copies the stream into `getCacheDir()/CollaboraOffice*.tmp`, passes `file://` to `COKit`, and upon `SAVE`, `copyTempBackToIntent()` writes the modified temp file directly back to the original SAF `content://` URI via `ContentResolver.openOutputStream(uri, "wt")`.

### 2.5 Build Dependencies & Integration Complexity
- **Build Host Requirement:** `android/README.md` states: *"A computer running Linux or macOS. The native parts of the Android app cannot currently be built on Windows."*
- **Toolchain Requirements:**
  - Android NDK `29.0.14206865` (NDK 27–30 supported).
  - Android Build Tools `37.0.0`, AGP `9.3.0`.
  - Autotools (`autoconf`, `automake`, `libtool`, `pkg-config`, `gmake`), Python 3, Node.js/npm (for `browser/dist` bundle), CMake `3.4.1+`, `ccache` / `icecream`.
- **Integration Complexity:** **High** for initial native engine build on Linux (takes 1.5–4 hours cold build without `ccache`); **Low-to-Medium** once prebuilt `.so` and `assets/` artifacts (or the `:lib` AAR) are exported to the Windows Kotlin/Compose project.

### 2.6 License & Branding Restrictions
- **Core License:** **MPL-2.0** (`COPYING`, `README.FILENOTICES.md`, `SBOM.md`).
- **Branding:** By default, `--enable-branding` applies `browser/branding` (`CollaboraOnline-branding`, licensed under Apache-2.0, containing Collabora logos/trademarks). Upstream explicitly provides `--disable-branding` or `--with-app-branding=<dir>` and `--with-app-name=<name>` (`configure.ac` lines 280–298, `android/README.md` lines 153–157) to produce unbranded or custom-branded builds (`EW Documents`, `EW Sheets`).
- **Bundled GPL/LGPL Dictionary & Font Assets Warning:** Inspection of `engine/readlicense_oo/license/license.xml` and `android/lib/build.gradle` (lines 553–598) reveals that `copyUnpackAssets` bundles `dict-de`, `dict-en`, `dict-es`, `dict-fr`, `dict-pt-BR` and certain fonts. Some dictionaries (e.g., `dict-de-spell`, `dict-vi-spell`, `Culmus` font) carry `GPL-2.0` or `GPL-3.0` licenses as standalone data extensions. These are optional asset files and can be excluded or replaced with permissive/OFL-1.1 equivalents (`Liberation`, `Carlito`, `Caladea`, `Gentium`) during packaging.

---

## 3. DETAILED AUDIT — ALTERNATIVE CANDIDATE 1: KERN (`verycareful/kern`)

### 3.1 Repository Ownership, Activity & Stack
- **URL:** `https://github.com/verycareful/kern`
- **Owner:** `verycareful` (Individual GitHub user).
- **History & Activity:** Created `2026-06-03T13:12:21Z`, latest release `0.1.14.3` on `2026-11-03T13:24:08Z` (`versionCode = 36`), latest commit `0358f2c8b99b42483a0c74253d6f1b86451d97fa` (`2026-11-03T13:17:56Z`). Stars: `6`, Forks: `0`, Open Issues: `2`.
- **Tech Stack (`gradle/libs.versions.toml` & `build.gradle.kts`):**
  - Kotlin `2.0.21`, Java `17`, AGP `8.7.2`, Gradle `8.10.2`.
  - Jetpack Compose BOM `2026.05.01`, Material 3, Navigation Compose `2.8.3`.
  - `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`.
  - **Office Engine:** **Apache POI `5.5.1`** (`org.apache.poi:poi` and `org.apache.poi:poi-ooxml`, pure JVM, no native code for DOCX/XLSX).

### 3.2 Deep Technical Audit of Kern's DOCX & XLSX Implementation
Direct source code inspection of `src/editors/word/WordDocument.kt` and `src/editors/excel/ExcelDocument.kt` reveals critical functional limitations against the P0 requirements:
1. **XLSX Formula Recalculation is UNSUPPORTED:**
   - Comment in `ExcelDocument.kt` (lines 11–14): *"Reads every sheet for display via DataFormatter, which yields the cached result for formula cells (no recalculation in alpha)."*
   - Comment in `ExcelDocument.kt` (lines 23–24): *"Writing a formula cell's grid text back would turn the formula into plain text."*
   - In `applyEditsAndSerialize()` (lines 142–144): `val number = value.toDoubleOrNull(); if (number != null) cell.setCellValue(number) else cell.setCellValue(value)`. Kern **never calls `setCellFormula()`** and **never invokes `FormulaEvaluator`**. Users cannot enter or edit formulas (`SUM`, `IF`, `VLOOKUP`, `XLOOKUP`, etc.).
2. **XLSX Advanced Features UNSUPPORTED in UI:**
   - No sheet add/delete/rename/reorder in `ExcelDocument.kt`.
   - No cell formatting editor (fonts, borders, fills, number formats, conditional formatting).
   - No Sort, Filter, Freeze Panes, or Charts.
   - No XLSX -> PDF export.
3. **DOCX Page Layout & Export UNSUPPORTED:**
   - `WordDocument.kt` parses paragraphs (`TITLE`, `HEADING1`, `HEADING2`, `BODY`), basic runs (`bold`, `italic`, `underline`, `strike`, `sizePt`, `colorHex`, `fontFamily`, `linkUrl`), simple 2D string tables (`TableBlock(val rows: List<List<String>>)`), and raster images (`ImageBlock`) into a vertical Compose `LazyColumn`.
   - It is a **block-list editor, not a WYSIWYG paginated layout engine**: there is no A4/Letter pagination, no portrait/landscape page orientation, no margin rendering, no header/footer editing, no page numbering, no Table of Contents generation, and **no DOCX -> PDF export**.
4. **Permissions Issue (`AndroidManifest.xml`):**
   - Requests `<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />` as primary storage model (`docs/architecture.md`), which violates the P0 principle *"Ưu tiên SAF thay vì MANAGE_EXTERNAL_STORAGE"*.
5. **License Issue:**
   - Licensed under **AGPL-3.0**, and bundles `elkjs` (`GPL-3.0-or-later`) inside its Mermaid JS bundle. Any derivative work of Kern's code must be distributed under AGPL-3.0. (Note: Apache POI 5.5.1 itself is Apache-2.0, so using Apache POI directly in a clean-room project does not trigger AGPL-3.0, but Apache POI still lacks a layout/rendering/PDF engine on Android).

---

## 4. DETAILED AUDIT — ALTERNATIVE CANDIDATE 2: ONLYOFFICE (`ONLYOFFICE`)

### 4.1 Repository Landscape & Architecture
- **Organization URL:** `https://github.com/ONLYOFFICE` (`Ascensio System SIA`).
- **Audited Repositories:**
  - `ONLYOFFICE/core` (`https://github.com/ONLYOFFICE/core`): Latest commit `ed63e94de814ff1d4ea28cddfaba6939bc9df9ed` (`2026-10-15T13:23:05Z`, `Merge branch release/v10.0.0 into master`). Description: *"Server core components which are a part of ONLYOFFICE Document Server"*.
  - `ONLYOFFICE/editors-webview-android` (`https://github.com/ONLYOFFICE/editors-webview-android`): Latest commit `2026-05-14T09:16:50Z`. Description: *"A demo example of an Android application for opening ONLYOFFICE editors based on WebView."*
  - `ONLYOFFICE/documents-app-android`: Returns `HTTP 404 Not Found` (not publicly available as an open-source standalone offline build repository).
- **Technical & Architectural Blockers:**
  1. **No Public Standalone Offline Android Engine Build:** Unlike Collabora (which compiles `COKit` + `FakeSocket` into an offline Android library module `android/lib` with documented build steps), ONLYOFFICE's public repositories separate the server core (`ONLYOFFICE/core`, built for Linux/Windows server environments with V8/Qt/x2t converters) from a thin WebView demo (`editors-webview-android`) that connects to a remote ONLYOFFICE Document Server URL.
  2. **License:** Licensed under **AGPL-3.0**, with strict commercial licensing requirements from Ascensio System SIA if AGPL-3.0 conditions or branding restrictions are modified in commercial/proprietary contexts.

---

## 5. REPOSITORY AUDIT CONCLUSION (GATE G0)

- **Gate G0 Status:** **PASS**
- **Conclusion:**
  1. **Collabora (`CollaboraOnline/online.mirror`)** is verified with hard source and binary evidence as the **only viable, production-proven, 100% offline, high-fidelity Office engine** for Android (`minSdk 26`, `arm64-v8a`, MPL-2.0).
  2. **Kern (`verycareful/kern`)** is an alpha-stage Compose wrapper around Apache POI 5.5.1 that cannot calculate formulas, cannot render paginated DOCX layouts, cannot export DOCX/XLSX to PDF, and is licensed under AGPL-3.0.
  3. **ONLYOFFICE** does not provide a public, reproducible, server-less offline Android native engine repository.
