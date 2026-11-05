package com.eurowindow.sheets

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.eurowindow.office.core.database.RecentDocumentRecord
import com.eurowindow.office.core.database.RecentDocumentStore
import com.eurowindow.office.core.datastore.EWPreferencesStore
import com.eurowindow.office.core.security.BroadcastIsolationPolicy
import com.eurowindow.office.core.security.TempFileGuard
import com.eurowindow.office.core.storage.SafDocumentController
import com.eurowindow.sheets.engine.XlsxEngine
import com.eurowindow.sheets.model.XlsxWorkbook
import com.eurowindow.sheets.ui.SampleCorpusSheet
import com.eurowindow.sheets.ui.SheetsMainScreen
import com.eurowindow.sheets.ui.XlsxEditorScreen
import java.io.File
import java.io.FileOutputStream

class SheetsPrototypeActivity : ComponentActivity() {

    private lateinit var recentStore: RecentDocumentStore
    private lateinit var prefsStore: EWPreferencesStore

    private var recentSheetsList by mutableStateOf<List<RecentDocumentRecord>>(emptyList())
    private var isAutoCalcEnabled by mutableStateOf(true)
    private var statusMessage by mutableStateOf("Sẵn sàng • Native Jetpack Compose (<2 MB)")

    // Active Editor State
    private var activeWorkbook by mutableStateOf<XlsxWorkbook?>(null)
    private var activeWorkbookUri by mutableStateOf<Uri?>(null)
    private var isWorkbookModified by mutableStateOf(false)

    private val sampleCorpusList = listOf(
        SampleCorpusSheet(
            title = "01. Báo giá cửa nhôm & Công thức",
            fileName = "01_formulas_multisheet_small.xlsx",
            description = "Kiểm tra bảng tính nhiều trang tính, các hàm SUM, AVERAGE và định dạng tiền tệ.",
            badge = "Công thức"
        ),
        SampleCorpusSheet(
            title = "02. Định dạng ô & Bảng dữ liệu mẫu",
            fileName = "02_formatting_merges_validation_medium.xlsx",
            description = "Kiểm tra ô tô màu, chữ đậm, căn phải cho số tiền và lưới bảng tính.",
            badge = "Định dạng"
        ),
        SampleCorpusSheet(
            title = "03. Dữ liệu vật tư lớn (2,000 dòng)",
            fileName = "03_large_dataset_2000rows.xlsx",
            description = "Kiểm tra tốc độ cuộn mượt mà 2 chiều và xuất PDF bảng tính lớn.",
            badge = "Dữ liệu lớn"
        )
    )

