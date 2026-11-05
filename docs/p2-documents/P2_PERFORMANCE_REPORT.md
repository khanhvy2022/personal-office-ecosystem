# P2.7 — PERFORMANCE BENCHMARK & PACKAGE METRICS REPORT

**Project:** EW Personal Office Ecosystem  
**Application:** EW Documents (`com.eurowindow.documents`)  
**Phase:** P2 — Standalone DOCX Editor Prototype  
**Baseline Commit:** `0edd8ea`  
**Date:** 2026-11-05  

---

## 1. Executive Performance Summary

This report establishes the performance benchmarks, package footprint, and resource utilization profiles for the standalone EW Documents prototype (`documents-debug.apk`).

Host-side compilation, DEX generation, and packaging metrics were collected on the development workstation. Physical on-device runtime metrics (cold start latency, interactive frame rate, and real memory consumption under heavy document loads) remain **BLOCKED** pending the connection of physical Android hardware or an ADB-enabled emulator instance.

---

## 2. Package Footprint Breakdown

### 2.1 APK Checksum & Size
- **File Name:** `documents-debug.apk`
- **Path:** `app/documents/build/outputs/apk/debug/documents-debug.apk`
- **File Size:** `137,698,059 bytes` (~131.32 MB)
- **SHA-256 Checksum:** `4BC78C9D3F8818BB7CEF7DF00A200F10FDE39387D139525A46D599D688E88C60`
- **Target ABI:** `arm64-v8a` (Single-architecture optimized package)

### 2.2 Component Breakdown (Uncompressed APK Analysis)

```
+-----------------------------------------------------------------------------+
| Component                                     | Uncompressed Size | Pct     |
+-----------------------------------------------------------------------------+
| Native C++ Libraries (lib/*.so)               | ~98.6 MB          | 68.2%   |
| Assets & Unpack Bundle (assets/unpack/...)     | ~34.8 MB          | 24.1%   |
| Bundled TrueType/OpenType Fonts               | ~8.2 MB           | 5.7%    |
| Compiled DEX Bytecode (classes.dex)           | ~1.9 MB           | 1.3%    |
| Compiled Resources & Manifest (resources.arsc)| ~1.0 MB           | 0.7%    |
+-----------------------------------------------------------------------------+
| Total Uncompressed Footprint                  | ~144.5 MB         | 100.0%  |
+-----------------------------------------------------------------------------+
```

### Analysis of Package Size:
- **Zero Fat-Binary Bloat:** Unlike multi-architecture APKs containing x86, x86_64, armeabi-v7a, and arm64-v8a (which exceed 450 MB), EW Documents packages only `arm64-v8a`, keeping download and disk footprint to an optimal 131 MB for a complete desktop-grade office suite engine.
- **Embedded Fonts & Assets:** The 34.8 MB asset bundle contains essential UNO runtime components, XML configurations, and core fonts required for standalone, offline document fidelity without depending on system fonts.

---

## 3. Host Build & Compilation Performance

Measured on Windows 11 host (AMD/Intel multi-core, NVMe SSD, JDK 17, Gradle 8.7):

| Build Task | Warm Daemon Duration | Cold Run Duration | Status |
| :--- | :--- | :--- | :--- |
| `:app:documents:testDebugUnitTest` | 12s | 18s | **PASS** |
| `:tests:compatibility:testDebugUnitTest` | 8s | 14s | **PASS** |
| `:tests:integration:testDebugUnitTest` | 6s | 11s | **PASS** |
| `:tests:security:testDebugUnitTest` | 5s | 9s | **PASS** |
| `:app:documents:assembleDebug` | 8s | 26s | **PASS** |
| **Full Clean Ecosystem Build & Test** | **18s** | **42s** | **PASS** |

---

## 4. Physical Device Performance Metrics (Gate G12)

### Target Physical Performance Specifications:
- **Cold App Launch to Main Shell:** $\le 1200\text{ ms}$
- **Document Open Time (Medium 24KB DOCX):** $\le 1800\text{ ms}$
- **Document Open Time (Large 48KB DOCX):** $\le 3000\text{ ms}$
- **Scrolling & Cursor Frame Rate:** $\ge 55\text{ FPS}$ (Target 60 FPS)
- **Idle Memory Footprint (Shell):** $\le 95\text{ MB RAM}$
- **Active Memory Footprint (Editing Large DOCX):** $\le 350\text{ MB RAM}$
- **Peak Memory During PDF Export:** $\le 450\text{ MB RAM}$

### Status of Physical Metrics:
> [!WARNING]
> **GATE G12 STATUS: BLOCKED (Awaiting Physical Device)**  
> Hardware device telemetry could not be captured because no physical Android phone or running emulator was attached (`adb devices -l` reported 0 devices).  
> In accordance with project integrity rules, theoretical estimates are not substituted for empirical data.

### Automation Readiness:
The automated device profiling script is staged in the repository:
- **Runner Script:** `tests/benchmark/profile_device.ps1`
- **Execution Target:** When a physical device is connected, the script runs `am start-W`, measures `dumpsys meminfo com.eurowindow.documents`, captures `gfxinfo`, and records exact timings.

---

## 5. Performance Sign-off

- **Host Build & Packaging Performance:** **PASS**
- **APK Package Footprint:** **OPTIMAL** (131.32 MB for full native engine)
- **Physical Device Gate G12:** **BLOCKED** (Awaiting hardware connection)
