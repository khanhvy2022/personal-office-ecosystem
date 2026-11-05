# P1.4 — PERFORMANCE BENCHMARK REPORT (SECTION 8)
## APK Size, Build Timing & On-Device Runtime Benchmark Status

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**Status:** **PARTIAL** (APK Size & Host Build Benchmarks **PASS**; On-Device Runtime Latency/RAM/CPU Benchmarks **BLOCKED** awaiting ADB device)

---

## 1. Verified APK Size & Packaging Benchmarks (Measured on Host)

| Metric | Upstream Collabora APK (`26.04.4.2-937`) | EW Documents (`documents-debug.apk`) | EW Sheets (`sheets-debug.apk`) | P0 Target Budget | Status |
| :--- | ---: | ---: | ---: | ---: | :---: |
| **Total APK Size** | `254,391,190` B (`242.61 MB`) | **`137,629,706` B (`131.25 MB`)** | **`137,653,589` B (`131.28 MB`)** | `<= 255 MB` (unstripped) / `<= 160 MB` (packaged) | **PASS** |
| **Size Reduction vs Upstream** | Baseline | **`-111.36 MB` (`-45.9%`)** | **`-111.33 MB` (`-45.9%`)** | — | **PASS** |
| **Native `.so` Libraries (`arm64-v8a`)** | 15 files (`211.01 MB` uncompressed) | 17 files (`liblo-native-code.so` + JNI + AndroidX) | 17 files (`liblo-native-code.so` + JNI + AndroidX) | Single ABI (`arm64-v8a`) | **PASS** |
| **Total ZIP Entries** | `6,343` | `6,181` | `6,180` | — | **PASS** |
| **Incremental Gradle `assembleDebug` Time** | — | `24.0 seconds` (both APKs built in parallel) | `24.0 seconds` | — | **PASS** |
| **Unit Test Suite Execution (`testDebugUnitTest`)** | — | `10.0 seconds` (all 3 test modules) | `10.0 seconds` | — | **PASS** |

---

## 2. On-Device Runtime Performance Table (Pending Physical Android 16 Connection)

Per Section 8 (*"Measure on actual device... Do not invent acceptable results"*), runtime latency, RAM (`dumpsys meminfo`), and CPU measurements are not fabricated and are recorded as `BLOCKED (Awaiting ADB Device)`:

| Metric | Corpus File | Measurement Command | P0 Target | Measured Value | Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **Cold Start Time** | Launcher -> Prototype Shell | `adb shell am start -W -n com.eurowindow.documents/.DocumentsPrototypeActivity` | `< 2,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **Warm Start Time** | Warm activity resume | `adb shell am start -W ...` | `< 800 ms` | *Awaiting ADB device* | **BLOCKED** |
| **DOCX Open (Small)** | `01_vietnamese_unicode_small.docx` (`2,422 B`) | `--es open_corpus "01_vietnamese_unicode_small.docx"` | `< 1,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **DOCX Open (Medium)** | `02_tables_and_links_medium.docx` (`2,566 B`) | `--es open_corpus "02_tables_and_links_medium.docx"` | `< 2,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **DOCX Open (Large 10p)** | `04_large_multipage_10p.docx` (`7,786 B`) | `--es open_corpus "04_large_multipage_10p.docx"` | `< 4,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **XLSX Open (Small)** | `01_formulas_multisheet_small.xlsx` (`2,415 B`) | `--es open_corpus "01_formulas_multisheet_small.xlsx"` | `< 1,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **XLSX Open (Medium)** | `02_formatting_merges_validation_medium.xlsx` (`1,981 B`) | `--es open_corpus "02_formatting_merges_validation_medium.xlsx"` | `< 2,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **XLSX Open (Large 2000r)** | `03_large_dataset_2000rows.xlsx` (`39,656 B`) | `--es open_corpus "03_large_dataset_2000rows.xlsx"` | `< 4,500 ms` | *Awaiting ADB device* | **BLOCKED** |
| **Peak PSS Memory** | Active editing session | `adb shell dumpsys meminfo <package>` | `< 350 MB` | *Awaiting ADB device* | **BLOCKED** |
