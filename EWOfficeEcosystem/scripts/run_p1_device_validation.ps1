# ==============================================================================
# P1.3 / P1.4 — EW PERSONAL OFFICE REAL DEVICE VALIDATION & BENCHMARK HARNESS
# Target: Android 16 (API 36 / arm64-v8a) connected via USB/ADB
# ==============================================================================
$ErrorActionPreference = "Stop"

$ADB = "F:\pdf\toolchain\android-sdk\platform-tools\adb.exe"
$ROOT = "F:\word và Excel"
$DOC_APK = "$ROOT\EWOfficeEcosystem\app\documents\build\outputs\apk\debug\documents-debug.apk"
$SHEET_APK = "$ROOT\EWOfficeEcosystem\app\sheets\build\outputs\apk\debug\sheets-debug.apk"
$LOG_DIR = "$ROOT\logs\device"
New-Item -ItemType Directory -Force -Path $LOG_DIR | Out-Null

Write-Host "=== 1. Checking Connected Android Device via ADB ===" -ForegroundColor Cyan
$devices = & $ADB devices -l
Write-Host $devices
if ($devices -notmatch "\sdevice\s") {
    Write-Error "BLOCKED: No physical Android device connected in 'device' state via ADB. Connect Android 16 device and re-run."
    exit 2
}

$model = (& $ADB shell getprop ro.product.model).Trim()
$release = (& $ADB shell getprop ro.build.version.release).Trim()
$sdk = (& $ADB shell getprop ro.build.version.sdk).Trim()
$abi = (& $ADB shell getprop ro.product.cpu.abi).Trim()
$memInfo = (& $ADB shell cat /proc/meminfo | Select-String "MemTotal|MemAvailable") -join "; "

Write-Host "Connected Device: Model=$model | Android=$release (SDK $sdk) | ABI=$abi | $memInfo" -ForegroundColor Green

Write-Host "=== 2. Installing EW Documents & EW Sheets Simultaneously (Section 6) ===" -ForegroundColor Cyan
& $ADB install -r "$DOC_APK"
& $ADB install -r "$SHEET_APK"

$docUid = (& $ADB shell dumpsys package com.eurowindow.documents | Select-String "userId=" | Select-Object -First 1).ToString().Trim()
$sheetUid = (& $ADB shell dumpsys package com.eurowindow.sheets | Select-String "userId=" | Select-Object -First 1).ToString().Trim()
Write-Host "EW Documents UID: $docUid"
Write-Host "EW Sheets UID:    $sheetUid"

Write-Host "=== 3. Cold Start Benchmark (`am start -W`) (Section 8) ===" -ForegroundColor Cyan
& $ADB shell am force-stop com.eurowindow.documents
& $ADB shell am start -W -n com.eurowindow.documents/.DocumentsPrototypeActivity | Tee-Object -FilePath "$LOG_DIR\cold_start_documents.log"

& $ADB shell am force-stop com.eurowindow.sheets
& $ADB shell am start -W -n com.eurowindow.sheets/.SheetsPrototypeActivity | Tee-Object -FilePath "$LOG_DIR\cold_start_sheets.log"

Write-Host "=== 4. Opening Embedded Vietnamese DOCX & Formula XLSX Corpus in Offline LOKit Engine ===" -ForegroundColor Cyan
& $ADB shell am start -W -n com.eurowindow.documents/.DocumentsPrototypeActivity --es open_corpus "01_vietnamese_unicode_small.docx" | Tee-Object -FilePath "$LOG_DIR\open_docx_small.log"
Start-Sleep -Seconds 5
& $ADB shell dumpsys meminfo com.eurowindow.documents | Tee-Object -FilePath "$LOG_DIR\meminfo_documents.log"

& $ADB shell am start -W -n com.eurowindow.sheets/.SheetsPrototypeActivity --es open_corpus "01_formulas_multisheet_small.xlsx" | Tee-Object -FilePath "$LOG_DIR\open_xlsx_small.log"
Start-Sleep -Seconds 5
& $ADB shell dumpsys meminfo com.eurowindow.sheets | Tee-Object -FilePath "$LOG_DIR\meminfo_sheets.log"

Write-Host "=== 5. Two-App Uninstallation Independence Verification (Section 6) ===" -ForegroundColor Cyan
& $ADB uninstall com.eurowindow.documents
& $ADB shell am start -W -n com.eurowindow.sheets/.SheetsPrototypeActivity | Tee-Object -FilePath "$LOG_DIR\sheets_after_doc_uninstall.log"
& $ADB install -r "$DOC_APK"

& $ADB uninstall com.eurowindow.sheets
& $ADB shell am start -W -n com.eurowindow.documents/.DocumentsPrototypeActivity | Tee-Object -FilePath "$LOG_DIR\doc_after_sheets_uninstall.log"
& $ADB install -r "$SHEET_APK"

Write-Host "Device validation logs written to $LOG_DIR" -ForegroundColor Green
