# P1.1 — ENGINE PACKAGING REPORT
## Collabora ARM64 Runtime Integration & Two-App Independent Packaging

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**Git Baseline Commit (P0 preserved):** `e936f29660c16a4c5510896e6549ef6c131860f1` (`main`)  
**Implementation Branch:** `p1-prototype`  
**Status:** **PASS** (Host Packaging, Native Runtime Integration, Two-App APK Build & Signature Verification)

---

## 1. Upstream Binary Provenance & Cryptographic Verification

Before integrating the native runtime into `:engine:collabora-runtime`, the official Collabora Office Mobile ARM64 release artifact was downloaded from Collabora's official F-Droid repository and verified against its repository index metadata and signing certificate:

| Attribute | Verified Value |
| :--- | :--- |
| **Upstream Artifact** | `F:\word và Excel\downloads\collabora-office-mobile-26.04.4.2-937-release-arm64-v8a-2026-10-05.apk` |
| **Upstream Version** | `26.04.4.2` (`versionCode = 26040402`, build `937`) |
| **Upstream Git Hash** | `e7a69b5` (`assets/unpack/program/versionrc` `buildid=eb70f023c6c164ed8c9d0dfdc67c10a73bf4dc11`) |
| **File Size** | `254,391,190` bytes (`242.61 MB`) |
| **SHA-256 Digest** | `79e72f810d711706d205a9ff457b4bb830c607a2b988cf44246829ec4593c393` (Matches F-Droid `index-v1.json`) |
| **APK Signature Verification** | Verified (`v2` + `v3` schemes) via `apksigner.jar verify --print-certs` |
| **Signer Certificate DN** | `CN=Tomaz Vajngerl, OU=productivity, O=Collabora Productivity Limited, L=Cambridge, C=GB` |
| **Signer Cert SHA-256** | `573258c84e149b5f4d9299e7434b2b69a8410372921d4ae586ba91ec767892cc` |

---

## 2. Multi-Module Gradle Project Structure (`EWOfficeEcosystem/`)

Created at `F:\word và Excel\EWOfficeEcosystem\`:

```text
EWOfficeEcosystem/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  local.properties
  app/
    documents/              -> com.eurowindow.documents (EW Documents)
    sheets/                 -> com.eurowindow.sheets (EW Sheets)
  engine/
    collabora-runtime/      -> Shared build-time Android library wrapping LibreOfficeKit ARM64 JNI & assets
  core/
    storage/                -> SAF Document controller (ACTION_OPEN_DOCUMENT / ACTION_CREATE_DOCUMENT)
    database/               -> Per-app SQLite recent documents store (ew_documents.db / ew_sheets.db)
    datastore/              -> Per-app isolated preferences store (ew_documents_prefs / ew_sheets_prefs)
    ui/                     -> Jetpack Compose Material 3 P1 prototype validation shell
    security/               -> S1–S4 security policies (EWOfflineUrlPolicy, TempFileGuard, BroadcastIsolationPolicy)
  tests/
    integration/            -> Engine packaging & native library integrity tests
    compatibility/          -> OOXML DOCX/XLSX Vietnamese & formula corpus tests
    security/               -> S1–S4 security remediation unit tests
