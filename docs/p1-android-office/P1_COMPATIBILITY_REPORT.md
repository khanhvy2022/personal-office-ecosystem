# P1 — DOCUMENT & SPREADSHEET COMPATIBILITY REPORT
## Corpus Packaging, OOXML Structure Verification & Engine Filter Readiness

**Phase:** P1 — Native Engine Prototype & Android Device Validation  
**Date:** 2026-11-04  
**Status:** **PARTIAL** (Host OOXML Corpus & Filter Packaging Verification **PASS**; On-Device Interactive Round-Trip Execution **BLOCKED** awaiting ADB device)

---

## 1. Embedded Validation Corpus & Automated JUnit Verification

All 7 P0 test corpus files are embedded directly into the prototype APKs (`app/documents/src/main/assets/corpus/*.docx` and `app/sheets/src/main/assets/corpus/*.xlsx`) and verified by `:tests:compatibility:testDebugUnitTest` (`P1CorpusCompatibilityTest` — **PASS**):

| Corpus File | Target App | Verified OOXML Features (`P1CorpusCompatibilityTest`) | Host Test | Device Round-Trip |
| :--- | :--- | :--- | :---: | :---: |
| `01_vietnamese_unicode_small.docx` | EW Documents | Vietnamese precomposed/combining diacritics (`Việt Nam — Độc lập - Tự do - Hạnh phúc`), Headings, Bold/Italic/Underline, A4 (`w:w="11906"`) | **PASS** | **BLOCKED** (No ADB) |
| `02_tables_and_links_medium.docx` | EW Documents | 4x5 financial table (`<w:tbl>`), horizontal cell merges (`<w:gridSpan>`), external hyperlink (`https://eurowindow.biz`) | **PASS** | **BLOCKED** (No ADB) |
| `03_images_headers_pagebreaks.docx` | EW Documents | Embedded PNG image (`word/media/image1.png`), running header (`header1.xml`), running footer (`footer1.xml`), explicit page breaks | **PASS** | **BLOCKED** (No ADB) |
| `04_large_multipage_10p.docx` | EW Documents | 10+ page multi-section Vietnamese contract stress document (`7,786 B`) | **PASS** | **BLOCKED** (No ADB) |
| `01_formulas_multisheet_small.xlsx` | EW Sheets | Multi-sheet (`DoanhThu`, `BangGia`), `SUM(D3:D17)`, `IF(D18>1000000,...)`, `SUMIF(B3:B17,...)`, `VLOOKUP("EW-0001",...)`, `_xlfn.XLOOKUP("EW-0001",...)` | **PASS** | **BLOCKED** (No ADB) |
| `02_formatting_merges_validation_medium.xlsx` | EW Sheets | Custom VND number formats, percentage/date styles, merged title ranges (`<mergeCells>`), frozen header pane (`<pane state="frozen"/>`) | **PASS** | **BLOCKED** (No ADB) |
| `03_large_dataset_2000rows.xlsx` | EW Sheets | 2,000 rows x 10 columns dataset (`r="2001"`) with row-level & summary formulas (`39,656 B`) | **PASS** | **BLOCKED** (No ADB) |
