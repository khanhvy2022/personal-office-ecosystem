# P1 — OFFLINE VALIDATION REPORT (SECTION 7)
## Binary Manifest, In-Process Engine & Airplane Mode Validation

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**Overall Offline Validation Status:** **PARTIAL** (Binary APK Offline Enforcement **PASS**; Physical Device Airplane-Mode Execution **BLOCKED** awaiting ADB connection)

---

## 1. Binary & Architectural Offline Proof (Host-Verified)

Unlike cloud or hybrid office viewers, both `com.eurowindow.documents` and `com.eurowindow.sheets` are **physically incapable of opening network connections** at the Android OS kernel level:

1. **Zero `INTERNET` Permission in Final APKs (`logs/p1-aapt2-documents-permissions.log` & `logs/p1-aapt2-sheets-permissions.log`):**
   - `com.eurowindow.documents` permissions: `com.eurowindow.documents.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` only.
   - `com.eurowindow.sheets` permissions: `com.eurowindow.sheets.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` only.
   - Without `android.permission.INTERNET`, the Linux kernel (`CONFIG_ANDROID_PARANOID_NETWORK`) denies `AF_INET` / `AF_INET6` socket creation for the app UID regardless of Wi-Fi or cellular state.
2. **In-Process `cool://` MobileSocket Transport:**
   - `LOActivity` loads `file:///android_asset/dist/cool.html` into `COWebView`.
   - All communication between the HTML/JS canvas frontend and `liblo-native-code.so` passes through `COWebViewClient.shouldInterceptRequest()` (`cool://localhost/cool/mobilesocket/...`) backed by in-memory Java/JNI queues (`MobileSocket.java` <-> `libandroidapp.so`). No TCP/IP loopback or remote server is used.
3. **WebView Network Blockade:**
   - `WebSettings.setBlockNetworkLoads(true)` and `setBlockNetworkImage(true)` are enforced before loading `cool.html`.
   - `COWebViewClient.isAllowedOfflineUri()` and `EWOfflineUrlPolicy.isAllowedUrl()` reject all `http://`, `https://`, `ws://`, and `wss://` URLs (`P1SecurityRemediationTest.s1_offlineUrlPolicy_allowsCoolAndAssetSchemesOnly` = **PASS**).

---

## 2. Physical Device Airplane-Mode Checklist (Pending ADB Connection)

Per Section 7 (*"Do not claim offline PASS based only on source inspection"*), the following on-device Airplane Mode checks remain **BLOCKED** until the physical Android 16 phone is connected via ADB:

| Test Case | Airplane Mode Command / Action | Status |
| :--- | :--- | :---: |
| **1. Enable Airplane Mode** | `adb shell cmd connectivity airplane-mode enable` | **BLOCKED** (No ADB device) |
| **2. Cold Start** | `adb shell am start -W -n com.eurowindow.documents/.DocumentsPrototypeActivity` | **BLOCKED** (No ADB device) |
| **3. Open Document** | Open `01_vietnamese_unicode_small.docx` & `01_formulas_multisheet_small.xlsx` | **BLOCKED** (No ADB device) |
| **4. Edit & Save** | Modify text/formulas and save via LOKit / SAF | **BLOCKED** (No ADB device) |
| **5. Close & Reopen** | Close activity, reopen saved file, verify persistence | **BLOCKED** (No ADB device) |
| **6. Export PDF** | Trigger `Save As PDF` (`directExportToPdf`) in Airplane Mode | **BLOCKED** (No ADB device) |
