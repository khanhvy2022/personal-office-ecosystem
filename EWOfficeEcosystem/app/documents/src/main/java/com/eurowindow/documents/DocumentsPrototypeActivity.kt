package com.eurowindow.documents

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import com.eurowindow.documents.engine.DocxEngine
import com.eurowindow.documents.model.DocxDocument
import com.eurowindow.documents.ui.DocxEditorScreen
import com.eurowindow.documents.ui.DocumentsMainScreen
import com.eurowindow.documents.ui.SampleCorpusDoc
import com.eurowindow.office.core.database.RecentDocumentRecord
import com.eurowindow.office.core.database.RecentDocumentStore
import com.eurowindow.office.core.datastore.EWPreferencesStore
import com.eurowindow.office.core.security.BroadcastIsolationPolicy
import com.eurowindow.office.core.security.TempFileGuard
import com.eurowindow.office.core.storage.SafDocumentController
import java.io.File
import java.io.FileOutputStream

class DocumentsPrototypeActivity : ComponentActivity() {

    private lateinit var recentStore: RecentDocumentStore
    private lateinit var prefsStore: EWPreferencesStore

    private var recentDocumentsList by mutableStateOf<List<RecentDocumentRecord>>(emptyList())
    private var isSpellcheckDisabled by mutableStateOf(true)
    private var statusMessage by mutableStateOf("Sẵn sàng • Native Jetpack Compose (<10 MB)")

    // Active Editor State
    private var activeDocument by mutableStateOf<DocxDocument?>(null)
    private var activeDocumentUri by mutableStateOf<Uri?>(null)
    private var isDocumentModified by mutableStateOf(false)

    private val sampleCorpusList = listOf(
        SampleCorpusDoc(
            title = "01. Tiếng Việt Unicode & Định dạng",
            fileName = "01_vietnamese_unicode_small.docx",
            description = "Kiểm tra dấu thanh tiếng Việt, tiêu đề, in đậm/nghiêng/gạch chân và khổ giấy A4.",
            badge = "Tiếng Việt"
        ),
        SampleCorpusDoc(
            title = "02. Báo giá cửa nhôm Eurowindow & Bảng biểu",
            fileName = "02_tables_and_links_medium.docx",
            description = "Kiểm tra bảng dữ liệu chi tiết nhiều hàng cột, căn lề và định dạng ô.",
            badge = "Bảng biểu"
        ),
        SampleCorpusDoc(
            title = "03. Hợp đồng kinh tế mẫu",
            fileName = "03_images_headers_pagebreaks.docx",
            description = "Mẫu hợp đồng pháp lý với các điều khoản đánh số và căn lề chuyên nghiệp.",
            badge = "Hợp đồng"
        ),
        SampleCorpusDoc(
            title = "04. Báo cáo tổng hợp dự án",
            fileName = "04_large_multipage_10p.docx",
            description = "Kiểm tra tốc độ hiển thị và xuất PDF tài liệu dài.",
            badge = "Báo cáo"
        )
    )

