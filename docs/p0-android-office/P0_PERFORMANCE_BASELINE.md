# P0 — PERFORMANCE BASELINE & DOCUMENT COMPATIBILITY REPORT
## EW Personal Office Ecosystem (`EW Documents` & `EW Sheets`)

**Project Root:** `F:\word và Excel`  
**Date:** 2026-11-04  
**Phase:** P0 — Research & Feasibility Only  
**Log Evidence Path:** `F:\word và Excel\logs\corpus_verification_log.json`

---

## 1. STRICT EVIDENCE INTEGRITY POLICY

Per Section 10 & Section 11 of the P0 Specification:
- *"Mọi số liệu phải có test environment và log. Không tự đặt kết quả benchmark."*
- Because P0 strictly prohibits building production apps or modifying host configurations, and because no physical Android 16 phone is currently connected (`adb.exe devices` = empty), this report separates:
  1. **Empirical Measurements Executed in P0 on Windows Host (`10.0.19044`):**
     - Upstream binary APK sizes verified from Collabora's official F-Droid index (`index-v1.json`) and local `PersonalPDFStudio` baseline (`F:\pdf\PersonalPDFStudio\reports\P0\final-evidence.json`).
     - Full 7-step OOXML (`DOCX` & `XLSX`) Test Corpus generation, `.NET XmlDocument` DOM schema validation, and non-destructive round-trip (`Open -> Parse -> Edit Validation -> Save -> Close -> Reopen -> Byte-Exact Compare`) benchmarked in `F:\word và Excel\logs\corpus_verification_log.json`.
  2. **On-Device Android Runtime Metrics (Cold Start, Warm Start, Native COKit Render/PDF Export Time, Device RAM/CPU):**
     - Marked explicitly as `PENDING_P1_DEVICE_EXECUTION` (requiring the P1 native engine prototype APK and an attached physical Android 16 `arm64-v8a` device). No synthetic or fabricated numbers are reported.

---

## 2. EMPIRICAL APK SIZE BASELINE (VERIFIED FROM UPSTREAM BINARY METADATA)

Source: `https://www.collaboraoffice.com/downloads/fdroid/repo/index-v1.json` (fetched `2026-11-04T20:58:09+07:00`).

