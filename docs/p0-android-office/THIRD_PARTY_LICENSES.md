# THIRD-PARTY LICENSES & OPEN SOURCE NOTICES
## EW Personal Office Ecosystem (`EW Documents` & `EW Sheets`)

**Project Root:** `F:\word và Excel`  
**Generated Date:** 2026-11-04  
**Phase:** P0 Baseline Inventory

---

## 1. PRIMARY ENGINE & CANDIDATE REPOSITORY INVENTORY

| Component / Dependency | Repository / Upstream URL | Version | Verified Commit / Tag | Declared / Concluded License | Source Disclosure Requirements | Modification Obligations | Redistribution Conditions | Branding Restrictions | Legal Review Flag |
|---|---|---|---|---|---|---|---|---|---|
| **Collabora Online & Android Library (`android/lib`, `wsd`, `kit`, `browser`)** | `https://github.com/CollaboraOnline/online.mirror` (`https://gerrit.collaboraoffice.com/online`) | `26.04.4.2` / `main` | `a1d7358c4556f10b2f4bb7cb44550e7f57e5214b` (2026-11-04) | **MPL-2.0** | **Yes (File-level):** Must provide URL/means to obtain source code of all MPL-2.0 covered files (`Section 3.2`). | Modified MPL-2.0 files must stay MPL-2.0 and retain copyright headers (`README.FILENOTICES.md`). | Include `LICENSE.html`, `NOTICE`, and `THIRDPARTYLICENSES` in APK assets & About screen. | Must use `--disable-branding`; cannot use Collabora trademarks/logos as app identity. | **Cleared for Personal & APK Distribution (MPL-2.0)** |
| **Collabora Office Core Engine (`engine/` -> `liblo-native-code.so`)** | `https://github.com/CollaboraOnline/online.mirror/tree/main/engine` | `26.04.4.2` | `a1d7358c4556f10b2f4bb7cb44550e7f57e5214b` | **MPL-2.0** (with dual-licensed LGPL/MPL modules elected as MPL-2.0) | **Yes (File-level):** Point to upstream Gerrit/GitHub commit hash used to build `liblo-native-code.so`. | Any patched C++ files in `engine/` must be published under MPL-2.0. | Include `license.html` and `notice.txt` in APK. | Do not claim endorsement by The Document Foundation or Collabora. | **Cleared (MPL-2.0)** |
| **Kern (`verycareful/kern`) — *Evaluated Only, Not Bundled*** | `https://github.com/verycareful/kern` | `0.1.14.3` | `0358f2c8b99b42483a0c74253d6f1b86451d97fa` (2026-11-03) | **AGPL-3.0** | **Full App Source Disclosure** if any code is copied. | Strong copyleft across entire app. | Entire derivative work must be AGPL-3.0. | None specified beyond AGPL-3.0. | **DO NOT COPY CODE (AGPL-3.0 Contamination Risk)** |
| **ONLYOFFICE (`ONLYOFFICE/core` & `editors-webview-android`) — *Evaluated Only, Not Bundled*** | `https://github.com/ONLYOFFICE/core` | `v10.0.0` | `ed63e94de814ff1d4ea28cddfaba6939bc9df9ed` (2026-10-15) | **AGPL-3.0** | **Full App Source Disclosure** if used. | Strong copyleft across entire app and network users. | Must remain AGPL-3.0 and retain interactive user notices. | Ascensio System SIA trademark & interactive attribution rules. | **REJECTED / DO NOT USE (AGPL-3.0)** |

---

## 2. NATIVE C/C++ LIBRARIES BUNDLED IN COLLABORA ENGINE (`libandroidapp.so` & `liblo-native-code.so`)

Verified from `CollaboraOnline/online.mirror/scripts/sbom-externals.json`, `THIRDPARTYLICENSES`, `CMakeLists.txt.in`, and `engine/readlicense_oo/license/license.xml`:

