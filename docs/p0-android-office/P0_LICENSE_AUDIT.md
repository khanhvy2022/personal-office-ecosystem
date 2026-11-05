# P0 — LICENSE & REDISTRIBUTION AUDIT
## EW Personal Office Ecosystem (`EW Documents` & `EW Sheets`)

**Project Root:** `F:\word và Excel`  
**Date:** 2026-11-04  
**Phase:** P0 — Research & Feasibility Only  
**Companion Inventory:** `THIRD_PARTY_LICENSES.md`

---

## 1. SCOPE & AUDIT METHODOLOGY

This audit evaluates the legal, licensing, source disclosure, modification, redistribution, and branding obligations across all three evaluated upstream candidates (`CollaboraOnline/online.mirror`, `verycareful/kern`, `ONLYOFFICE`) and the proposed Android Kotlin/Jetpack tech stack.

**Evidence Sources Inspected:**
1. `CollaboraOnline/online.mirror/COPYING` & `README.FILENOTICES.md`
2. `CollaboraOnline/online.mirror/SBOM.md` & `scripts/sbom-externals.json`
3. `CollaboraOnline/online.mirror/THIRDPARTYLICENSES` & `CODA-THIRDPARTYLICENSES.html`
4. `CollaboraOnline/online.mirror/engine/readlicense_oo/license/license.xml` (251 component license declarations)
5. `CollaboraOnline/online.mirror/android/lib/build.gradle` (Asset packaging tasks `copyUnpackAssets` and `copyAssets`)
6. `verycareful/kern/LICENSE` (`AGPL-3.0`), `gradle/libs.versions.toml`, and `licenses/index.tsv`
7. `ONLYOFFICE/core/LICENSE` & `ONLYOFFICE/editors-webview-android/LICENSE` (`AGPL-3.0`)

---

## 2. LICENSE FAMILY ANALYSIS FOR ANDROID APK DISTRIBUTION

### 2.1 Mozilla Public License 2.0 (`MPL-2.0`) — Collabora Core & Online
- **Applies to:** Collabora Office Engine (`engine/`), Collabora Online (`wsd/`, `kit/`, `common/`, `net/`, `browser/`), and Collabora Android Library (`android/lib/`, `android/app/`).
- **Nature of Copyleft:** **Weak, File-Level Copyleft** (MPL-2.0 Section 1.10 "Modifications" & Section 3.3 "Distribution of a Larger Work").
- **Permitted Use & Redistribution:**
  - Explicitly permits **personal use, internal enterprise use, and binary APK redistribution** (`Section 2.1` Grant of Rights).
  - Explicitly permits combining MPL-2.0 Covered Software (`android/lib`, `liblo-native-code.so`, `libandroidapp.so`) with separate files under different licenses (`Apache-2.0` or proprietary/custom Kotlin modules `:app-documents`, `:app-sheets`, `:core:*`) to form a **Larger Work** (`Section 3.3`), provided MPL-2.0 requirements are satisfied for the MPL-covered files.