    private val openSafLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                SafDocumentController.persistUriPermissionIfPossible(contentResolver, uri)
                loadDocumentFromUri(uri)
            }
        }
    }

    private val createSafLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                SafDocumentController.persistUriPermissionIfPossible(contentResolver, uri)
                // Initialize with fresh blank template
                val newDoc = DocxDocument.createDefault()
                try {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        DocxEngine.writeDocx(newDoc, out)
                    }
                    loadDocumentFromUri(uri)
                } catch (e: Exception) {
                    statusMessage = "Lỗi tạo tài liệu: ${e.message}"
                    Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val exportPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                activeDocument?.let { doc ->
                    try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            DocxEngine.exportToPdf(doc, out)
                        }
                        Toast.makeText(this, "Xuất file PDF thành công!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Lỗi xuất PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val descriptor = BroadcastIsolationPolicy.DOCUMENTS_DESCRIPTOR
        recentStore = RecentDocumentStore(this, descriptor.databaseName)
        prefsStore = EWPreferencesStore(this, descriptor.dataStoreName)

        isSpellcheckDisabled = prefsStore.isSpellcheckDisabledByDefault
        refreshRecentDocuments()

        // Handle intent invocation
        if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT) {
            intent?.data?.let { uri ->
                loadDocumentFromUri(uri)
            }
        }

        setContent {
            if (activeDocument != null) {
                // Interactive Touch Editor
                BackHandler {
                    closeEditor()
                }

                DocxEditorScreen(
                    document = activeDocument!!,
                    isModified = isDocumentModified,
                    onBack = { closeEditor() },
                    onSave = { saveCurrentDocument() },
                    onExportPdf = { triggerExportPdf() }
                )
            } else {
                // Document Library & Management Shell
                DocumentsMainScreen(
                    recentDocuments = recentDocumentsList,
                    sampleDocuments = sampleCorpusList,
                    isSpellcheckDisabled = isSpellcheckDisabled,
                    onSpellcheckToggled = { enabled ->
                        isSpellcheckDisabled = enabled
                        prefsStore.isSpellcheckDisabledByDefault = enabled
                        statusMessage = if (enabled) "Đã tắt kiểm tra chính tả" else "Đã bật kiểm tra chính tả"
                    },
                    onOpenSaf = {
                        val intent = SafDocumentController.buildOpenDocumentIntent(
                            arrayOf(
                                SafDocumentController.MIME_DOCX,
                                "application/msword",
                                "application/vnd.oasis.opendocument.text"
                            )
                        )
                        openSafLauncher.launch(intent)
                    },
                    onCreateSaf = {
                        val intent = SafDocumentController.buildCreateDocumentIntent(
                            SafDocumentController.MIME_DOCX,
                            "Tai_lieu_Eurowindow.docx"
                        )
                        createSafLauncher.launch(intent)
                    },
                    onOpenRecent = { record ->
                        openRecentDocument(record)
                    },
                    onDeleteRecent = { record ->
                        deleteRecentDocumentRecord(record)
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

    private fun loadDocumentFromUri(uri: Uri) {
        val displayName = SafDocumentController.resolveDisplayName(contentResolver, uri, "Tai_lieu.docx")
        try {
            val doc = contentResolver.openInputStream(uri)?.use { stream ->
                DocxEngine.parseDocx(stream)
            } ?: DocxDocument.createDefault()

            doc.title = displayName.removeSuffix(".docx").removeSuffix(".DOCX")
            activeDocument = doc
            activeDocumentUri = uri
            isDocumentModified = false

            recentStore.upsertRecent(
                RecentDocumentRecord(
                    uriString = uri.toString(),
                    displayName = displayName,
                    mimeType = SafDocumentController.MIME_DOCX,
                    lastOpenedEpochMs = System.currentTimeMillis()
                )
            )
            refreshRecentDocuments()
            statusMessage = "Đang mở: $displayName"
        } catch (e: Exception) {
            statusMessage = "Lỗi khi đọc file: ${e.message}"
            Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveCurrentDocument() {
        val doc = activeDocument ?: return
        val uri = activeDocumentUri

        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    DocxEngine.writeDocx(doc, out)
                }
                isDocumentModified = false
                Toast.makeText(this, "Đã lưu tài liệu thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi khi lưu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Document without URI -> prompt create document
            val intent = SafDocumentController.buildCreateDocumentIntent(
                SafDocumentController.MIME_DOCX,
                "${doc.title}.docx"
            )
            createSafLauncher.launch(intent)
        }
    }

    private fun triggerExportPdf() {
        val doc = activeDocument ?: return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "${doc.title}.pdf")
        }
        exportPdfLauncher.launch(intent)
    }

    private fun closeEditor() {
        activeDocument = null
        activeDocumentUri = null
        isDocumentModified = false
        refreshRecentDocuments()
    }

    private fun openRecentDocument(record: RecentDocumentRecord) {
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
            loadDocumentFromUri(uri)
        } catch (e: Exception) {
            statusMessage = "Quyền truy cập tệp đã bị thu hồi: ${e.message}"
            Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteRecentDocumentRecord(record: RecentDocumentRecord) {
        try {
            recentStore.writableDatabase.execSQL(
                "DELETE FROM recent_documents WHERE uri_string = ?",
                arrayOf(record.uriString)
            )
            refreshRecentDocuments()
            statusMessage = "Đã xóa ${record.displayName}"
        } catch (e: Exception) {
            statusMessage = "Lỗi khi xóa: ${e.message}"
        }
    }

    private fun openBundledCorpusSample(sample: SampleCorpusDoc) {
        val sampleFile = File(cacheDir, sample.fileName)
        if (!sampleFile.exists()) {
            // Create a rich demo document if not existing
            val sampleDoc = DocxDocument(
                title = sample.title,
                paragraphs = mutableListOf(
                    com.eurowindow.documents.model.DocxParagraph(
                        text = "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM",
                        alignment = com.eurowindow.documents.model.DocxAlignment.CENTER,
                        headingLevel = 1,
                        isBold = true,
                        fontSizeSp = 20f,
                        colorHex = "0F2E5A"
                    ),
                    com.eurowindow.documents.model.DocxParagraph(
                        text = "Độc lập - Tự do - Hạnh phúc",
                        alignment = com.eurowindow.documents.model.DocxAlignment.CENTER,
                        isBold = true,
                        isUnderline = true,
                        fontSizeSp = 14f,
                        colorHex = "0F2E5A"
                    ),
                    com.eurowindow.documents.model.DocxParagraph(
                        text = "BÁO GIÁ CỬA NHÔM KÍNH EUROWINDOW CAO CẤP",
                        alignment = com.eurowindow.documents.model.DocxAlignment.CENTER,
                        headingLevel = 1,
                        isBold = true,
                        fontSizeSp = 18f,
                        colorHex = "2260B4"
                    ),
                    com.eurowindow.documents.model.DocxParagraph(
                        text = "Kính gửi Quý Khách hàng bản dự toán chi tiết các hạng mục cửa sổ và cửa đi cho công trình biệt thự năm 2026. Toàn bộ sản phẩm được gia công chính xác theo tiêu chuẩn ISO 9001.",
                        alignment = com.eurowindow.documents.model.DocxAlignment.JUSTIFY,
                        fontSizeSp = 16f
                    ),
                    com.eurowindow.documents.model.DocxParagraph(
                        tableData = com.eurowindow.documents.model.DocxTable(
                            rows = mutableListOf(
                                com.eurowindow.documents.model.DocxTableRow(mutableListOf("STT", "Hạng mục", "Kích thước (mm)", "Đơn giá (VNĐ)")),
                                com.eurowindow.documents.model.DocxTableRow(mutableListOf("1", "Cửa đi mở quay 2 cánh Eurowindow", "1800 x 2400", "8.500.000")),
                                com.eurowindow.documents.model.DocxTableRow(mutableListOf("2", "Cửa sổ mở hất hệ nhôm EA55", "1200 x 1400", "4.200.000")),
                                com.eurowindow.documents.model.DocxTableRow(mutableListOf("3", "Kính hộp 2 lớp cách âm cách nhiệt", "Theo thiết kế", "Bao gồm"))
                            )
                        )
                    ),
                    com.eurowindow.documents.model.DocxParagraph(
                        text = "Ghi chú: Đơn giá đã bao gồm phụ kiện đồng bộ kim khí cao cấp và chi phí lắp đặt hoàn thiện.",
                        alignment = com.eurowindow.documents.model.DocxAlignment.LEFT,
                        isItalic = true,
                        fontSizeSp = 13f,
                        colorHex = "64748B"
                    )
                )
            )
            FileOutputStream(sampleFile).use { out ->
                DocxEngine.writeDocx(sampleDoc, out)
            }
        }

        val uri = FileProvider.getUriForFile(
            this,
            BroadcastIsolationPolicy.DOCUMENTS_DESCRIPTOR.fileProviderAuthority,
            sampleFile
        )
        loadDocumentFromUri(uri)
    }

    private fun refreshRecentDocuments() {
        recentDocumentsList = recentStore.listRecent(30)
    }
}