| Library | Upstream URL | Linkage in APK | Declared License | Concluded License in Build | Source Disclosure Required? | Notice / Redistribution Obligations | Legal Review Flag |
|---|---|---|---|---|---|---|---|
| **POCO C++ Libraries (`libPocoNet.a`, `libPocoUtil.a`, `libPocoXML.a`, `libPocoJSON.a`, `libPocoFoundation.a`)** | `https://pocoproject.org/` | Static in `libandroidapp.so` | `BSL-1.0` (Boost Software License 1.0) | `MPL-2.0` (Binary) / `BSL-1.0` | No (Binary exemption in BSL-1.0, reproduced in `THIRDPARTYLICENSES`) | Preserve Boost notice in `THIRDPARTYLICENSES`. | None (Permissive) |
| **OpenSSL (`libssl3.so` / `crypto`)** | `https://www.openssl.org/` | Static / Shared | `Apache-2.0` | `MPL-2.0` / `Apache-2.0` | No | Include Apache-2.0 notice in `THIRDPARTYLICENSES`. | None (Permissive) |
| **Mozilla NSS & NSPR (`libnss3.so`, `libnspr4.so`, `libfreebl3.so`, `libsoftokn3.so`, `libsmime3.so`, `libplc4.so`, `libplds4.so`, `libnssutil3.so`, `libnssckbi.so`, `libnssdbm3.so`)** | `https://ftp.mozilla.org/pub/security/nss/releases/` | Dynamic `.so` in `jniLibs/arm64-v8a` | `MPL-2.0` | `MPL-2.0` | Yes (File-level MPL-2.0; unmodified upstream tarball in `engine/download.lst`) | Preserve MPL-2.0 notice and pointer to upstream NSS tarball. | None (MPL-2.0) |
| **SQLite (`libsqlite3.so`)** | `https://sqlite.org/` | Dynamic `.so` in `jniLibs/arm64-v8a` | `SQLite Blessing` (Public Domain) | `MPL-2.0` | No | None | None (Public Domain) |
| **Zstandard (`libzstd.a`)** | `https://facebook.github.io/zstd/` | Static in `libandroidapp.so` | `BSD-3-Clause OR GPL-2.0-only` | `MPL-2.0` (via `BSD-3-Clause` election) | No | Retain BSD-3-Clause copyright notice in `THIRDPARTYLICENSES`. | None (BSD-3-Clause elected) |
| **zlib (`libz`)** | `http://www.zlib.net/` | Static / Bionic NDK | `Zlib` | `MPL-2.0` | No | Retain zlib notice in `license.html`. | None (Permissive) |
| **libpng (`liblibpng.a`)** | `http://www.libpng.org/` | Static in `libandroidapp.so` | `Libpng` | `MPL-2.0` | No | Retain libpng notice in `license.html`. | None (Permissive) |
| **Expat (`libexpat.a`)** | `https://libexpat.github.io/` | Static in `libandroidapp.so` | `MIT` | `MPL-2.0` | No | Retain MIT copyright notice. | None (Permissive) |
| **ICU (`International Components for Unicode`)** | `https://icu.unicode.org/` | Static in `liblo-native-code.so` + `ICU.dat` | `ICU` (Unicode-DFS-2016 / MIT-style) | `MPL-2.0` | No | Retain Unicode/ICU copyright notice in `license.html`. | None (Permissive) |
| **HarfBuzz & Graphite2** | `https://harfbuzz.github.io/` | Static in `liblo-native-code.so` | `MIT` (HarfBuzz) / `LGPL-2.1+ OR MPL-2.0 OR GPL-2.0+` (Graphite2) | `MPL-2.0` | File-level for Graphite2 under MPL-2.0 | Retain notices in `license.html`. | None (MPL-2.0 elected) |
| **PDFium & Skia** | `https://pdfium.googlesource.com/pdfium/` | Static in `liblo-native-code.so` | `BSD-3-Clause` | `MPL-2.0` | No | Retain Google BSD-3-Clause notice in `license.html`. | None (Permissive) |
| **libxml2 & libxslt** | `http://xmlsoft.org/` | Static in `liblo-native-code.so` | `MIT` | `MPL-2.0` | No | Retain MIT notice in `license.html`. | None (Permissive) |
| **libjpeg-turbo & libwebp** | `https://www.libjpeg-turbo.org/` | Static in `liblo-native-code.so` | `IJG AND BSD-3-Clause AND Zlib` / `BSD-3-Clause` | `MPL-2.0` | No | Retain BSD/IJG notices in `license.html`. | None (Permissive) |
| **Document Import Filters (`libwpd`, `libwps`, `libwpg`, `librevenge`, `libodfgen`, `libmwaw`, `libetonyek`, `libvisio`, `liborcus`)** | `https://wiki.documentfoundation.org/DLP/Libraries` | Static in `liblo-native-code.so` | `MPL-2.0` or `LGPL-2.1+ OR MPL-2.0` | `MPL-2.0` | File-level MPL-2.0 | Retain MPL-2.0 notices in `license.html`. | None (MPL-2.0 elected) |
| **Hunspell & Hyphen C++ Libraries** | `https://hunspell.github.io/` | Static in `liblo-native-code.so` | `LGPL-2.1-or-later OR MPL-1.1 OR GPL-2.0-or-later` | `MPL-2.0` | File-level MPL | Retain notices in `license.html`. | None for C++ library (MPL elected) |
| **Hunspell Dictionary Data Extensions (`dict-de`, `dict-fr`, `dict-es`, `dict-vi`)** | `engine/dictionaries` | Optional asset files in `assets/unpack/share/extensions/` | Various (`GPL-2.0`, `GPL-3.0`, `LGPL-2.1`, `MPL-2.0`) | `GPL-2.0` / `LGPL-2.1` | **Yes if bundled** | Copyleft dictionary files. | **FLAG L-01: EXCLUDE FROM APK BUILD (`copyUnpackAssets`)** |