- **Mandatory Obligations When Distributing APKs:**
  1. **Source Code Availability for MPL-Covered Files (`Section 3.2`):** Any MPL-2.0 file that is included in or modified for the APK (e.g., `LOActivity.java`, `androidapp.cpp`, `CMakeLists.txt.in`) must retain its MPL-2.0 header (`README.FILENOTICES.md`), and you must inform recipients how they can obtain the Source Code Form of those MPL-2.0 files (e.g., linking to the upstream/forked repository in the app's "About / Open Source Licenses" screen).
  2. **License Notice Preservation (`Section 3.1` & `3.2`):** Bundle `license.html` (`LICENSE.html`), `notice.txt` (`NOTICE`), and `THIRDPARTYLICENSES` inside the APK (`src/main/assets/`) and expose them via an in-app viewer (`ShowHTMLActivity` or Compose License screen).

### 2.2 Dual/Tri-Licensed Libraries Inside Collabora Engine (`LGPL` / `MPL` / `GPL`)
- **Finding:** Inspection of all 251 entries in `engine/readlicense_oo/license/license.xml` confirms that libraries statically linked into `liblo-native-code.so` which carry dual/tri-licenses in upstream projects — such as `hunspell` (`LGPL-2.1-or-later OR MPL-1.1 OR GPL-2.0-or-later`), `hyphen`, `graphite2` (`LGPL-2.1-or-later OR MPL-2.0 OR GPL-2.0-or-later`), `libwpd`, `libwps`, `libwpg`, `librevenge`, `libodfgen`, `libmwaw`, `liblangtag`, `raptor`, `rasqal`, `redland`, and `zstd` (`BSD-3-Clause OR GPL-2.0-only`) — are **all elected and concluded under `MPL-2.0` (or permissive `BSD-3-Clause`/`Apache-2.0`)** by the LibreOffice/Collabora build system (`Concluded: MPL-2.0`).
- **Result:** Static linking of these libraries into `liblo-native-code.so` does **NOT** trigger GPL or LGPL relinking requirements on the APK because their permissive/MPL-2.0 license option is exercised.

### 2.3 CRITICAL FINDING: Standalone GPL/LGPL Dictionary & Font Assets in Collabora
- **Finding:** While the C++ binaries in `liblo-native-code.so` are clean `MPL-2.0`, our audit of `engine/readlicense_oo/license/license.xml` and `android/lib/build.gradle` (lines 553–598) uncovered two categories of **non-code asset files** that carry `GPL` or `LGPL` licenses:
  1. **Hunspell Dictionaries (`copyUnpackAssets` lines 587–598):**
     - `android/lib/build.gradle` copies `extensions/dict-de/**`, `extensions/dict-en/**`, `extensions/dict-es/**`, `extensions/dict-fr/**`, `extensions/dict-pt-BR/**`.
     - Per `license.xml`, `dict-de-spell` is `GPL-2.0 OR GPL-3.0` (`Concluded: GPL-2.0 OR GPL-3.0`), `dict-de-hyph` and `dict-de-thes` are `LGPL-2.1`, `dict-es-thes` is `LGPL-2.1`, `dict-fr-thes` is `LGPL-2.1`, and `dict-vi-spell` (if enabled) is `GPL-2.0`.
  2. **Culmus Hebrew Font (`license.xml` line 11):**
     - `Culmus` (`FONT_CULMUS_TARBALL`) is `Declared: GPL-2.0, Concluded: GPL-2.0`. Note that `android/lib/build.gradle` (lines 557–564) already restricts Android bundled fonts to `EmojiOne*.ttf`, `Liberation*.ttf` (`OFL-1.1`), `Caladea-*.ttf` (`OFL-1.1`), `Carlito-*.ttf` (`OFL-1.1`), `Gen*.ttf` (`OFL-1.1`), and `opens___.ttf`, so `Culmus` is NOT copied into the Android APK.
- **Remediation / Mitigation for `EW Documents` & `EW Sheets`:**
  - Flagged as **REQUIRES LEGAL REVIEW IF BUNDLED**.
  - **Recommended Technical Action in P1:** In `android/lib/build.gradle` (`copyUnpackAssets`), **remove the GPL/LGPL dictionary extensions** (`extensions/dict-de/**`, etc.) or include only permissive (`MIT` / `BSD` / `MPL-2.0`) dictionaries, relying on Android's native system keyboard spellchecker (Gboard / Samsung Keyboard) for Vietnamese and English input. This eliminates 100% of GPL/LGPL dictionary assets from both APKs while also saving ~12–18 MB of APK size.

### 2.4 GNU Affero General Public License v3.0 (`AGPL-3.0`) — Kern & ONLYOFFICE
- **Applies to:**
  - `verycareful/kern` (`LICENSE` = `AGPL-3.0`, plus `elkjs` under `GPL-3.0-or-later` inside `assets/preview/mermaid/mermaid.min.js`).
  - `ONLYOFFICE/core` and `ONLYOFFICE/editors-webview-android` (`LICENSE` = `AGPL-3.0`).
- **Copyleft Impact:**
  - **Strong Copyleft:** Copying or modifying any source file from `verycareful/kern` or `ONLYOFFICE` into `EW Documents` or `EW Sheets` forces the entire resulting Android application to be licensed under `AGPL-3.0`.
- **Policy Decision:**
  - **Do NOT copy code from `verycareful/kern` or `ONLYOFFICE` into `EW Documents` or `EW Sheets`.**
  - Note: `Apache POI 5.5.1` (`org.apache.poi:poi-ooxml`), which Kern uses as a Maven dependency, is owned by the Apache Software Foundation under **Apache-2.0**. If any lightweight XML inspection utility is needed, Apache POI or standard Android `XmlPullParser` can be used directly from Maven Central under `Apache-2.0` without touching Kern's AGPL-3.0 code.

### 2.5 Apache License 2.0, MIT, BSD-3-Clause, BSL-1.0, OFL-1.1
- **Applies to:**
  - Android Jetpack (`Compose`, `Room`, `DataStore`, `WorkManager`, `Lifecycle`, `Core-KTX`, `AppCompat`, `Material`) — `Apache-2.0`.
  - Kotlin Standard Library & Coroutines — `Apache-2.0`.
  - POCO C++ Libraries (`BSL-1.0`), OpenSSL 3.x (`Apache-2.0`), zlib (`Zlib`), libpng (`Libpng`), Expat (`MIT`), Skia (`BSD-3-Clause`), PDFium (`BSD-3-Clause`), HarfBuzz (`MIT`), ICU (`ICU`), libxml2/libxslt (`MIT`).
  - Bundled Fonts (`Liberation`, `Carlito`, `Caladea`, `Gentium`) — `SIL Open Font License 1.1 (OFL-1.1)`.
- **Obligations:** Permissive; requires only preserving copyright notices, license texts, and `NOTICE` attributions in `THIRD_PARTY_LICENSES.md` and the in-app "Open Source Licenses" screen.

---

## 3. BRANDING & TRADEMARK RESTRICTIONS AUDIT

| Component | Branding / Trademark Rule | Technical Enforcement in EW Ecosystem |
|---|---|---|
| **Collabora Online / Office (`Collabora Productivity Limited`)** | Collabora name, logos, and `browser/branding` (`CollaboraOnline-branding`, Apache-2.0) are trademarks/branding of Collabora.MPL-2.0 Exhibit A states that no trademark rights are granted. | Must configure engine/online build with `--disable-branding` or `--with-app-branding=<ew_branding_dir>`, `--with-app-name="EW Documents"` / `"EW Sheets"`, and `--with-vendor="Eurowindow Personal Office"`. Replace `ic_launcher` and `drawer_header.png` in `:app-documents` and `:app-sheets`. |
| **LibreOffice (`The Document Foundation`)** | "LibreOffice" is a trademark of The Document Foundation. | Package names are `com.eurowindow.documents` and `com.eurowindow.sheets`; user-facing titles are `EW Documents` and `EW Sheets`. Internal Java package `org.libreoffice.androidlib` is kept only as internal JNI symbol namespace, while attribution is given in the About screen (*"Powered by Collabora Office / LibreOfficeKit technology under MPL-2.0"*). |
| **SIL Open Font License 1.1 (`Liberation`, `Carlito`, `Caladea`)** | Reserved Font Names cannot be used to name the app itself, and fonts cannot be sold by themselves. | Bundling unmodified `.ttf` files inside an application APK is explicitly permitted by OFL-1.1 FAQ & Section 3. |

---

## 4. CASES FLAGGED FOR ADDITIONAL LEGAL REVIEW

1. **Flag L-01 (Hunspell Dictionary Extensions in Collabora):** If `dict-de`, `dict-fr`, `dict-es`, or `dict-vi` Hunspell `.oxt`/`.dic` files are bundled in `assets/unpack/share/extensions/`, several are `GPL-2.0`/`GPL-3.0`/`LGPL-2.1`. **Resolution:** Exclude all GPL/LGPL Hunspell dictionaries in P1 build configuration (`copyUnpackAssets`).
2. **Flag L-02 (F-Droid Binary Extraction in P1 Prototype):** If P1 extracts `liblo-native-code.so` from Collabora's official F-Droid APK (`com.collabora.libreoffice` v`26.04.4.2`) for rapid prototyping on Windows before Linux CI is set up, `CollaboraOnline-branding` assets and Collabora logos MUST be stripped, and the exact upstream source tag/commit (`26.04.4.2`) MUST be linked in `THIRD_PARTY_LICENSES.md` to satisfy MPL-2.0 Section 3.2.

---

## 5. GATE G1 VERDICT

- **Gate G1 (License Verified):** **PASS**
  - Collabora's `MPL-2.0` licensing model + permissive `Apache-2.0`/`MIT`/`BSD`/`OFL-1.1` dependencies legally permit personal use, modification, custom branding (`EW Documents` & `EW Sheets`), and standalone APK distribution when GPL dictionary assets and Collabora trademark assets are excluded and MPL-2.0 source notices are included.
