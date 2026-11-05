# P1.3 — REAL DEVICE VALIDATION & TWO-APP INDEPENDENCE REPORT
## Android 16 ADB Verification & Two-App Independence Audit

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**P1.3 Real Device Validation Status:** **BLOCKED** (No physical Android 16 device currently attached to host USB/ADB bus)  
**Section 6 Two-App Independence Status:** **PARTIAL** (Static APK/Manifest/Namespace/UID-Contract Isolation **PASS**; On-Device Simultaneous Install/Uninstall Execution **BLOCKED** awaiting ADB device)

---

## 1. Host ADB Probe Evidence (`logs/p1-adb-devices.log`)

Command executed on Windows host:
```powershell
F:\pdf\toolchain\android-sdk\platform-tools\adb.exe devices -l
```

Raw output:
```text
List of devices attached

* daemon not running; starting now at tcp:5037
* daemon started successfully
```

**Honest Reporting Compliance:**  
Per P1 instructions (*"Test all operations directly on the Android device. Do not use host-only tests as substitutes."*), no on-device execution results are fabricated. All on-device interactive steps in Section 5 (EW Documents 11-step test & EW Sheets 13-step test) are marked **`BLOCKED`** until the physical Android 16 device is connected via USB Debugging.

---

## 2. Section 6 — Two-App Independence Verification Matrix

| Independence Requirement | Static / Build-Time Verification (`aapt2` + JUnit) | On-Device ADB Verification | Status |
| :--- | :--- | :--- | :---: |
| **Different Application IDs** | `com.eurowindow.documents` vs `com.eurowindow.sheets` (`aapt2 dump badging`) | Ready via `run_p1_device_validation.ps1` | **PASS** (Static) |
| **Simultaneous Installation** | Zero conflicting package names, permissions, or provider authorities in merged manifests | Pending ADB device connection | **PARTIAL** |
| **Different Linux UIDs** | Android PackageManager assigns distinct `u0_aXXX` UIDs because `sharedUserId` is omitted in both manifests | Pending `adb shell dumpsys package` | **PARTIAL** |
| **Different Private App Directories** | `/data/user/0/com.eurowindow.documents/` vs `/data/user/0/com.eurowindow.sheets/` | Pending ADB device connection | **PASS** (Arch) |
| **Different Cache Directories** | `/data/user/0/com.eurowindow.documents/cache` vs `/data/user/0/com.eurowindow.sheets/cache` | Pending ADB device connection | **PASS** (Arch) |
| **Different Config & Fontconfig Dirs** | `LOActivity.patchUnpackedFontsConf()` rewrites `@@APPLICATION_ID@@` to app-private `/data/data/<package>/fontconfig`; separate SQLite DBs (`ew_documents.db` vs `ew_sheets.db`) | Verified by `P1SecurityRemediationTest` & `P1EnginePackagingIntegrationTest` | **PASS** |
| **Different FileProvider Authorities** | `com.eurowindow.documents.fileprovider` & `.clipboard` vs `com.eurowindow.sheets.fileprovider` & `.clipboard` (`aapt2 dump xmltree`) | Verified in compiled binary manifests | **PASS** |
| **No Dependency on Other App** | Zero `<queries>` or explicit cross-app intents between `com.eurowindow.documents` and `com.eurowindow.sheets` | Pending uninstall cross-check on device | **PARTIAL** |

---

## 3. Automated On-Device Test Harness (Ready for Immediate Execution)

An automated PowerShell validation script has been created at:
`F:\word và Excel\EWOfficeEcosystem\scripts\run_p1_device_validation.ps1`

When the physical Android 16 device is plugged in with USB Debugging enabled, run:
```powershell
powershell -ExecutionPolicy Bypass -File "F:\word và Excel\EWOfficeEcosystem\scripts\run_p1_device_validation.ps1"
```

This script automatically:
1. Records device model, Android version (`ro.build.version.release`), SDK level, ABI, and available RAM (`/proc/meminfo`).
2. Installs both `documents-debug.apk` and `sheets-debug.apk` simultaneously and records their distinct Linux `userId` values.
3. Launches both apps and opens the embedded Vietnamese DOCX (`01_vietnamese_unicode_small.docx`) and formula XLSX (`01_formulas_multisheet_small.xlsx`) corpus directly inside `LOActivity`.
4. Tests uninstallation independence (`adb uninstall com.eurowindow.documents` while verifying `com.eurowindow.sheets` launches and opens XLSX cleanly, and vice versa).