---

## 3. BUNDLED FONTS INVENTORY (`assets/unpack/user/fonts`)

| Font Family | Files | License | Redistribution & Embedding Rules | Legal Review Flag |
|---|---|---|---|---|
| **Liberation Fonts** | `LiberationSans-*.ttf`, `LiberationSerif-*.ttf`, `LiberationMono-*.ttf` | `SIL Open Font License 1.1 (OFL-1.1)` | Freely redistributable when bundled with software; retain OFL-1.1 notice; do not sell font standalone. | None (Cleared) |
| **Carlito (Calibri metric-compatible)** | `Carlito-Regular.ttf`, `Carlito-Bold.ttf`, `Carlito-Italic.ttf`, `Carlito-BoldItalic.ttf` | `OFL-1.1` | Same as above. | None (Cleared) |
| **Caladea (Cambria metric-compatible)** | `Caladea-Regular.ttf`, `Caladea-Bold.ttf`, `Caladea-Italic.ttf`, `Caladea-BoldItalic.ttf` | `OFL-1.1` | Same as above. | None (Cleared) |
| **Gentium** | `Gen*.ttf` | `OFL-1.1` | Same as above. | None (Cleared) |
| **OpenSymbol** | `opens___.ttf` | `MPL-2.0` | Covered by LibreOffice MPL-2.0 notice. | None (Cleared) |

---

## 4. ANDROID KOTLIN / JETPACK DEPENDENCIES (`:app-documents`, `:app-sheets`, `:core:*`)

| Dependency Coordinates | Version | License | Source Disclosure Required? | Notice Requirement |
|---|---|---|---|---|
| `org.jetbrains.kotlin:kotlin-stdlib` & `kotlinx-coroutines-android` | `2.1.0` / `1.9.0` | `Apache-2.0` | No | Include Apache-2.0 license & JetBrains copyright notice. |
| `androidx.compose:compose-bom` (`ui`, `material3`, `foundation`, `runtime`) | `2024.10.01+` / `2026.05.01` | `Apache-2.0` | No | Include Apache-2.0 license & AOSP copyright notice. |
| `androidx.core:core-ktx`, `androidx.appcompat:appcompat`, `androidx.activity:activity-compose` | `1.13.1+` / `1.7.1` / `1.9.3` | `Apache-2.0` | No | Include Apache-2.0 license. |
| `androidx.lifecycle:lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose` | `2.8.7` | `Apache-2.0` | No | Include Apache-2.0 license. |
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` | `2.6.1+` | `Apache-2.0` | No | Include Apache-2.0 license. |
| `androidx.datastore:datastore-preferences` | `1.1.1+` | `Apache-2.0` | No | Include Apache-2.0 license. |
| `androidx.work:work-runtime-ktx` | `2.9.1+` | `Apache-2.0` | No | Include Apache-2.0 license. |
| `com.google.android.material:material` | `1.13.0` | `Apache-2.0` | No | Include Apache-2.0 license. |
| `junit:junit`, `androidx.test.ext:junit`, `androidx.test.espresso:espresso-core` | `4.13.2` / `1.2.1` / `3.6.1` | `EPL-1.0` / `Apache-2.0` | Test-only (Not packaged in release APK) | None in release APK. |