| Package / Variant | Version | ABI (`nativecode`) | Exact Verified APK Size | `minSdk` / `targetSdk` | Notes |
|---|---|---|---|---|---|
| `com.collabora.libreoffice` (Official Release) | `26.04.4.2` (`937`) | `arm64-v8a` | **242.61 MB** (`254,394,xxx` B) | `26` / `37` | Full all-in-one suite (Writer + Calc + Impress + Draw + 5 Hunspell dicts + full fonts) |
| `com.collabora.libreoffice` (Official Release) | `26.04.4.2` (`937`) | `armeabi-v7a` | **178.62 MB** | `26` / `37` | 32-bit ARM single-ABI split |
| `com.collabora.libreoffice` (Official Universal) | `26.04.4.2` (`937`) | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` | **788.63 MB** | `26` / `37` | Multi-ABI universal APK (Do NOT use for EW apps; use `arm64-v8a` split only) |
| `com.collabora.libreoffice.snapshot` | `26.04.6.0-971` (`971`) | `arm64-v8a` | **245.29 MB** | `26` / `37` | Latest nightly snapshot |
| **Projected `EW Documents` (`com.eurowindow.documents`)** | P1 Target | `arm64-v8a` | **~155 MB – 185 MB** (Unstripped: ~225 MB) | `26` / `35+` | Stripping GPL Hunspell dicts (`dict-de`, `dict-es`, `dict-fr`, `dict-pt-BR`), Impress/Draw templates, and unused gallery/branding assets |
| **Projected `EW Sheets` (`com.eurowindow.sheets`)** | P1 Target | `arm64-v8a` | **~155 MB – 185 MB** (Unstripped: ~225 MB) | `26` / `35+` | Same stripped `arm64-v8a` engine + Calc templates & UI |

---

## 3. P0 TEST DOCUMENT CORPUS & 7-STEP ROUND-TRIP VERIFICATION

We generated 7 canonical test documents in `F:\word và Excel\tests\corpus\` covering Small, Medium, and Large workloads, and executed the 7-step verification pipeline (`1. Open -> 2. Parse/Render DOM -> 3. Edit Check -> 4. Save -> 5. Close -> 6. Reopen -> 7. Compare Output`) plus `.NET System.Xml.XmlDocument` validation.

### 3.1 DOCX Test Corpus Results (`F:\word và Excel\tests\corpus\docx\`)

| File Name | Size (Bytes) | SHA-256 (`Original == Round-Trip`) | Parts | Open/Parse (ms) | Validate/Edit (ms) | Save (ms) | Verified Features Preserved | Status |
|---|---|---|---|---|---|---|---|---|
| `01_vietnamese_unicode_small.docx` | `2,819` | `3ab851999d1fd2080e1938ddc77c4db19c92a4589329c8e44216738799e82a37` | 7 | `29.892` | `0.020` | `34.993` | Vietnamese NFC Unicode, Bold/Italic/Underline, Colors, `styles.xml`, `header1.xml`, `footer1.xml`, Page Numbering fields (`PAGE`/`NUMPAGES`) | **PASS** |
| `02_tables_and_links_medium.docx` | `3,270` | `c320196d8b8cf6fa702a7ba1ec82b2ebe90338ca2aa7fec7b06c6ae056f64376` | 7 | `25.958` | `0.035` | `0.957` | 6-column x 16-row table (`<w:tbl>`), External Hyperlink (`rIdLink1`), Vietnamese currency/text, Header/Footer | **PASS** |
| `03_images_headers_pagebreaks.docx` | `3,162` | `e33c0a65c32b7114838bb73b4bf1f6e88382a93a05d1b8eeb7c73938bcfd590c` | 8 | `29.535` | `0.020` | `0.694` | Embedded PNG (`word/media/image1.png`), DrawingML `<wp:inline>`, Explicit Page Break (`<w:br w:type="page"/>`), Header/Footer | **PASS** |
| `04_large_multipage_10p.docx` | `3,180` | `0e39f6f6abc0a83ae1e993bb0b8385a39cf9363f0c986516e81c7165392c58e8` | 7 | `27.893` | `0.068` | `0.998` | 120 Vietnamese paragraphs across 10 explicit page breaks, A4 page setup (`11906x16838` twips), Margins | **PASS** |

### 3.2 XLSX Test Corpus Results (`F:\word và Excel\tests\corpus\xlsx\`)

| File Name | Size (Bytes) | SHA-256 (`Original == Round-Trip`) | Parts | Open/Parse (ms) | Validate/Edit (ms) | Save (ms) | Verified Features Preserved | Status |
|---|---|---|---|---|---|---|---|---|
| `01_formulas_multisheet_small.xlsx` | `3,727` | `1eac7443c6b14772aeaf95c983328698631783c20d38d556e7896bb812905722` | 7 | `6.176` | `0.027` | `0.923` | 2 Worksheets (`BangTinhChinh`, `DanhMucThamChieu`), 9 Formulas (`SUM`, `AVERAGE`, `COUNT`, `COUNTA`, `IF`, `SUMIF`, `COUNTIF`, `VLOOKUP`, `_xlfn.XLOOKUP`), Merge Cells (`A1:C1`), Freeze Pane (`ySplit="2"`), Conditional Formatting, Data Validation | **PASS** |
| `02_formatting_merges_validation_medium.xlsx` | `7,653` | `2c15cae03d4e44f1893783cb77d48714157e655800344f56f283080a0151bf6e` | 7 | `6.659` | `0.055` | `1.031` | 150 data rows + 9 summary formulas, Styles (`styles.xml` fills/fonts), Merge Cells, Conditional Formatting, Data Validation | **PASS** |
| `03_large_dataset_2000rows.xlsx` | `62,031` | `b09aef22395e57103a0f85d6b2f625affe1b161a120e67ab31057086d16aaaf8` | 7 | `7.303` | `0.645` | `4.613` | 2,000 data rows x 4 columns (8,000 cells + 2,009 formulas), Multi-sheet preservation, Zero corruption | **PASS** |

---

## 4. RUNTIME PERFORMANCE BASELINE TABLE (P0 STATUS VS. P1 DEVICE TARGET)

| Metric | Small File (`<10 KB`) | Medium File (`10–100 KB`) | Large File (`2,000+ rows / 10+ pages`) | Measurement Environment / Status |
|---|---|---|---|---|
| **APK Size (`arm64-v8a`)** | **242.61 MB** (Upstream baseline) / **~165 MB** (Stripped target) | Same | Same | Verified from Collabora F-Droid `index-v1.json` (`versionCode: 937`) |
| **OOXML Package Open + DOM Parse (Host Baseline)** | `6.18 ms` (XLSX) / `29.89 ms` (DOCX) | `6.66 ms` (XLSX) / `25.96 ms` (DOCX) | `7.30 ms` (XLSX) / `27.89 ms` (DOCX) | Windows 10 IoT Enterprise LTSC Build `19044`, Python `3.13.14` + `.NET XmlDocument` (`F:\word và Excel\logs\corpus_verification_log.json`) |
| **OOXML Package Save + Close (Host Baseline)** | `0.92 ms` (XLSX) / `34.99 ms` (DOCX) | `1.03 ms` (XLSX) / `0.96 ms` (DOCX) | `4.61 ms` (XLSX) / `1.00 ms` (DOCX) | Windows 10 IoT Enterprise LTSC Build `19044` (`corpus_verification_log.json`) |
| **Cold Start (First Launch Unpack)** | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | Note: On first launch after install/update (`ASSETS_EXTRACTED_GIT_COMMIT`), `LOActivity.init()` extracts `assets/unpack` (`udkapi.rdb`, `offapi.rdb`, `oovbaapi.rdb`, fonts) to `dataDir` |
| **Warm Start (Subsequent Launch)** | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | Requires physical Android 16 device via `adb shell am start -W` in P1 |
| **Native Engine Open & First Tile Render Time** | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | Requires P1 prototype APK + Android 16 device |
| **Native Engine Export PDF Time** | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | Requires P1 prototype APK + Android 16 device |
| **Android Process RAM Usage (`dumpsys meminfo`)** | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | Requires P1 prototype APK + Android 16 device |
| **Android Process CPU Usage (`top` / `dumpsys cpuinfo`)** | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | `PENDING_P1_DEVICE_EXECUTION` | Requires P1 prototype APK + Android 16 device |
| **Crash Rate (P0 Corpus Verification)** | **0.0% (0 / 7 crashes)** | **0.0%** | **0.0%** | Verified on P0 Corpus (`corpus_verification_log.json`); on-device instrumentation crash rate pending P1 |

---

## 5. GATE G7 VERDICT

- **Gate G7 (Performance Baseline Established):** **PARTIAL**
  - **Justification:** APK size baselines (`242.61 MB` `arm64-v8a` release, `245.29 MB` snapshot) and the 7-document DOCX/XLSX test corpus with SHA-256 round-trip integrity and host I/O timings are established and logged with real evidence. On-device Android 16 runtime metrics (cold/warm start, native render latency, PDF export latency, RAM/CPU footprint) require the P1 prototype APK and a connected Android 16 device (`adb devices` currently empty), so G7 is recorded strictly as **PARTIAL**.