```

---

## 3. Native Libraries, JNI Dependencies, Assets, Fonts & UNO Configuration

### 3.1 Packaged ARM64 (`arm64-v8a`) Native Libraries
Packaged in `engine/collabora-runtime/src/main/jniLibs/arm64-v8a/` (15 Collabora runtime `.so` libraries + 2 AndroidX runtime `.so` libraries in final APKs = 17 total `.so` files):

| Native Library | Size (Bytes) | Role |
| :--- | ---: | :--- |
| `liblo-native-code.so` | `204,703,808` | Monolithic LibreOfficeKit core (Writer, Calc, Impress, Draw, OOXML filters, Cairo/UI) |
| `libandroidapp.so` | `223,128` | Collabora Android JNI bridge (`LOActivity`, `COWebViewClient`, `MobileSocket`) |
| `libc++_shared.so` | `1,300,320` | LLVM C++ standard library runtime |
| `libnss3.so` | `1,254,328` | Mozilla NSS cryptographic library |
| `libsqlite3.so` | `1,046,248` | SQLite dependency for NSS |
| `libfreebl3.so` | `607,480` | NSS crypto primitive library |
| `libnssckbi.so` | `489,840` | NSS built-in root certificates module |
| `libssl3.so` | `428,448` | NSS SSL/TLS library |
| `libsoftokn3.so` | `322,064` | NSS PKCS#11 soft token |
| `libnspr4.so` | `257,072` | Netscape Portable Runtime |
| `libnssutil3.so` | `200,824` | NSS utilities |
| `libsmime3.so` | `172,568` | NSS S/MIME library |
| `libnssdbm3.so` | `164,864` | NSS legacy database library |
| `libplc4.so` | `21,152` | NSPR PLC library |
| `libplds4.so` | `16,968` | NSPR PLDS library |

### 3.2 Assets, Fonts & UNO Configuration Isolation
- **Assets Count:** `5,272` files in `engine/collabora-runtime/src/main/assets/` (`dist/` COOL frontend bundle, `program/` UNO services/types/rc files, `share/` Calc/Writer configurations, `unpack/` runtime files).
- **Fonts:** `35` font files (`Caladea`, `Carlito`, `Gentium`, `Liberation`, `OpenSymbol`) under `assets/unpack/user/fonts/`.
- **UNO Configuration:** `assets/program/bootstraprc`, `fundamentalrc`, `unorc`, `versionrc`, `services.rdb`, `types.rdb`, `offapi.rdb`.
- **Per-Application Fontconfig Cache Fix:** Upstream Collabora hardcoded `<cachedir>/data/data/com.collabora.libreoffice/fontconfig</cachedir>` in `assets/unpack/etc/fonts/fonts.conf`. We replaced this with `<cachedir>/data/data/@@APPLICATION_ID@@/fontconfig</cachedir>` and implemented `LOActivity.patchUnpackedFontsConf()` on extraction so each app writes exclusively to its own private `/data/data/<applicationId>/fontconfig` directory.
- **Engine Lifecycle Management:** `LOActivity.java` initializes the native LOKit engine on `onCreate()`, processes document messages on a dedicated `nativeMsgThread` (`nativeLooper`), saves/closes documents cleanly on pause/back navigation, and invokes `purgeTemporaryCacheFiles()` in `onPause()`/`onDestroy()`.

---

## 4. Built Prototype APK Metrics & Hashes

Both applications were compiled and packaged independently via `.\gradlew.bat assembleDebug` (log: `F:\word và Excel\logs\p1-assemble-debug.log`):

| Metric | EW Documents (`:app:documents`) | EW Sheets (`:app:sheets`) |
| :--- | :--- | :--- |
| **APK Path** | `EWOfficeEcosystem/app/documents/build/outputs/apk/debug/documents-debug.apk` | `EWOfficeEcosystem/app/sheets/build/outputs/apk/debug/sheets-debug.apk` |
| **Application ID** | `com.eurowindow.documents` | `com.eurowindow.sheets` |
| **Version** | `0.1.0-p1-prototype` (`versionCode = 100`) | `0.1.0-p1-prototype` (`versionCode = 100`) |
| **APK Size** | **`137,629,706` bytes (`131.25 MB`)** | **`137,653,589` bytes (`131.28 MB`)** |
| **SHA-256** | `ee4bc6b19b9718d59532704a63f0def41f89a8370f02a5a4889ffe9666ba71e3` | `8e7159b7e377609de0f24193eb2413004273031e0f00d0b6d78170dd23afa0c8` |
| **Supported ABI** | `arm64-v8a` | `arm64-v8a` |
| **Min SDK / Target SDK / Compile SDK** | `26` / `35` / `35` | `26` / `35` / `35` |
| **FileProvider Authority** | `com.eurowindow.documents.fileprovider` | `com.eurowindow.sheets.fileprovider` |
| **Clipboard Authority** | `com.eurowindow.documents.clipboard` | `com.eurowindow.sheets.clipboard` |
| **Database / DataStore** | `ew_documents.db` / `ew_documents_prefs` | `ew_sheets.db` / `ew_sheets_prefs` |
| **APK Signature** | Verified (`APK Signature Scheme v2`) | Verified (`APK Signature Scheme v2`) |

---

## 5. Runtime Dependency Tree Summary

Full dependency graphs are archived in `F:\word và Excel\logs\p1-dependencies.log`. Key direct & transitive runtime dependencies:
- `:engine:collabora-runtime` -> `androidx.appcompat:appcompat:1.7.0`, `com.google.android.material:material:1.12.0`, `androidx.constraintlayout:constraintlayout:2.1.4`, `androidx.preference:preference:1.2.1`, `androidx.annotation:annotation:1.8.2`
- `:core:security` -> `androidx.core:core-ktx:1.13.1`, `androidx.annotation:annotation:1.8.2`
- `:core:storage` -> `:core:security`, `androidx.core:core-ktx:1.13.1`, `androidx.documentfile:documentfile:1.0.1`
- `:core:database` -> `androidx.core:core-ktx:1.13.1`
- `:core:datastore` -> `androidx.datastore:datastore-preferences:1.1.1`
- `:core:ui` -> `androidx.compose:compose-bom:2024.09.00` (`ui`, `ui-graphics`, `material3`), `androidx.activity:activity-compose:1.9.2`
