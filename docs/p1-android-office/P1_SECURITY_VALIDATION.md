# P1.2 — SECURITY HARDENING & VALIDATION REPORT
## Implementation & Empirical Verification of S1, S2, S3, and S4

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**Status:** **PASS** (Verified via `aapt2` Merged Manifest Inspection, Source Hardening & Automated JUnit Security Suite)

---

## 1. Executive Summary of P1.2 Remediations

| Security Gate | Requirement | Implementation & Verification Evidence | Status |
| :--- | :--- | :--- | :---: |
| **S1: Network Elimination** | Strip `INTERNET` permission; block WebView network loads; whitelist internal schemes only | `tools:node="remove"` on `INTERNET` & `ACCESS_NETWORK_STATE`; `aapt2 dump permissions` shows **0 network permissions**; `setBlockNetworkLoads(true)`; `COWebViewClient` & `EWOfflineUrlPolicy` block `http/https/ws/wss` | **PASS** |
| **S2: Scoped Storage (SAF)** | Strip `MANAGE_EXTERNAL_STORAGE` & legacy storage permissions; enforce SAF | `tools:node="remove"` on `MANAGE/READ/WRITE_EXTERNAL_STORAGE`; `aapt2 dump permissions` shows **0 storage permissions**; `SafDocumentController` uses `ACTION_OPEN_DOCUMENT` & `ACTION_CREATE_DOCUMENT` | **PASS** |
| **S3: Temp File Scrubbing** | Track & delete temporary document, print, and clipboard files on lifecycle end | `LOActivity.purgeTemporaryCacheFiles()` called in `onPause(isFinishing)` & `onDestroy()`; `PrintAdapter.onFinish()` deletes `print.pdf`; `TempFileGuard` protects user-owned source files | **PASS** |
| **S4: Broadcast & Component Isolation** | Package-scope internal broadcasts; verify `exported="false"` & `allowBackup="false"` | `intent.setPackage(getPackageName())` in `LOActivity.java`; `android:allowBackup="false"`, `usesCleartextTraffic="false"`, and `exported="false"` on all internal activities/providers verified in `aapt2 dump xmltree` | **PASS** |

---

## 2. Detailed Remediation & Verification Evidence

### 2.1 S1 — `INTERNET` Permission Removal & WebView Offline Lockdown
1. **Manifest Merger Stripping:**
   Both `app/documents/src/main/AndroidManifest.xml`, `app/sheets/src/main/AndroidManifest.xml`, and `engine/collabora-runtime/src/main/AndroidManifest.xml` declare:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
   ```
2. **Actual `aapt2.exe dump permissions` Output on Built APKs:**
   - **EW Documents (`logs/p1-aapt2-documents-permissions.log`):**
     ```text
     package: com.eurowindow.documents
permission: com.eurowindow.documents.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.eurowindow.documents.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
     ```
   - **EW Sheets (`logs/p1-aapt2-sheets-permissions.log`):**
     ```text
     package: com.eurowindow.sheets
permission: com.eurowindow.sheets.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.eurowindow.sheets.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
     ```
   Neither APK requests `android.permission.INTERNET`, making OS-level socket creation (`socket(AF_INET, ...)`) impossible (`EACCES`).
3. **WebView & URL Scheme Hardening:**
   - `LOActivity.java` and `SlideShowActivity.java` configure `WebSettings`:
     - `setBlockNetworkLoads(true)`
     - `setBlockNetworkImage(true)`
     - `setAllowFileAccessFromFileURLs(false)`
     - `setAllowUniversalAccessFromFileURLs(false)`
   - `COWebViewClient.java` (`isAllowedOfflineUri`) and `EWOfflineUrlPolicy.kt` enforce a strict scheme allowlist (`cool://`, `file:///android_asset/`, `data:`, `blob:`, `javascript:`). Any external navigation or subresource request (`http://`, `https://`, `ws://`, `wss://`, `file:///sdcard/...`) is intercepted and rejected with HTTP `403 Blocked by EW Offline Security Policy`.
   - `RateAppController.java` was converted into a strict no-op, removing upstream Google Play Store URL redirects (`market://` and `https://play.google.com/...`).

### 2.2 S2 — `MANAGE_EXTERNAL_STORAGE` Removal & SAF Enforcement
1. **Manifest Merger Stripping:**
   `android.permission.MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, and `WRITE_EXTERNAL_STORAGE` are stripped via `tools:node="remove"`. Confirmed absent in `aapt2 dump permissions` above.
2. **Code Hardening:**
   - In `LOActivity.java`, upstream's `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` prompt was replaced with a direct SAF `Intent.ACTION_CREATE_DOCUMENT` fallback (`initiateSaveAs(exportFormat)`).
   - `SafDocumentController.kt` implements `buildOpenDocumentIntent()`, `buildCreateDocumentIntent()`, and `persistUriPermissionIfPossible()` (`contentResolver.takePersistableUriPermission`).

### 2.3 S3 — Deterministic Temporary File Cleanup
1. **Tracked Ephemeral Artifacts:**
   - `mTempFile` in `LOActivity` (`new File(getCacheDir(), displayName)`)
   - `print.pdf` in `PrintAdapter` (`new File(mActivity.getCacheDir(), "print.pdf")`)
   - `lok_clip_*` temporary files in `getCacheDir()`
2. **Deterministic Cleanup Execution:**
   - `LOActivity.purgeTemporaryCacheFiles()` is executed in `onPause()` (when `isFinishing()`) and unconditionally in `onDestroy()`.
   - `PrintAdapter.onFinish()` deletes `printDocFile` immediately after streaming to the print spooler.
   - `TempFileGuard.scrubAppCacheDirectory()` verifies canonical paths so that **only** files inside the app's private `cacheDir` are deleted, guaranteeing user-owned source files are never touched.
   - Verified by unit test `P1SecurityRemediationTest.s3_tempFileGuard_deletesEphemeralCacheAndPreservesUserSourceFiles` (`PASS`).

### 2.4 S4 — Broadcast Isolation & Component Export Hardening
1. **Package-Scoped Broadcasts:**
   All internal `LOActivityBroadcast` intents in `LOActivity.java` call `intent.setPackage(getPackageName())` before `sendBroadcast(intent)`, preventing any third-party app from eavesdropping on document events.
2. **Merged Manifest Component Audit (`aapt2 dump xmltree`):**
   - `android:allowBackup="false"`, `android:fullBackupContent="false"`, `android:dataExtractionRules="@null"`
   - `android:usesCleartextTraffic="false"`
   - `org.libreoffice.androidlib.LOActivity`: `android:exported="false"`
   - `org.libreoffice.androidlib.SlideShowActivity`: `android:exported="false"`
   - `androidx.core.content.FileProvider`: `android:exported="false"`, distinct authorities (`com.eurowindow.documents.fileprovider` vs `com.eurowindow.sheets.fileprovider`)
   - `org.libreoffice.androidlib.ClipboardFileProvider`: `android:exported="false"`, distinct authorities (`com.eurowindow.documents.clipboard` vs `com.eurowindow.sheets.clipboard`)
