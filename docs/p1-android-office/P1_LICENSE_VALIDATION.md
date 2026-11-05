# P1 — LICENSE & BRANDING VALIDATION REPORT (SECTION 9)
## Dependency License Audit, Dictionary Verification & Branding Removal

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**Status:** **PASS**

---

## 1. Packaged APK License Verification Summary

Both built APKs (`documents-debug.apk` and `sheets-debug.apk`) were inspected entry-by-entry via `inspect_built_apks.py` (results logged in `F:\word và Excel\logs\p1_apk_inspection_summary.json`):

| License Check | `documents-debug.apk` | `sheets-debug.apk` | Status |
| :--- | :--- | :--- | :---: |
| **GPL / LGPL Dictionary Files (`.dic`, `.aff`, `.oxt`)** | **`0` files** (`dictionary_files_count = 0`) | **`0` files** (`dictionary_files_count = 0`) | **PASS** |
| **Bundled Fonts (`assets/unpack/user/fonts/`)** | `35` font files (`OFL-1.1` & `MPL-2.0` only) | `35` font files (`OFL-1.1` & `MPL-2.0` only) | **PASS** |
| **Collabora Trademark / Branding Removal** | Replaced in `branding.js`, `branding.css`, `cool.html`, `bootstraprc` (`ProductKey=EW Personal Office 26.04`) | Replaced in `branding.js`, `branding.css`, `cool.html`, `bootstraprc` (`ProductKey=EW Personal Office 26.04`) | **PASS** |
| **Bundled Third-Party License Notices** | `assets/THIRD_PARTY_LICENSES.md` (`true`) + `assets/dist/license.txt` | `assets/THIRD_PARTY_LICENSES.md` (`true`) + `assets/dist/license.txt` | **PASS** |
| **MPL-2.0 Source File Headers Preserved** | Preserved on all `org.libreoffice.androidlib.*` files | Preserved on all `org.libreoffice.androidlib.*` files | **PASS** |

---

## 2. Bundled Component License Inventory

| Component | Version | License | Verification Notes |
| :--- | :--- | :--- | :--- |
| **LibreOfficeKit / Collabora Core (`liblo-native-code.so`, `libandroidapp.so`)** | `26.04.4.2-937` | `MPL-2.0` | Official Collabora build; zero GPL/LGPL dictionaries included |
| **Mozilla NSS & NSPR (`libnss3.so`, `libnspr4.so`, `libsoftokn3.so`, etc.)** | Bundled | `MPL-2.0` | File-level copyleft compatible with `MPL-2.0` |
| **SQLite (`libsqlite3.so`)** | Bundled | Public Domain | Unrestricted |
| **LLVM `libc++_shared.so`** | NDK r26+ | `Apache-2.0 WITH LLVM-exception` | Permissive |
| **Bundled Fonts (`Caladea`, `Carlito`, `Gentium`, `Liberation`, `OpenSymbol`)** | Bundled | `OFL-1.1` / `MPL-2.0` | Permissive font redistribution |
| **AndroidX & Jetpack Compose Libraries** | `2024.09.00` / `1.7.0` | `Apache-2.0` | Permissive |
