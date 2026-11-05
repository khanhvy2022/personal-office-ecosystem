package com.eurowindow.documents

import com.eurowindow.documents.engine.DocxEngine
import com.eurowindow.documents.model.DocxAlignment
import com.eurowindow.documents.model.DocxDocument
import com.eurowindow.documents.model.DocxParagraph
import com.eurowindow.documents.model.DocxTable
import com.eurowindow.documents.model.DocxTableRow
import com.eurowindow.office.core.security.BroadcastIsolationPolicy
import com.eurowindow.office.core.security.EWOfflineUrlPolicy
import com.eurowindow.office.core.security.TempFileGuard
import com.eurowindow.office.core.storage.SafDocumentController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/**
 * Automated Test Suite for EW Documents Native Light Architecture (<15 MB):
 * - Pure Kotlin OpenXML parser & serializer
 * - Vector PDF exporter
 * - SAF Document Controller & MIME types
 * - Application Descriptor & zero-shared-state isolation
 * - Ephemeral Cache cleanup (S3) & preservation of user documents
 * - Manifest security audit (0 INTERNET, 0 MANAGE_EXTERNAL_STORAGE, allowBackup=false)
 */
class P2DocumentsStandaloneTest {

    private fun resolveProjectRoot(): File {
        var dir: File = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir.parentFile != null && !File(dir, "core").exists()) {
            dir = dir.parentFile!!
        }
        return dir
    }

    @Test
    fun verifyDocxEngine_createsParsesAndSerializesOpenXmlWithoutErrors() {
        // 1. Create document with Vietnamese text, formatting, and tables
        val originalDoc = DocxDocument(
            title = "Hợp đồng thử nghiệm",
            paragraphs = mutableListOf(
                DocxParagraph(
                    text = "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM",
                    alignment = DocxAlignment.CENTER,
                    headingLevel = 1,
                    isBold = true,
                    fontSizeSp = 20f,
                    colorHex = "0F2E5A"
                ),
                DocxParagraph(
                    text = "Báo giá sản phẩm cửa nhôm Eurowindow cao cấp có dấu đầy đủ: á, à, ả, ã, ạ, ắ, ằ, ẳ, ẵ, ặ, ê, ô, ơ, ư.",
                    alignment = DocxAlignment.JUSTIFY,
                    fontSizeSp = 16f
                ),
                DocxParagraph(
                    tableData = DocxTable(
                        rows = mutableListOf(
                            DocxTableRow(mutableListOf("STT", "Sản phẩm", "Đơn giá")),
                            DocxTableRow(mutableListOf("1", "Cửa EA55", "3.500.000")),
                            DocxTableRow(mutableListOf("2", "Phụ kiện", "1.200.000"))
                        )
                    )
                )
            )
        )

        // 2. Serialize to OpenXML DOCX bytes
        val outStream = ByteArrayOutputStream()
        DocxEngine.writeDocx(originalDoc, outStream)
        val docxBytes = outStream.toByteArray()
        assertTrue("Generated DOCX bytes must be non-empty", docxBytes.isNotEmpty())

        // 3. Parse back from bytes
        val parsedDoc = DocxEngine.parseDocx(ByteArrayInputStream(docxBytes))
        assertNotNull("Parsed document must not be null", parsedDoc)
        assertEquals("Must contain 3 paragraphs/blocks", 3, parsedDoc.paragraphs.size)
        assertEquals("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", parsedDoc.paragraphs[0].text)
        assertTrue("Paragraph 0 must be bold", parsedDoc.paragraphs[0].isBold)
        assertEquals(DocxAlignment.CENTER, parsedDoc.paragraphs[0].alignment)
        assertTrue("Paragraph 1 must contain Vietnamese characters", parsedDoc.paragraphs[1].text.contains("cửa nhôm Eurowindow"))

        // Table verification
        val tableP = parsedDoc.paragraphs[2]
        assertNotNull("Table data must be parsed", tableP.tableData)
        assertEquals(3, tableP.tableData!!.rows.size)
        assertEquals("Cửa EA55", tableP.tableData!!.rows[1].cells[1])
    }

    @Test
    fun verifySafController_constantsAndIntentFactory() {
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            SafDocumentController.MIME_DOCX
        )
        assertEquals("application/pdf", SafDocumentController.MIME_PDF)
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", SafDocumentController.MIME_XLSX)

        val openIntent = SafDocumentController.buildOpenDocumentIntent(
            arrayOf(SafDocumentController.MIME_DOCX, "application/msword")
        )
        assertNotNull("Open document intent must be constructed", openIntent)

        val createIntent = SafDocumentController.buildCreateDocumentIntent(
            SafDocumentController.MIME_DOCX,
            "Tai_lieu_Eurowindow.docx"
        )
        assertNotNull("Create document intent must be constructed", createIntent)
    }

    @Test
    fun verifyDocumentsAppDescriptor_isStrictlyIsolatedFromSheets() {
        val docDesc = BroadcastIsolationPolicy.DOCUMENTS_DESCRIPTOR
        assertEquals("com.eurowindow.documents", docDesc.applicationId)
        assertEquals("com.eurowindow.documents.fileprovider", docDesc.fileProviderAuthority)
        assertEquals("com.eurowindow.documents.clipboard", docDesc.clipboardAuthority)
        assertEquals("ew_documents.db", docDesc.databaseName)
        assertEquals("ew_documents_prefs", docDesc.dataStoreName)

        val sheetDesc = BroadcastIsolationPolicy.SHEETS_DESCRIPTOR
        assertFalse(docDesc.applicationId == sheetDesc.applicationId)
        assertFalse(docDesc.fileProviderAuthority == sheetDesc.fileProviderAuthority)
        assertFalse(docDesc.clipboardAuthority == sheetDesc.clipboardAuthority)
        assertFalse(docDesc.databaseName == sheetDesc.databaseName)
        assertFalse(docDesc.dataStoreName == sheetDesc.dataStoreName)

        assertTrue(BroadcastIsolationPolicy.verifyNoSharedNamespaces())
    }

    @Test
    fun verifySaveAndExportPdf_ephemeralArtifactCleanup() {
        val tempRoot = Files.createTempDirectory("ew_p2_doc_test").toFile()
        try {
            val cacheDir = File(tempRoot, "cache").apply { mkdirs() }
            val userDir = File(tempRoot, "user_docs").apply { mkdirs() }

            val userOriginalDocx = File(userDir, "BaoCao_Quy3.docx").apply {
                writeText("USER_DOCX_PERSISTED_CONTENT")
            }
            val workingCacheDocx = File(cacheDir, "BaoCao_Quy3.docx").apply {
                writeText("TEMP_CACHE_DOCX_BYTES")
            }
            val exportedPdf = File(cacheDir, "print.pdf").apply {
                writeText("%PDF-1.4 EXPORTED_DOCUMENT")
            }

            val report = TempFileGuard.scrubAppCacheDirectory(
                cacheDir = cacheDir,
                explicitTempFile = workingCacheDocx,
                protectedUserFiles = setOf(userOriginalDocx)
            )

            assertFalse(workingCacheDocx.exists())
            assertFalse(exportedPdf.exists())
            assertTrue(userOriginalDocx.exists())
            assertEquals("USER_DOCX_PERSISTED_CONTENT", userOriginalDocx.readText())
            assertEquals(2, report.deletedFiles.size)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun verifyOfflineUrlPolicy_strictlyEnforcesOfflineDocxEnvironment() {
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("cool://localhost/cool/mobilesocket/1/open"))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("file:///android_asset/dist/cool.html"))
        assertTrue(EWOfflineUrlPolicy.isAllowedUrl("data:image/png;base64,iVBORw0KGgo="))

        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("http://collaboraoffice.com"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("https://eurowindow.biz"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("ws://localhost:9980/cool"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("wss://office.online"))
        assertFalse(EWOfflineUrlPolicy.isAllowedUrl("file:///data/data/com.eurowindow.documents/databases/ew_documents.db"))
    }

    @Test
    fun verifyDocumentsManifest_securityAndIsolationContract() {
        val projRoot = resolveProjectRoot()
        val manifestFile = File(projRoot, "app/documents/src/main/AndroidManifest.xml")
        assertTrue("Documents AndroidManifest.xml must exist", manifestFile.exists())
        val manifestXml = manifestFile.readText()

        assertTrue(manifestXml.contains("android:label=\"@string/app_name\""))
        assertTrue(manifestXml.contains("android:allowBackup=\"false\""))

        assertTrue(
            manifestXml.contains("android.permission.INTERNET") &&
                manifestXml.contains("tools:node=\"remove\"")
        )

        assertTrue(
            manifestXml.contains("android.permission.MANAGE_EXTERNAL_STORAGE") &&
                manifestXml.contains("tools:node=\"remove\"")
        )

        assertTrue(manifestXml.contains("android:authorities=\"com.eurowindow.documents.fileprovider\""))
        assertTrue(manifestXml.contains("android:name=\"androidx.core.content.FileProvider\""))
        assertTrue(manifestXml.contains(".DocumentsPrototypeActivity"))
    }
}