    private val openSafLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                SafDocumentController.persistUriPermissionIfPossible(contentResolver, uri)
                loadWorkbookFromUri(uri)
            }
        }
    }

    private val createSafLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                SafDocumentController.persistUriPermissionIfPossible(contentResolver, uri)
                val newWb = XlsxWorkbook.createDefault()
                try {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        XlsxEngine.writeXlsx(newWb, out)
                    }
                    loadWorkbookFromUri(uri)
                } catch (e: Exception) {
                    statusMessage = "Lỗi tạo bảng tính: ${e.message}"
                    Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val exportPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                activeWorkbook?.let { wb ->
                    try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            XlsxEngine.exportToPdf(wb, out)
                        }
                        Toast.makeText(this, "Xuất file PDF bảng tính thành công!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Lỗi xuất PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                activeWorkbook?.let { wb ->
                    val sheet = wb.sheets.firstOrNull() ?: return@let
                    try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            XlsxEngine.exportToCsv(sheet, out)
                        }
                        Toast.makeText(this, "Xuất file CSV thành công!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Lỗi xuất CSV: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val descriptor = BroadcastIsolationPolicy.SHEETS_DESCRIPTOR
        recentStore = RecentDocumentStore(this, descriptor.databaseName)
        prefsStore = EWPreferencesStore(this, descriptor.dataStoreName)

        refreshRecentSheets()

        // Handle intent invocation
        if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT) {
            intent?.data?.let { uri ->
                loadWorkbookFromUri(uri)
            }
        }

        setContent {
            if (activeWorkbook != null) {
                // Interactive Touch Spreadsheet Editor
                BackHandler {
                    closeEditor()
                }

                XlsxEditorScreen(
                    workbook = activeWorkbook!!,
                    isModified = isWorkbookModified,
                    onBack = { closeEditor() },
                    onSave = { saveCurrentWorkbook() },
                    onExportPdf = { triggerExportPdf() },
                    onExportCsv = { triggerExportCsv() }
                )
            } else {
                // Spreadsheet Library Shell
                SheetsMainScreen(
                    recentSheets = recentSheetsList,
                    sampleSheets = sampleCorpusList,
                    isSpellcheckDisabled = isAutoCalcEnabled,
                    onSpellcheckToggled = { enabled ->
                        isAutoCalcEnabled = enabled
                    },
                    onOpenSaf = {
                        val intent = SafDocumentController.buildOpenDocumentIntent(
                            arrayOf(
                                SafDocumentController.MIME_XLSX,
                                "application/vnd.ms-excel",
                                SafDocumentController.MIME_CSV
                            )
                        )
                        openSafLauncher.launch(intent)
                    },
                    onCreateSaf = {
                        val intent = SafDocumentController.buildCreateDocumentIntent(
                            SafDocumentController.MIME_XLSX,
                            "Bang_tinh_Eurowindow.xlsx"
                        )
                        createSafLauncher.launch(intent)
                    },
                    onOpenRecent = { record ->
                        openRecentSheet(record)
                    },
                    onDeleteRecent = { record ->
                        deleteRecentSheetRecord(record)
                    },
                    onOpenSample = { sample ->
                        openBundledCorpusSample(sample)
                    },
                    onClearCache = {
                        val report = TempFileGuard.scrubAppCacheDirectory(cacheDir)
                        statusMessage = "Đã dọn dẹp ${report.deletedFiles.size} tệp đệm (${report.bytesReclaimed} B)"
                        Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
                    },
                    statusMessage = statusMessage
                )
            }
        }
    }

    private fun loadWorkbookFromUri(uri: Uri) {
        val displayName = SafDocumentController.resolveDisplayName(contentResolver, uri, "Bang_tinh.xlsx")
        val isCsv = displayName.endsWith(".csv", ignoreCase = true)
        try {
            val wb = contentResolver.openInputStream(uri)?.use { stream ->
                XlsxEngine.parseXlsx(stream, isCsv = isCsv)
            } ?: XlsxWorkbook.createDefault()

            wb.title = displayName.removeSuffix(".xlsx").removeSuffix(".XLSX").removeSuffix(".csv")
            activeWorkbook = wb
            activeWorkbookUri = uri
            isWorkbookModified = false

            recentStore.upsertRecent(
                RecentDocumentRecord(
                    uriString = uri.toString(),
                    displayName = displayName,
                    mimeType = if (isCsv) SafDocumentController.MIME_CSV else SafDocumentController.MIME_XLSX,
                    lastOpenedEpochMs = System.currentTimeMillis()
                )
            )
            refreshRecentSheets()
            statusMessage = "Đang mở: $displayName"
        } catch (e: Exception) {
            statusMessage = "Lỗi khi đọc bảng tính: ${e.message}"
            Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveCurrentWorkbook() {
        val wb = activeWorkbook ?: return
        val uri = activeWorkbookUri

        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    XlsxEngine.writeXlsx(wb, out)
                }
                isWorkbookModified = false
                Toast.makeText(this, "Đã lưu bảng tính thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi khi lưu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            val intent = SafDocumentController.buildCreateDocumentIntent(
                SafDocumentController.MIME_XLSX,
                "${wb.title}.xlsx"
            )
            createSafLauncher.launch(intent)
        }
    }

    private fun triggerExportPdf() {
        val wb = activeWorkbook ?: return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "${wb.title}.pdf")
        }
        exportPdfLauncher.launch(intent)
    }

    private fun triggerExportCsv() {
        val wb = activeWorkbook ?: return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "${wb.title}.csv")
        }
        exportCsvLauncher.launch(intent)
    }

    private fun closeEditor() {
        activeWorkbook = null
        activeWorkbookUri = null
        isWorkbookModified = false
        refreshRecentSheets()
    }

    private fun openRecentSheet(record: RecentDocumentRecord) {
        val uri = Uri.parse(record.uriString)
        try {
            val exists = if (uri.scheme == "file") {
                File(uri.path ?: "").exists()
            } else {
                contentResolver.openInputStream(uri)?.use { true } ?: false
            }

            if (!exists) {
                statusMessage = "Tệp không tồn tại hoặc đã bị di chuyển: ${record.displayName}"
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
                return
            }
            loadWorkbookFromUri(uri)
        } catch (e: Exception) {
            statusMessage = "Quyền truy cập tệp đã bị thu hồi: ${e.message}"
            Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteRecentSheetRecord(record: RecentDocumentRecord) {
        try {
            recentStore.writableDatabase.execSQL(
                "DELETE FROM recent_documents WHERE uri_string = ?",
                arrayOf(record.uriString)
            )
            refreshRecentSheets()
            statusMessage = "Đã xóa ${record.displayName}"
        } catch (e: Exception) {
            statusMessage = "Lỗi khi xóa: ${e.message}"
        }
    }

    private fun openBundledCorpusSample(sample: SampleCorpusSheet) {
        val sampleFile = File(cacheDir, sample.fileName)
        if (!sampleFile.exists()) {
            val defaultWb = XlsxWorkbook.createDefault()
            defaultWb.title = sample.title
            FileOutputStream(sampleFile).use { out ->
                XlsxEngine.writeXlsx(defaultWb, out)
            }
        }

        val uri = FileProvider.getUriForFile(
            this,
            BroadcastIsolationPolicy.SHEETS_DESCRIPTOR.fileProviderAuthority,
            sampleFile
        )
        loadWorkbookFromUri(uri)
    }

    private fun refreshRecentSheets() {
        recentSheetsList = recentStore.listRecent(30)
    }
}
